package com.ctrip.framework.drc.replicator.impl.oubound.filter;

import com.ctrip.framework.drc.core.config.DynamicConfig;
import com.ctrip.framework.drc.core.driver.binlog.constant.LogEventType;
import com.ctrip.framework.drc.core.driver.binlog.impl.TableMapLogEvent;
import com.ctrip.framework.drc.core.driver.util.LogEventUtils;
import com.ctrip.framework.drc.core.monitor.entity.TrafficStatisticKey;
import com.ctrip.framework.drc.core.monitor.kpi.OutboundMonitorReport;
import com.ctrip.framework.drc.core.server.common.enums.ConsumeType;
import com.ctrip.framework.drc.core.server.common.filter.AbstractPostLogEventFilter;
import org.apache.commons.lang3.StringUtils;

import static com.ctrip.framework.drc.core.driver.binlog.constant.LogEventType.*;

/**
 * Created by jixinwang on 2023/10/25
 */
public class MonitorFilter extends AbstractPostLogEventFilter<OutboundLogEventContext> {

    private OutboundMonitorReport outboundMonitorReport;

    private String registerKey;

    private ConsumeType consumeType;

    private String srcRegion;

    private String dstRegion;

    private long transactionSize;

    private long tableMapSize;

    private String dbName;

    public MonitorFilter(OutFilterChainContext context) {
        this.outboundMonitorReport = context.getOutboundMonitorReport();
        this.registerKey = context.getRegisterKey();
        this.consumeType = context.getConsumeType();
        this.srcRegion = context.getSrcRegion();
        this.dstRegion = context.getDstRegion();
    }

    @Override
    public boolean doFilter(OutboundLogEventContext value) {
        boolean skipEvent = doNext(value, value.isSkipEvent());

        if (skipEvent) {
            return true;
        }

        LogEventType eventType = value.getEventType();
        boolean trafficCountChange = DynamicConfig.getInstance().getTrafficCountChangeSwitch();

        //TODO: for test, can remove
        if (ConsumeType.Replicator == consumeType) {
            outboundMonitorReport.addSize(value.getEventSize());
        }

        if (gtid_log_event == eventType) {
            transactionSize = value.getEventSize();
            outboundMonitorReport.addOutboundGtid(registerKey, value.getGtid());
            outboundMonitorReport.addOneCount();
        } else if (table_map_log_event == eventType) {
            tableMapSize = value.getEventSize();
            if (!trafficCountChange) {
                transactionSize += value.getEventSize();
            }
            if (ConsumeType.Replicator == consumeType) {
                dbName = registerKey;
            } else {
                dbName = ((TableMapLogEvent) value.getLogEvent()).getSchemaName();
            }
        } else if (LogEventUtils.isRowsEvent(eventType)) {
            if (trafficCountChange) {
                transactionSize += value.getEventSize() + tableMapSize;
            } else {
                transactionSize += value.getEventSize();
            }
            tableMapSize = 0;
        } else if (xid_log_event == eventType) {
            transactionSize += value.getEventSize();
            outboundMonitorReport.updateTrafficStatistic(new TrafficStatisticKey(dbName, srcRegion, dstRegion, consumeType.name()), transactionSize);
            clear();
        }

        return false;
    }

    private void clear() {
        transactionSize = 0;
        tableMapSize = 0;
        dbName = StringUtils.EMPTY;
    }
}
