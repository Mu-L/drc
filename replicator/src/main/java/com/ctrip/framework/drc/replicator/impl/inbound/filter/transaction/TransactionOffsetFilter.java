package com.ctrip.framework.drc.replicator.impl.inbound.filter.transaction;

import com.ctrip.framework.drc.core.driver.binlog.LogEvent;
import com.ctrip.framework.drc.core.driver.binlog.constant.LogEventType;
import com.ctrip.framework.drc.core.driver.binlog.impl.*;
import com.ctrip.framework.drc.core.driver.util.LogEventUtils;

import java.util.List;

import static com.ctrip.framework.drc.core.driver.binlog.constant.LogEventType.*;

/**
 * @Author limingdong
 * @create 2021/10/9
 */
public class TransactionOffsetFilter extends AbstractTransactionFilter {

    private String lastTrxSchema = null;
    private String lastTableName = null;

    @Override
    public boolean doFilter(ITransactionEvent transactionEvent) {
        calculateNextEventStartPosition(transactionEvent);
        return doNext(transactionEvent, false);
    }

    protected void calculateNextEventStartPosition(ITransactionEvent transactionEvent) {
        List<LogEvent> logEvents = transactionEvent.getEvents();

        long nextTransactionStartPosition = 0;
        FilterLogEvent firstLogEvent = (FilterLogEvent) logEvents.get(0);
        LogEvent secondLogEvent = logEvents.get(1);
        LogEvent lastLogEvent = logEvents.get(logEvents.size() - 1);

        for (int i = 2; i < logEvents.size(); i++) {
            LogEvent logEvent = logEvents.get(i);
            LogEventType logEventType = logEvent.getLogEventType();
            if (table_map_log_event == logEventType || drc_table_map_log_event == logEventType) {
                TableMapLogEvent tableMapLogEvent = logEvent instanceof TableMapLogEvent
                        ? (TableMapLogEvent) logEvent
                        : ((TransactionTableMarkedTableMapLogEvent) logEvent).getDelegate();
                lastTrxSchema = tableMapLogEvent.getSchemaName();
                lastTableName = tableMapLogEvent.getTableName();
            }
            nextTransactionStartPosition += logEvent.getLogEventHeader().getEventSize();
        }


        //non-ddl, non-bigTrx
        if (transactionEvent.canSkipParseTransaction()) {
            GtidLogEvent gtidLogEvent = (GtidLogEvent) secondLogEvent;
            gtidLogEvent.setNextTransactionOffsetAndUpdateEventSize(nextTransactionStartPosition);
        } else {
            if (LogEventUtils.isGtidLogEvent(secondLogEvent.getLogEventType())) {
                GtidLogEvent gtidLogEvent = (GtidLogEvent) secondLogEvent;
                gtidLogEvent.setNextTransactionOffsetAndUpdateEventSize(0);
            }
        }

        int rowsEventCount = (int) logEvents.stream().map(LogEvent::getLogEventType).filter(LogEventUtils::isRowsEvent).count();
        firstLogEvent.encode(lastTrxSchema, lastTableName, logEvents.size(), rowsEventCount,
                nextTransactionStartPosition + secondLogEvent.getLogEventHeader().getEventSize());

        if (xid_log_event == lastLogEvent.getLogEventType()) {
            lastTrxSchema = null;
            lastTableName = null;
        }
    }

}
