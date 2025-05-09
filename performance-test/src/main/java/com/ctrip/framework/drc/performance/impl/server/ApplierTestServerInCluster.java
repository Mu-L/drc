package com.ctrip.framework.drc.performance.impl.server;

import com.ctrip.framework.drc.fetcher.activity.event.ApplierGroupActivity;
import com.ctrip.framework.drc.applier.activity.event.ApplyActivity;
import com.ctrip.framework.drc.fetcher.activity.event.DispatchActivity;
import com.ctrip.framework.drc.applier.activity.monitor.MetricsActivity;
import com.ctrip.framework.drc.applier.activity.monitor.ReportConflictActivity;
import com.ctrip.framework.drc.fetcher.resource.position.TransactionTableResource;
import com.ctrip.framework.drc.fetcher.resource.condition.LWMResource;
import com.ctrip.framework.drc.fetcher.resource.condition.ProgressResource;
import com.ctrip.framework.drc.applier.resource.mysql.DataSourceResource;
import com.ctrip.framework.drc.applier.server.ApplierServerInCluster;
import com.ctrip.framework.drc.core.server.config.applier.dto.ApplierConfigDto;
import com.ctrip.framework.drc.fetcher.activity.event.InvolveActivity;
import com.ctrip.framework.drc.fetcher.activity.event.LoadEventActivity;
import com.ctrip.framework.drc.fetcher.resource.condition.CapacityResource;
import com.ctrip.framework.drc.fetcher.resource.condition.ListenableDirectMemoryResource;
import com.ctrip.framework.drc.fetcher.resource.context.LinkContextResource;
import com.ctrip.framework.drc.fetcher.resource.thread.ExecutorResource;
import com.ctrip.framework.drc.performance.activity.LocalCommitActivity;
import com.ctrip.framework.drc.performance.activity.LocalDumpEventActivity;

/**
 * Created by jixinwang on 2021/8/16
 */
public class ApplierTestServerInCluster extends ApplierServerInCluster {

    public ApplierTestServerInCluster(ApplierConfigDto config) throws Exception {
        super(config);
    }

    @Override
    public void define() throws Exception {
        source(LocalDumpEventActivity.class)
                .with(ExecutorResource.class)
                .with(LinkContextResource.class)
                .with(DataSourceResource.class)
                .with(LWMResource.class)
                .with(ProgressResource.class)
                .with(CapacityResource.class)
                .with(ListenableDirectMemoryResource.class)
                .with(TransactionTableResource.class)
                .with(MetricsActivity.class)
                .with(ReportConflictActivity.class)
                .with(LoadEventActivity.class)
                .link(InvolveActivity.class)
                .link(ApplierGroupActivity.class)
                .link(DispatchActivity.class)
                .link(ApplyActivity.class, 100)
                .link(LocalCommitActivity.class);
        check();
    }
}
