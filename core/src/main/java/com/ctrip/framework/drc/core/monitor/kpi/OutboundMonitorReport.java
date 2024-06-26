package com.ctrip.framework.drc.core.monitor.kpi;

import com.ctrip.framework.drc.core.driver.schema.data.TableKey;
import com.ctrip.framework.drc.core.monitor.entity.TrafficStatisticEntity;
import com.ctrip.framework.drc.core.monitor.entity.TrafficStatisticKey;
import com.ctrip.framework.drc.core.monitor.entity.RowsFilterEntity;
import com.ctrip.framework.drc.core.monitor.entity.TrafficEntity;
import com.ctrip.framework.drc.core.monitor.reporter.DefaultEventMonitorHolder;
import com.ctrip.framework.drc.core.server.common.filter.ExtractType;
import com.ctrip.xpipe.utils.VisibleForTesting;
import com.google.common.collect.Maps;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @Author limingdong
 * @create 2020/3/14
 */
public class OutboundMonitorReport extends AbstractMonitorReport {

    private static final String OUTBOUND_GTID = "DRC.replicator.outbound.gtid";

    private Map<String, AtomicLong> outboundGtid = Maps.newConcurrentMap();

    private Map<TableKey, RowsFilterEntity> rowsFilterEntityMap = Maps.newConcurrentMap();

    private Map<TrafficStatisticKey, TrafficStatisticEntity> trafficStatisticEntityMap = Maps.newConcurrentMap();

    public OutboundMonitorReport(long domain, TrafficEntity trafficEntity) {
        super(domain, trafficEntity);
    }

    @Override
    protected void doMonitor() {

        for (Map.Entry<String, AtomicLong> entry : outboundGtid.entrySet()) {
            AtomicLong atomicLong = entry.getValue();
            DefaultEventMonitorHolder.getInstance().logEvent(OUTBOUND_GTID, entry.getKey(), atomicLong.getAndSet(0));
        }

        for (Map.Entry<TableKey, RowsFilterEntity> entry : rowsFilterEntityMap.entrySet()) {
            RowsFilterEntity rowsFilterEntity = entry.getValue();
            hickwallReporter.reportRowsFilter(rowsFilterEntity);
            rowsFilterEntity.clearCount();
        }

        for (Map.Entry<TrafficStatisticKey, TrafficStatisticEntity> entry : trafficStatisticEntityMap.entrySet()) {
            TrafficStatisticEntity trafficStatisticEntity = entry.getValue();
            hickwallReporter.reportTrafficStatistic(trafficStatisticEntity);
            trafficStatisticEntity.clearCount();
        }
    }

    public void addOutboundGtid(String applierName, String gtid) {
        AtomicLong atomicLong = outboundGtid.get(applierName);
        if(atomicLong == null) {
            atomicLong = new AtomicLong(0);
            outboundGtid.put(applierName, atomicLong);
        }
        atomicLong.addAndGet(1);
        delayMonitorReport.sendGtid(gtid);
    }

    public void updateFilteredRows(String dbName, String tableName, int beforeSize, int afterSize, ExtractType extractType) {
        RowsFilterEntity rowsFilterEntity = getRowsFilterEntity(dbName, tableName, extractType);
        rowsFilterEntity.updateCount(beforeSize, afterSize);
    }

    private RowsFilterEntity getRowsFilterEntity(String dbName, String tableName, ExtractType extractType) {
        TableKey tableKey = TableKey.from(dbName, tableName);
        RowsFilterEntity rowsFilterEntity = rowsFilterEntityMap.get(tableKey);
        if (rowsFilterEntity == null) {
            rowsFilterEntity = new RowsFilterEntity.Builder()
                    .dcName(this.trafficEntity.getDcName())
                    .buName(this.trafficEntity.getBuName())
                    .mha(this.trafficEntity.getMhaName())
                    .clusterAppId(this.trafficEntity.getClusterAppId())
                    .registryKey(this.trafficEntity.getRegistryKey())
                    .clusterName(this.trafficEntity.getClusterName())
                    .mysqlIp(this.trafficEntity.getIp())
                    .mysqlPort(this.trafficEntity.getPort())
                    .dbName(dbName)
                    .tableName(tableName).build();
            rowsFilterEntityMap.put(tableKey, rowsFilterEntity);
        }
        Map<String, String> tags = rowsFilterEntity.getTags();
        tags.put("extract", extractType.getName());
        return rowsFilterEntity;
    }

    public void updateTrafficStatistic(TrafficStatisticKey trafficStatisticKey, long eventSize) {
        TrafficStatisticEntity trafficStatisticEntity = getTrafficStatistic(trafficStatisticKey);
        trafficStatisticEntity.updateCount(eventSize);
    }

    private TrafficStatisticEntity getTrafficStatistic(TrafficStatisticKey trafficStatisticKey) {
        TrafficStatisticEntity trafficStatisticEntity = trafficStatisticEntityMap.get(trafficStatisticKey);
        if (trafficStatisticEntity == null) {
            trafficStatisticEntity = new TrafficStatisticEntity.Builder()
                    .dcName(this.trafficEntity.getDcName())
                    .buName(this.trafficEntity.getBuName())
                    .mha(this.trafficEntity.getMhaName())
                    .clusterAppId(this.trafficEntity.getClusterAppId())
                    .registryKey(this.trafficEntity.getRegistryKey())
                    .clusterName(this.trafficEntity.getClusterName())
                    .mysqlIp(this.trafficEntity.getIp())
                    .mysqlPort(this.trafficEntity.getPort())
                    .dbName(trafficStatisticKey.getDbName())
                    .srcRegion(trafficStatisticKey.getSrcRegion())
                    .dstRegion(trafficStatisticKey.getDstRegion())
                    .dstType(trafficStatisticKey.getDstType()).build();
            trafficStatisticEntityMap.put(trafficStatisticKey, trafficStatisticEntity);
        }
        return trafficStatisticEntity;
    }

    public String getClusterName() {
        return trafficEntity.getClusterName();
    }

    @VisibleForTesting
    public Map<TableKey, RowsFilterEntity> getRowsFilterEntityMap() {
        return rowsFilterEntityMap;
    }
}
