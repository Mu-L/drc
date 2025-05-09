package com.ctrip.framework.drc.replicator.impl.oubound.filter.extract;

import com.ctrip.framework.drc.core.meta.DataMediaConfig;
import com.ctrip.framework.drc.core.monitor.kpi.OutboundMonitorReport;
import com.ctrip.framework.drc.core.server.common.filter.row.RowsFilterContext;
import com.ctrip.framework.drc.replicator.impl.oubound.filter.context.ExtractContext;

/**
 * Created by jixinwang on 2022/12/29
 */
public class ExtractFilterChainContext {

    private DataMediaConfig dataMediaConfig;

    private OutboundMonitorReport outboundMonitorReport;

    private RowsFilterContext rowsFilterContext;

    public ExtractFilterChainContext(DataMediaConfig dataMediaConfig, OutboundMonitorReport outboundMonitorReport,
                                     RowsFilterContext rowsFilterContext) {
        this.dataMediaConfig = dataMediaConfig;
        this.outboundMonitorReport = outboundMonitorReport;
        this.rowsFilterContext = rowsFilterContext;
    }

    public DataMediaConfig getDataMediaConfig() {
        return dataMediaConfig;
    }

    public void setDataMediaConfig(DataMediaConfig dataMediaConfig) {
        this.dataMediaConfig = dataMediaConfig;
    }

    public OutboundMonitorReport getOutboundMonitorReport() {
        return outboundMonitorReport;
    }

    public void setOutboundMonitorReport(OutboundMonitorReport outboundMonitorReport) {
        this.outboundMonitorReport = outboundMonitorReport;
    }

    public boolean shouldFilterRows() {
        if (dataMediaConfig == null) {
            return false;
        }
        return dataMediaConfig.shouldFilterRows();
    }

    public boolean shouldFilterColumns() {
        if (dataMediaConfig == null) {
            return false;
        }
        return dataMediaConfig.shouldFilterColumns();
    }

    public RowsFilterContext getRowsFilterContext() {
        return rowsFilterContext;
    }

    public void setRowsFilterContext(RowsFilterContext rowsFilterContext) {
        this.rowsFilterContext = rowsFilterContext;
    }

    public static ExtractFilterChainContext from(ExtractContext outboundFilterChainContext, RowsFilterContext rowsFilterContext) {
        rowsFilterContext.setSrcRegion(outboundFilterChainContext.getSrcRegion());
        rowsFilterContext.setDstRegion(outboundFilterChainContext.getDstRegion());
        return new ExtractFilterChainContext(outboundFilterChainContext.getDataMediaConfig(),
                outboundFilterChainContext.getOutboundMonitorReport(), rowsFilterContext);
    }
}
