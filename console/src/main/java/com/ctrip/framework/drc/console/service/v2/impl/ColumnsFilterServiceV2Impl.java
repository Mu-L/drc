package com.ctrip.framework.drc.console.service.v2.impl;

import com.ctrip.framework.drc.console.dao.entity.v2.ColumnsFilterTblV2;
import com.ctrip.framework.drc.console.dao.v2.ColumnsFilterTblV2Dao;
import com.ctrip.framework.drc.console.enums.ColumnsFilterModeEnum;
import com.ctrip.framework.drc.console.service.v2.ColumnsFilterServiceV2;
import com.ctrip.framework.drc.core.meta.ColumnsFilterConfig;
import com.ctrip.framework.drc.core.service.utils.JsonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by dengquanliang
 * 2023/5/26 16:21
 */
@Service
public class ColumnsFilterServiceV2Impl implements ColumnsFilterServiceV2 {

    @Autowired
    private ColumnsFilterTblV2Dao columnsFilterTblDao;

    @Override
    public List<ColumnsFilterTblV2> queryByIds(List<Long> columnsFilterIds) throws SQLException {
        if (CollectionUtils.isEmpty(columnsFilterIds)) {
            return Collections.emptyList();
        }
        return columnsFilterTblDao.queryByIds(columnsFilterIds);
    }

    @Override
    public List<ColumnsFilterConfig> generateColumnsFilterConfig(String tableName, List<Long> columnsFilterIds) throws SQLException {
        List<ColumnsFilterTblV2> columnsFilterTbls = this.queryByIds(columnsFilterIds);
        return generateColumnsFilterConfigFromTbl(tableName, columnsFilterTbls);
    }

    @Override
    public List<ColumnsFilterConfig> generateColumnsFilterConfigFromTbl(String tableName, List<ColumnsFilterTblV2> colsFilterTblList) {
        return colsFilterTblList.stream().map(source -> {
            ColumnsFilterConfig target = new ColumnsFilterConfig();
            target.setTables(tableName);
            target.setMode(ColumnsFilterModeEnum.getNameByCode(source.getMode()));
            target.setColumns(JsonUtils.fromJsonToList(source.getColumns(), String.class));

            return target;
        }).collect(Collectors.toList());
    }
}
