package com.ctrip.framework.drc.applier.activity.event;

import com.ctrip.framework.drc.applier.activity.replicator.converter.TransactionTableApplierByteBufConverter;
import com.ctrip.framework.drc.applier.activity.replicator.driver.ApplierPooledConnector;
import com.ctrip.framework.drc.fetcher.resource.position.TransactionTable;
import com.ctrip.framework.drc.core.driver.binlog.gtid.Gtid;
import com.ctrip.framework.drc.core.driver.binlog.gtid.GtidSet;
import com.ctrip.framework.drc.fetcher.activity.replicator.FetcherSlaveServer;
import com.ctrip.framework.drc.fetcher.event.ApplierDrcGtidEvent;
import com.ctrip.framework.drc.fetcher.event.ApplierGtidEvent;
import com.ctrip.framework.drc.fetcher.event.ApplierXidEvent;
import com.ctrip.framework.drc.fetcher.event.FetcherEvent;
import com.ctrip.framework.drc.fetcher.system.InstanceResource;
import com.ctrip.xpipe.utils.VisibleForTesting;

import static com.ctrip.framework.drc.fetcher.resource.position.TransactionTableResource.TRANSACTION_TABLE_SIZE;

/**
 * Created by jixinwang on 2021/8/20
 */
public class TransactionTableApplierDumpEventActivity extends ApplierDumpEventActivity {

    private String lastUuid;

    private int filterCount;

    private boolean needFilter;

    @InstanceResource
    public TransactionTable transactionTable;

    @Override
    protected FetcherSlaveServer getFetcherSlaveServer() {
        return new FetcherSlaveServer(config, new ApplierPooledConnector(config.getEndpoint()), new TransactionTableApplierByteBufConverter());
    }

    @Override
    protected void handleApplierDrcGtidEvent(FetcherEvent event) {
        Gtid gtid = Gtid.from((ApplierDrcGtidEvent) event);
        loggerER.info("{} {} - RECEIVED - {}", registryKey, gtid, event.getClass().getSimpleName());
        transactionTable.recordToMemory(gtid);
        updateContextGtidSet(gtid);
    }

    @Override
    protected void handleApplierGtidEvent(ApplierGtidEvent event) {
        String currentUuid = event.getServerUUID().toString();
        if (!currentUuid.equalsIgnoreCase(lastUuid)) {
            loggerTT.info("[Merge][{}] uuid has changed, old uuid is: {}, new uuid is: {}", registryKey, lastUuid, currentUuid);
            GtidSet gtidSet = transactionTable.mergeRecord(currentUuid, true);
            updateContextGtidSet(gtidSet);
            lastUuid = currentUuid;
            filterCount = 0;
            needFilter = true;
        }

        if (needFilter) {
            String gtid = event.getGtid();
            GtidSet gtidSet = context.fetchGtidSet();

            if (skipEvent = new GtidSet(gtid).isContainedWithin(gtidSet)) {
                loggerTT.info("[Skip] skip gtid: {}", gtid);
            }
            if (filterCount < TRANSACTION_TABLE_SIZE) {
                filterCount++;
            } else {
                needFilter = false;
                skipEvent = false;
            }
        }

        super.handleApplierGtidEvent(event);
    }

    @Override
    protected void persistPosition(GtidSet gtidSet) {
        transactionTable.merge(gtidSet);
        loggerTT.info("[Merge][{}] transaction table init merge: {}", registryKey, gtidSet.toString());
    }

    @Override
    protected void persistPosition(Gtid gtid) {
        transactionTable.recordToMemory(gtid);
    }

    @Override
    protected boolean shouldSkip(FetcherEvent event) {
        if (event instanceof ApplierDrcGtidEvent) {
            return true;
        }
        if (skipEvent && event instanceof ApplierXidEvent) {
            skipEvent = false;
            return true;
        }
        return skipEvent;
    }

    @VisibleForTesting
    public boolean isNeedFilter() {
        return needFilter;
    }
}
