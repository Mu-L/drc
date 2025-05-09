package com.ctrip.framework.drc.fetcher.resource.context;

import com.ctrip.framework.drc.core.driver.binlog.gtid.GtidSet;
import com.ctrip.framework.drc.core.driver.binlog.gtid.db.DbTransactionTableGtidReader;
import com.ctrip.framework.drc.core.driver.binlog.gtid.db.GtidReader;
import com.ctrip.framework.drc.core.driver.binlog.gtid.db.ShowMasterGtidReader;
import com.ctrip.framework.drc.core.driver.binlog.gtid.db.TransactionTableGtidReader;
import com.ctrip.framework.drc.core.driver.command.netty.endpoint.DefaultEndPoint;
import com.ctrip.framework.drc.core.driver.command.netty.endpoint.KeyedEndPoint;
import com.ctrip.framework.drc.core.driver.healthcheck.task.ExecutedGtidQueryTask;
import com.ctrip.framework.drc.core.server.config.applier.dto.ApplyMode;
import com.ctrip.framework.drc.fetcher.resource.position.MessengerGtidQueryTask;
import com.ctrip.framework.drc.fetcher.system.InstanceConfig;
import com.ctrip.framework.drc.fetcher.system.InstanceResource;
import com.ctrip.xpipe.api.endpoint.Endpoint;
import com.ctrip.xpipe.utils.VisibleForTesting;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Objects;

import static com.ctrip.framework.drc.core.server.config.SystemConfig.isIntegrityTest;

/**
 * @Author Slight
 * Dec 20, 2019
 */
public class NetworkContextResource extends AbstractContext implements EventGroupContext {

    @InstanceConfig(path = "gtidExecuted")
    public String initialGtidExecuted;

    @InstanceConfig(path = "registryKey")
    public String registryKey;

    @InstanceConfig(path = "applyMode")
    public int applyMode;

    @InstanceConfig(path = "includedDbs")
    public String includedDbs;

    @InstanceResource
    public MqPosition mqPosition;

    @InstanceConfig(path = "target.ip")
    public String ip = "";

    @InstanceConfig(path = "target.port")
    public int port;

    @InstanceConfig(path = "target.username")
    public String username;

    @InstanceConfig(path = "target.password")
    public String password;

    @VisibleForTesting
    protected volatile boolean emptyPositionFromDb = false;

    @Override
    public void doInitialize() throws Exception {
        super.doInitialize();
        GtidSet executedGtidSet = unionGtidSet(initialGtidExecuted);
        if (isIntegrityTest()) {
            executedGtidSet = new GtidSet(StringUtils.EMPTY);
        }
        updateGtidSet(executedGtidSet);
        updateGtid("");
    }

    public GtidSet queryTheNewestGtidset() {
        if (isIntegrityTest()) {
            return fetchGtidSet();
        }

        if (emptyPositionFromDb) {
            GtidSet executedGtidSet = unionGtidSet(initialGtidExecuted);
            updateGtidSet(executedGtidSet);
        }

        return fetchGtidSet();
    }

    private GtidSet unionGtidSet(String initialGtidExecuted) {
        GtidSet executedGtidSet = new GtidSet(StringUtils.EMPTY);
        executedGtidSet = executedGtidSet.union(new GtidSet(initialGtidExecuted));
        logger.info("[{}][NETWORK GTID] initial position: {}", registryKey, executedGtidSet);

        ApplyMode applyMode = ApplyMode.getApplyMode(this.applyMode);
        switch (applyMode) {
            case set_gtid:
            case transaction_table:
            case db_transaction_table:
                executedGtidSet = unionPositionFromDb(executedGtidSet);
                break;
            case mq:
            case db_mq:
            case kafka:
                executedGtidSet = unionMessengerPositionFromDb(executedGtidSet);
                break;
        }

        logger.info("[{}][NETWORK GTID]union emptyPositionFromDb: {}, executed gtidset: {}", registryKey, emptyPositionFromDb, executedGtidSet);
        return emptyPositionFromDb ? new GtidSet(StringUtils.EMPTY) : executedGtidSet;
    }

    private GtidSet unionPositionFromDb(GtidSet gtidSet) {
        GtidSet positionFromDb = queryPositionFromDb();
        logger.info("[{}][NETWORK GTID] db position: {}", registryKey, positionFromDb);
        return gtidSet.union(positionFromDb);
    }

    private GtidSet unionMessengerPositionFromDb(GtidSet gtidSet) {
        GtidSet positionFromMessenger = queryMessengerPositionFromDb();
        logger.info("[{}][NETWORK GTID] db messenger position: {}", registryKey, positionFromMessenger);
        return gtidSet.union(positionFromMessenger);
    }

    @VisibleForTesting
    protected GtidSet queryPositionFromDb() {
        Endpoint endpoint = new KeyedEndPoint(registryKey, new DefaultEndPoint(ip, port, username, password));
        List<GtidReader> gtidReaders = this.getExecutedGtidReaders(endpoint);
        ExecutedGtidQueryTask queryTask = new ExecutedGtidQueryTask(endpoint, gtidReaders);
        String gtidSet = queryTask.doQuery();
        emptyPositionFromDb = StringUtils.isBlank(gtidSet);
        return new GtidSet(gtidSet);
    }

    protected GtidSet queryMessengerPositionFromDb() {
        Endpoint endpoint = new KeyedEndPoint(registryKey, new DefaultEndPoint(ip, port, username, password));
        MessengerGtidQueryTask gtidQueryTask = new MessengerGtidQueryTask(endpoint, registryKey);
        Pair<String, Boolean> executedGtid = gtidQueryTask.getExecutedGtid();
        emptyPositionFromDb = !executedGtid.getRight();
        return new GtidSet(executedGtid.getLeft());
    }

    @VisibleForTesting
    protected List<GtidReader> getExecutedGtidReaders(Endpoint endpoint) {
        List<GtidReader> gtidReaders;
        if (ApplyMode.db_transaction_table == ApplyMode.getApplyMode(applyMode)) {
            gtidReaders = Lists.newArrayList(new ShowMasterGtidReader(), new TransactionTableGtidReader(endpoint), new DbTransactionTableGtidReader(endpoint, Objects.requireNonNull(includedDbs)));
        } else {
            gtidReaders = Lists.newArrayList(new ShowMasterGtidReader(), new TransactionTableGtidReader(endpoint));
        }
        return gtidReaders;
    }
}
