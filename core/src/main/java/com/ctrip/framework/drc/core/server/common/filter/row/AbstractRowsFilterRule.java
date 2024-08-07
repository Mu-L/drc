package com.ctrip.framework.drc.core.server.common.filter.row;

import com.ctrip.framework.drc.core.driver.binlog.impl.AbstractRowsEvent;
import com.ctrip.framework.drc.core.driver.schema.data.Columns;
import com.ctrip.framework.drc.core.exception.DrcException;
import com.ctrip.framework.drc.core.meta.RowsFilterConfig;
import com.ctrip.framework.drc.core.monitor.reporter.DefaultEventMonitorHolder;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.ctrip.framework.drc.core.driver.binlog.constant.LogEventType.update_rows_event_v2;
import static com.ctrip.framework.drc.core.server.common.filter.row.RowsFilterResult.Status.*;

/**
 * @Author limingdong
 * @create 2022/4/26
 */
public abstract class AbstractRowsFilterRule implements RowsFilterRule<List<AbstractRowsEvent.Row>> {

    protected String registryKey;

    protected List<RowsFilterConfig.Parameters> parametersList;

    private List<String> fields;

    public AbstractRowsFilterRule(RowsFilterConfig rowsFilterConfig) {
        this.registryKey = rowsFilterConfig.getRegistryKey();
        parametersList = rowsFilterConfig.getConfigs() != null ? rowsFilterConfig.getConfigs().getParameterList() : Lists.newArrayList(rowsFilterConfig.getParameters());
        fields = parametersList.stream().flatMap(parameters -> parameters.getColumns().stream()).collect(Collectors.toList());
    }

    @Override
    public RowsFilterResult<List<AbstractRowsEvent.Row>> filterRows(AbstractRowsEvent rowsEvent, RowsFilterContext rowsFilterContext) throws Exception {
        Columns columns = Columns.from(rowsFilterContext.getDrcTableMapLogEvent().getColumns());
        List<AbstractRowsEvent.Row> rows = rowsEvent.getRows();

        LinkedHashMap<String, Integer> indices = getIndices(columns, fields);
        if (indices == null) {
            return new RowsFilterResult(No_Filtered);
        }

        List<AbstractRowsEvent.Row> filteredRow = doFilterRows(rowsEvent, rowsFilterContext, indices);

        if (filteredRow != null && rows.size() == filteredRow.size()) {
            return new RowsFilterResult(No_Filtered);
        }
        return new RowsFilterResult(Filtered, filteredRow);
    }

    protected List<List<Object>> getValues(AbstractRowsEvent rowsEvent) {
        List<List<Object>> values;
        if (update_rows_event_v2 == rowsEvent.getLogEventType()) {
            values = rowsEvent.getAfterPresentRowsValues();
        } else {
            values = rowsEvent.getBeforePresentRowsValues();
        }

        return values;
    }

    protected LinkedHashMap<String, Integer> getIndices(Columns columns, List<String> fields) {
        final int fieldSize = fields.size();
        LinkedHashMap<String, Integer> integerMap = Maps.newLinkedHashMap();

        int found = 0;
        for (int i = 0; i < fieldSize; ++i) {
            for (int j = 0; j < columns.size(); ++j) {
                String colName = columns.get(j).getName();
                if (colName.equalsIgnoreCase(fields.get(i))) {
                    integerMap.put(colName.toLowerCase(), j);
                    found++;
                    break;
                }
            }
        }

        return found == fieldSize ? integerMap : ((parametersList != null && parametersList.size() > 1) ? integerMap : null);
    }

    /**
     * @param rowsEvent rows event in binlog
     * @param indices   column name to its index in values
     * @return
     * @throws Exception
     */
    protected List<AbstractRowsEvent.Row> doFilterRows(AbstractRowsEvent rowsEvent, RowsFilterContext rowsFilterContext, LinkedHashMap<String, Integer> indices) throws Exception {
        List<AbstractRowsEvent.Row> result = Lists.newArrayList();
        List<List<Object>> values = getValues(rowsEvent);
        List<AbstractRowsEvent.Row> rows = rowsEvent.getRows();
        for (int i = 0; i < values.size(); ++i) {
            RowsFilterResult.Status filterResult = handleRow(values.get(i), rowsFilterContext, indices);
            if (filterResult.noRowFiltered()) {
                result.add(rows.get(i));
            }
        }
        return result;
    }

    private RowsFilterResult.Status handleRow(List<Object> rowValue, RowsFilterContext rowsFilterContext, LinkedHashMap<String, Integer> indices) throws Exception {
        RowsFilterResult.Status filterResult = Illegal;
        Object lastValue = null;
        boolean foundColumn = false;
        RowsFilterConfig.Parameters parameters = null;
        for (int i = 0; i < parametersList.size(); ++i) { // iterate Parameters : udl、uid
            parameters = parametersList.get(i);
            List<String> fieldList = parameters.getColumns();
            if (fieldList != null && !fieldList.isEmpty()) {
                Integer index = indices.get(fieldList.get(0).toLowerCase());
                if (index == null) {
                    continue;
                }
                foundColumn = true;
                lastValue = rowValue.get(index);
                filterResult = handleValue(lastValue, parameters, rowsFilterContext);
                if (Illegal != filterResult) {
                    rowsFilterContext.putIfAbsent(new CacheKey(parameters.getUserFilterMode(), lastValue), filterResult);
                    return filterResult;
                }
            }

        }
        if (!foundColumn) {
            throw new DrcException(String.format("[Row] filter without matching column for %s:%s",
                    rowsFilterContext.getDrcTableMapLogEvent().getSchemaNameDotTableName(),
                    registryKey));
        }
        return handleIllegal(new CacheKey(parametersList.get(parametersList.size() - 1).getUserFilterMode(), lastValue), filterResult, parameters, rowsFilterContext);
    }

    private RowsFilterResult.Status handleValue(Object field, RowsFilterConfig.Parameters parameters, RowsFilterContext rowsFilterContext) throws Exception {
        RowsFilterResult.Status filterResult = rowsFilterContext.get(new CacheKey(parameters.getUserFilterMode(), field));
        if (filterResult == null) {
            filterResult = doFilterRows(field, parameters);
        } else {
            DefaultEventMonitorHolder.getInstance().logEvent("DRC.replicator.rows.filter.cache", registryKey);
        }
        return filterResult;
    }

    private RowsFilterResult.Status handleIllegal(CacheKey key, RowsFilterResult.Status filterResult, RowsFilterConfig.Parameters parameters, RowsFilterContext rowsFilterContext) {
        RowsFilterResult.Status status = filterResult;
        if (Illegal == filterResult) {
            status = RowsFilterResult.Status.from(parameters.getIllegalArgument());
            rowsFilterContext.putIfAbsent(key, status);

        }
        return status;
    }

    protected RowsFilterResult.Status doFilterRows(Object field, RowsFilterConfig.Parameters parameters) throws Exception {
        return No_Filtered;
    }

    static class CacheKey {
        private String userFilterMode;
        private Object lastValue;

        public CacheKey(String userFilterMode, Object lastValue) {
            this.userFilterMode = userFilterMode;
            this.lastValue = lastValue;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof CacheKey)) return false;
            CacheKey cacheKey = (CacheKey) o;
            return Objects.equals(userFilterMode, cacheKey.userFilterMode) &&
                    Objects.equals(lastValue, cacheKey.lastValue);
        }

        @Override
        public int hashCode() {

            return Objects.hash(userFilterMode, lastValue);
        }
    }
}
