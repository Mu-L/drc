package com.ctrip.framework.drc.replicator.impl.oubound.handler;

import com.ctrip.framework.drc.core.driver.binlog.gtid.GtidManager;
import com.ctrip.framework.drc.core.driver.binlog.gtid.GtidSet;
import com.ctrip.framework.drc.core.driver.binlog.impl.DrcDdlLogEvent;
import com.ctrip.framework.drc.core.driver.binlog.impl.GtidLogEvent;
import com.ctrip.framework.drc.core.driver.binlog.impl.TableMapLogEvent;
import com.ctrip.framework.drc.core.driver.binlog.manager.SchemaManager;
import com.ctrip.framework.drc.core.driver.command.packet.applier.ApplierDumpCommandPacket;
import com.ctrip.framework.drc.core.monitor.kpi.OutboundMonitorReport;
import com.ctrip.framework.drc.core.server.common.enums.ConsumeType;
import com.ctrip.framework.drc.core.server.common.enums.RowsFilterType;
import com.ctrip.framework.drc.core.server.config.SystemConfig;
import com.ctrip.framework.drc.core.server.config.applier.dto.ApplyMode;
import com.ctrip.framework.drc.core.server.config.replicator.ReplicatorConfig;
import com.ctrip.framework.drc.replicator.container.zookeeper.UuidConfig;
import com.ctrip.framework.drc.replicator.container.zookeeper.UuidOperator;
import com.ctrip.framework.drc.replicator.impl.oubound.channel.ChannelAttributeKey;
import com.ctrip.framework.drc.replicator.store.AbstractTransactionTest;
import com.ctrip.framework.drc.replicator.store.manager.file.DefaultFileManager;
import com.ctrip.framework.drc.replicator.store.manager.gtid.DefaultGtidManager;
import com.ctrip.xpipe.netty.commands.NettyClient;
import com.ctrip.framework.drc.core.utils.ScheduleCloseGate;
import com.google.common.collect.Sets;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.DefaultFileRegion;
import io.netty.util.Attribute;
import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.Set;
import java.util.UUID;

import static com.ctrip.framework.drc.replicator.AllTests.ROW_FILTER_PROPERTIES;

/**
 * @Author limingdong
 * @create 2020/6/22
 */
@FixMethodOrder(value = MethodSorters.NAME_ASCENDING)
public class ApplierRegisterCommandHandlerTest extends AbstractTransactionTest {

    @InjectMocks
    private ApplierRegisterCommandHandlerBefore applierRegisterCommandHandler;

    @Mock
    private Channel channel;

    @Mock
    private ApplierDumpCommandPacket dumpCommandPacket;

    @Mock
    private OutboundMonitorReport outboundMonitorReport;

    @Mock
    private NettyClient nettyClient;

    @Mock
    private ChannelFuture channelFuture;

    @Mock
    private SchemaManager schemaManager;

    @Mock
    private ReplicatorConfig replicatorConfig;

    @Mock
    private UuidOperator uuidOperator;

    @Mock
    private UuidConfig uuidConfig;

    @Mock
    private Attribute<ChannelAttributeKey> attribute;

    @Mock
    private ChannelAttributeKey channelAttributeKey;

    @Mock
    private ScheduleCloseGate gate;

    private static final String APPLIER_NAME = "ut_applier_name";

    private static final String LOCAL_HOST = "127.0.0.1";

    private static final String LAST_FILE_NAME = "rbinlog.0000000004";

    //send file 1 and 4
    private static final GtidSet EXCLUDED_GTID = new GtidSet("c372080a-1804-11ea-8add-98039bbedf9c:1-1510:1512-2000");

    private Set<UUID> uuids = Sets.newHashSet();

    private InetSocketAddress socketAddress = new InetSocketAddress(LOCAL_HOST, 8080);

    private GtidManager gtidManager;

    @Before
    public void setUp() throws Exception {
        System.setProperty(SystemConfig.REPLICATOR_WHITE_LIST, String.valueOf(true));
        super.initMocks();
        when(replicatorConfig.getWhiteUUID()).thenReturn(uuids);
        when(replicatorConfig.getRegistryKey()).thenReturn("");
        when(replicatorConfig.getApplyMode()).thenReturn(ApplyMode.set_gtid.getType());
        when(uuidOperator.getUuids(anyString())).thenReturn(uuidConfig);
        when(uuidConfig.getUuids()).thenReturn(Sets.newHashSet("c372080a-1804-11ea-8add-98039bbedf9c"));

        fileManager = new DefaultFileManager(schemaManager, APPLIER_NAME);
        gtidManager = new DefaultGtidManager(fileManager, uuidOperator, replicatorConfig);
        fileManager.initialize();
        fileManager.start();
        fileManager.setGtidManager(gtidManager);

        applierRegisterCommandHandler = new ApplierRegisterCommandHandlerBefore(gtidManager, fileManager, outboundMonitorReport, replicatorConfig);

        createFiles();

        when(dumpCommandPacket.getApplierName()).thenReturn(APPLIER_NAME);
        when(nettyClient.channel()).thenReturn(channel);
        when(channel.remoteAddress()).thenReturn(socketAddress);
        when(channel.closeFuture()).thenReturn(channelFuture);
        when(dumpCommandPacket.getConsumeType()).thenReturn(ConsumeType.Replicator.getCode());
        when(dumpCommandPacket.getProperties()).thenReturn(String.format(ROW_FILTER_PROPERTIES, RowsFilterType.None.getName()));
        when(dumpCommandPacket.getApplyMode()).thenReturn(ApplyMode.set_gtid.getType());
        when(dumpCommandPacket.getGtidSet()).thenReturn(EXCLUDED_GTID);
        when(dumpCommandPacket.getNameFilter()).thenReturn("drc1\\..*,drc2\\..*,drc3\\..*,drc4\\..*");
        when(channel.attr(ReplicatorMasterHandler.KEY_CLIENT)).thenReturn(attribute);
        when(attribute.get()).thenReturn(channelAttributeKey);
        when(channelAttributeKey.getGate()).thenReturn(gate);
    }

    @After
    public void tearDown() throws Exception {
        System.setProperty(SystemConfig.REPLICATOR_WHITE_LIST, String.valueOf(false));
        File logDir = fileManager.getDataDir();
        deleteFiles(logDir);
        fileManager.stop();
        fileManager.dispose();
    }

    @Test
    public void test_01_SkipFiles() throws InterruptedException {
        applierRegisterCommandHandler.handle(dumpCommandPacket, nettyClient);

        while (!LAST_FILE_NAME.equalsIgnoreCase(fileManager.getCurrentLogFile().getName())) {
            Thread.sleep(100);
        }
        Thread.sleep(300);

        verify(channel, times(2 * 4 + 1)).writeAndFlush(any(DefaultFileRegion.class));  //format_log、previous_log、drc_uuid_log、gtid in last two files, switch file will send empty msg
        verify(outboundMonitorReport, times(2)).addOutboundGtid(anyString(), anyString());
        verify(outboundMonitorReport, times(2)).addOneCount();
        verify(channelAttributeKey, times(1)).setHeartBeat(false);
    }

    @Test
    public void test_02_DrcTableMapLogEvent() throws Exception {
        createDrcTableMapLogEventFiles();
        applierRegisterCommandHandler.handle(dumpCommandPacket, nettyClient);
        Thread.sleep(250);

        verify(channel, times( 1 + 1 + 1 + 1)).writeAndFlush(any(DefaultFileRegion.class));  //format_log、previous_log、drc_uuid_log、xid
        verify(outboundMonitorReport, times(0)).addOutboundGtid(anyString(), anyString());
        verify(outboundMonitorReport, times(0)).addOneCount();
        verify(channelAttributeKey, times(1)).setHeartBeat(false);
    }

    @Test
    public void test_03_DrcDdlLogEvent() throws Exception {
        createDrcDdlLogEventFiles();
        applierRegisterCommandHandler.handle(dumpCommandPacket, nettyClient);
        Thread.sleep(250);

        verify(channel, times( 5)).writeAndFlush(any(DefaultFileRegion.class));  //ddl、drc table map、 format_log、previous_log、drc_uuid_log
        verify(outboundMonitorReport, times(0)).addOutboundGtid(anyString(), anyString());
        verify(outboundMonitorReport, times(0)).addOneCount();
        verify(channelAttributeKey, times(1)).setHeartBeat(false);
    }

    @Test
    public void test_04_DrcIndexLogEvent() throws Exception {
        GtidSet gtidSet = new GtidSet("c372080a-1804-11ea-8add-98039bbedf9c:1-3500");
        when(dumpCommandPacket.getGtidSet()).thenReturn(gtidSet);

        int transactionSize = createDrcIndexLogEventFiles();
        applierRegisterCommandHandler.handle(dumpCommandPacket, nettyClient);
        Thread.sleep(250);

        verify(channel, Mockito.atLeast( 2 + transactionSize * 4)).writeAndFlush(any(DefaultFileRegion.class));  //ddl、drc table map
        verify(channelAttributeKey, times(1)).setHeartBeat(false);
    }

    @Test
    public void test_04_DrcIndexLogEventWithDdl() throws Exception {
        GtidSet gtidSet = new GtidSet("c372080a-1804-11ea-8add-98039bbedf9c:1-3500");
        when(dumpCommandPacket.getGtidSet()).thenReturn(gtidSet);

        int transactionSize = createDrcIndexLogEventFilesWithDdl();
        applierRegisterCommandHandler.handle(dumpCommandPacket, nettyClient);
        Thread.sleep(250);

        verify(channel, Mockito.atLeast( 2 + transactionSize * 4)).writeAndFlush(any(DefaultFileRegion.class));  //ddl、drc table map
        verify(channelAttributeKey, times(1)).setHeartBeat(false);
    }

    @Test
    public void test_05_DrcIndexLogEventWithGtidsetNotInclude() throws Exception {
        GtidSet gtidSet = new GtidSet("c372080a-1804-11ea-8add-98039bbedf9c:1-2001:2003-3500");
        when(dumpCommandPacket.getGtidSet()).thenReturn(gtidSet);

        int transactionSize = createDrcIndexLogEventFiles();
        applierRegisterCommandHandler.handle(dumpCommandPacket, nettyClient);
        Thread.sleep(250);

        verify(channel, Mockito.atLeast( 2 + transactionSize * 4)).writeAndFlush(any(DefaultFileRegion.class));  //ddl、drc table map
        verify(channelAttributeKey, times(1)).setHeartBeat(false);
    }

    @Test
    public void test_06_DrcIndexLogEventWithRealGtidsetInclude() throws Exception {
        int maxGtidId = testIndexFileWithDiffGtidSetAndDdl();
        GtidSet gtidSet = new GtidSet(UUID_STRING + ":1-" + maxGtidId);
        when(dumpCommandPacket.getGtidSet()).thenReturn(gtidSet);

        applierRegisterCommandHandler.handle(dumpCommandPacket, nettyClient);
        Thread.sleep(250);

        verify(channel, Mockito.times( 2/*format_log、previous_log*/ + 2/*ddl with 2 events, drc_ddl and drc_talbe_map*/ + 1 /*previous_gtid_log_event in the mid file*/ + 1 /*drc_uuid_log_event in the mid file*/ + 1 /*empty msg to close file channel*/)).writeAndFlush(any(DefaultFileRegion.class));
        verify(channelAttributeKey, times(1)).setHeartBeat(false);
    }

    @Test
    public void test_07_DrcIndexLogEventWithRealGtidsetNotInclude() throws Exception {
        int maxGtidId = testIndexFileWithDiffGtidSetAndDdl();
        int ddlId = (maxGtidId - 1) / 2;
        GtidSet gtidSet = new GtidSet(UUID_STRING + ":" + ddlId);
        when(dumpCommandPacket.getGtidSet()).thenReturn(gtidSet);

        applierRegisterCommandHandler.handle(dumpCommandPacket, nettyClient);
        Thread.sleep(500);

        verify(channel, Mockito.times( 2/*format_log、previous_log*/ + 2/*ddl with 2 events, drc_ddl and drc_talbe_map*/ + 1 /*previous_gtid_log_event in the mid file*/ + 1 /*drc_uuid_log_event in the mid file*/ + (maxGtidId - 1) * 4 /*transaction size*/ + 1 /*empty msg to close file channel*/)).writeAndFlush(any(DefaultFileRegion.class));
        verify(channelAttributeKey, times(1)).setHeartBeat(false);
    }

    @Test
    public void test_08_DrcIndexLogEventWithRealGtidsetWithGapBeforeDdl() throws Exception {
        int maxGtidId = testIndexFileWithDiffGtidSetAndDdl();
        int ddlId = (maxGtidId - 1) / 2;
        GtidSet gtidSet = new GtidSet(UUID_STRING + ":1-" + (ddlId - 3) + ":" + (ddlId - 1) + "-" + maxGtidId);
        when(dumpCommandPacket.getGtidSet()).thenReturn(gtidSet);

        applierRegisterCommandHandler.handle(dumpCommandPacket, nettyClient);
        Thread.sleep(250);

        verify(channel, Mockito.times( 2/*format_log、previous_log*/ + 2/*ddl with 2 events, drc_ddl and drc_talbe_map*/ + 1 /*previous_gtid_log_event in the mid file*/ + 1 /*drc_uuid_log_event in the mid file*/ + 4 /*exclude transaction*/ + 1 /*empty msg to close file channel*/)).writeAndFlush(any(DefaultFileRegion.class));
        verify(channelAttributeKey, times(1)).setHeartBeat(false);
    }

    @Test
    public void test_09_DrcIndexLogEventWithRealGtidsetWithGapAfterDdl() throws Exception {
        int maxGtidId = testIndexFileWithDiffGtidSetAndDdl();
        int ddlId = (maxGtidId - 1) / 2;
        GtidSet gtidSet = new GtidSet(UUID_STRING + ":1-" + (ddlId + 1) + ":" + (ddlId + 3) + "-" + maxGtidId);
        when(dumpCommandPacket.getGtidSet()).thenReturn(gtidSet);

        applierRegisterCommandHandler.handle(dumpCommandPacket, nettyClient);
        Thread.sleep(250);

        verify(channel, Mockito.times( 2/*format_log、previous_log*/ + 2/*ddl with 2 events, drc_ddl and drc_talbe_map*/ + 1 /*previous_gtid_log_event in the mid file*/ + 1 /*drc_uuid_log_event in the mid file*/ + 4 /*exclude transaction*/ + 1 /*empty msg to close file channel*/)).writeAndFlush(any(DefaultFileRegion.class));
        verify(channelAttributeKey, times(1)).setHeartBeat(false);
    }

    @Test
    public void test_10_DrcGtidLogEvent_slave_consume() throws Exception {
        int maxGtidId = testDrcGtidLogEvent();
        int ddlId = (maxGtidId - 1) / 2;
        GtidSet gtidSet = new GtidSet(UUID_STRING + ":1-" + (ddlId + 1));
        when(dumpCommandPacket.getGtidSet()).thenReturn(gtidSet);
        when(dumpCommandPacket.getConsumeType()).thenReturn(ConsumeType.Replicator.getCode());

        applierRegisterCommandHandler.handle(dumpCommandPacket, nettyClient);
        Thread.sleep(250);

        int numTransaction = maxGtidId - (ddlId + 1);

        verify(channel, Mockito.times( 2/*format_log、previous_log*/ + 1 /*previous_gtid_log_event in the mid file*/ + 1 /*drc_uuid_log_event in the mid file*/ + numTransaction * 4 /*exclude transaction*/ + 1 /*drc_gtid_log_event*/ + 1 /*empty msg to close file channel*/)).writeAndFlush(any(DefaultFileRegion.class));
        verify(channelAttributeKey, times(1)).setHeartBeat(false);
    }

    @Test
    public void test_10_DrcGtidLogEvent_applier_consume() throws Exception {
        int maxGtidId = testDrcGtidLogEvent();
        int ddlId = (maxGtidId - 1) / 2;
        GtidSet gtidSet = new GtidSet(UUID_STRING + ":1-" + (ddlId + 1));
        when(dumpCommandPacket.getGtidSet()).thenReturn(gtidSet);
        when(dumpCommandPacket.getConsumeType()).thenReturn(ConsumeType.Applier.getCode());

        applierRegisterCommandHandler.handle(dumpCommandPacket, nettyClient);
        Thread.sleep(250);

        int numTransaction = maxGtidId - (ddlId + 1);

        verify(channel, Mockito.times( 2/*format_log、previous_log*/ + 1 /*previous_gtid_log_event in the mid file*/ + 1 /*drc_uuid_log_event in the mid file*/ + numTransaction * 4 /*exclude transaction*/ + 1 /*empty msg to close file channel*/)).writeAndFlush(any(DefaultFileRegion.class));
    }

    @Test
    public void test_11_replicatorSlaveRequestBinlog() throws Exception {
        createMultiUuidsFiles();
        GtidSet gtidSet = new GtidSet("c372080a-1804-11ea-8add-98039bbedf9c:1-1500,c372080a-1804-11ea-8add-98039bbedfad:1-15000");
        when(dumpCommandPacket.getGtidSet()).thenReturn(gtidSet);

        applierRegisterCommandHandler.handle(dumpCommandPacket, nettyClient);
        Thread.sleep(250);
        verify(channel, Mockito.atLeast(4)).writeAndFlush(any(DefaultFileRegion.class)); //4 files and 4 gtid events
        verify(channelAttributeKey, times(1)).setHeartBeat(false);
    }

    @Test
    public void test_12_replicatorSlaveRequestBinlog() throws Exception {
        createMultiUuidsFiles();
        GtidSet gtidSet = new GtidSet("c372080a-1804-11ea-8add-98039bbedf9c:1-3:5-1500,c372080a-1804-11ea-8add-98039bbedfad:1-15000");
        when(dumpCommandPacket.getGtidSet()).thenReturn(gtidSet);

        applierRegisterCommandHandler.handle(dumpCommandPacket, nettyClient);
        Thread.sleep(100);
        verify(channel, Mockito.times(1)).writeAndFlush(any(ByteBuf.class)); //can not find first file and send DrcErrorLogEvent(event)
        verify(channelAttributeKey, times(1)).setHeartBeat(false);
    }

    @Test
    public void test_13_replicatorSlaveRequestBinlog() throws Exception {
        createMultiUuidsFiles();
        GtidSet gtidSet = new GtidSet("c372080a-1804-11ea-8add-98039bbedf9c:1-1830,c372080a-1804-11ea-8add-98039bbedfad:1-15600");
        when(dumpCommandPacket.getGtidSet()).thenReturn(gtidSet);

        applierRegisterCommandHandler.handle(dumpCommandPacket, nettyClient);
        Thread.sleep(250);
        verify(channel, Mockito.atLeast(3)).writeAndFlush(any(DefaultFileRegion.class)); //second file
        verify(channelAttributeKey, times(1)).setHeartBeat(false);
    }


    @Test
    public void test_14_replicatorSlaveRequestBinlog() throws Exception {
        createMultiUuidsFiles_2();
        // file 01->03->05,skip file 02,04
        GtidSet gtidSet = new GtidSet("c372080a-1804-11ea-8add-98039bbedf9c:1-1500:1502-2020,c372080a-1804-11ea-8add-98039bbedfad:1-16000:16002-16500");
        when(dumpCommandPacket.getGtidSet()).thenReturn(gtidSet);

        applierRegisterCommandHandler.handle(dumpCommandPacket, nettyClient);
        Thread.sleep(250);
        verify(channel, Mockito.atLeast(3)).writeAndFlush(any(DefaultFileRegion.class)); // send 3 files
        verify(channelAttributeKey, times(1)).setHeartBeat(false);
    }

    // send 123(tableId) -> db1.table1(tableName), skip 124(tableId) -> db1.table2(tableName)
    @Test
    public void test_15_nameFilter() throws Exception {

        String nameFilter = "db1.table1";
        when(dumpCommandPacket.getNameFilter()).thenReturn(nameFilter);

        createDiffTableRows();
        GtidSet gtidSet = new GtidSet("c372080a-1804-11ea-8add-98039bbedf9c:1-2500");
        when(dumpCommandPacket.getConsumeType()).thenReturn(ConsumeType.Applier.getCode());
        when(dumpCommandPacket.getGtidSet()).thenReturn(gtidSet);

        applierRegisterCommandHandler.handle(dumpCommandPacket, nettyClient);
        Thread.sleep(250);
        verify(channel, Mockito.times(2/*format_log、previous_log*/ + 6 /* db1.table1 */ + 1 /* drc_uuid*/ + 1 /* close fd write empty event*/)).writeAndFlush(any(DefaultFileRegion.class));
        verify(channelAttributeKey, times(0)).setHeartBeat(false);
    }

    public int createDiffDbRows(Set<String> dbNames) throws Exception {
        fileManager = new DefaultFileManager(schemaManager, APPLIER_NAME);
        fileManager.initialize();
        fileManager.start();
        fileManager.setGtidManager(gtidManager);

        File logDir = fileManager.getDataDir();
        deleteFiles(logDir);

        GtidSet gtidSet1 = new GtidSet("c372080a-1804-11ea-8add-98039bbedf9c:1-2000");
        gtidManager.updateExecutedGtids(gtidSet1);
        int eventSize = 0;
        for (String db : dbNames) {
            writeTransaction(db);
            eventSize++;
        }

        GtidSet gtidSet2 = new GtidSet("c372080a-1804-11ea-8add-98039bbedf9c:1-3000");
        gtidManager.updateExecutedGtids(gtidSet2);

        for (String db : dbNames) {
            writeTransaction(db);
            eventSize++;
        }

        GtidSet gtidSet3 = new GtidSet("c372080a-1804-11ea-8add-98039bbedf9c:1-4000");
        gtidManager.updateExecutedGtids(gtidSet3);
        for (String db : dbNames) {
            writeTransaction(db);
            eventSize++;
        }
        fileManager.flush();

        return eventSize;
    }

    public int createDiffTableRows() throws Exception {
        fileManager = new DefaultFileManager(schemaManager, APPLIER_NAME);
        fileManager.initialize();
        fileManager.start();
        fileManager.setGtidManager(gtidManager);

        File logDir = fileManager.getDataDir();
        deleteFiles(logDir);

        GtidSet gtidSet1 = new GtidSet("c372080a-1804-11ea-8add-98039bbedf9c:1-2500");
        gtidManager.updateExecutedGtids(gtidSet1);
        int eventSize = 0;

        writeTransactionWithMultiTableMapLogEvent();
        eventSize++;

        fileManager.flush();

        return eventSize;
    }

    public void createFiles() throws Exception {
        File logDir = fileManager.getDataDir();
        deleteFiles(logDir);
        gtidManager.updateExecutedGtids(new GtidSet("c372080a-1804-11ea-8add-98039bbedf9c:1-1500"));
        ByteBuf byteBuf = getGtidEvent();
        fileManager.append(byteBuf);
        fileManager.rollLog();
        byteBuf.release();

        gtidManager.updateExecutedGtids(new GtidSet("c372080a-1804-11ea-8add-98039bbedf9c:1-1550"));
        byteBuf = getGtidEvent();
        fileManager.append(byteBuf);
        fileManager.rollLog();
        byteBuf.release();

        gtidManager.updateExecutedGtids(EXCLUDED_GTID);
        byteBuf = getGtidEvent();
        fileManager.append(byteBuf);
        fileManager.rollLog();
        byteBuf.release();


        gtidManager.updateExecutedGtids(new GtidSet("c372080a-1804-11ea-8add-98039bbedf9c:1-2020"));
        byteBuf = getGtidEvent();
        fileManager.append(byteBuf);
        fileManager.rollLog();
        byteBuf.release();
    }

    public void createMultiUuidsFiles() throws Exception {
        File logDir = fileManager.getDataDir();
        deleteFiles(logDir);
        gtidManager.updateExecutedGtids(new GtidSet("c372080a-1804-11ea-8add-98039bbedf9c:1-1500,c372080a-1804-11ea-8add-98039bbedfad:1-15000"));
        ByteBuf byteBuf = getGtidEvent();
        fileManager.append(byteBuf);
        fileManager.rollLog();
        byteBuf.release();

        gtidManager.updateExecutedGtids(new GtidSet("c372080a-1804-11ea-8add-98039bbedf9c:1-1550,c372080a-1804-11ea-8add-98039bbedfad:1-15500"));
        byteBuf = getGtidEvent();
        fileManager.append(byteBuf);
        fileManager.rollLog();
        byteBuf.release();

        gtidManager.updateExecutedGtids(new GtidSet("c372080a-1804-11ea-8add-98039bbedf9c:1-2020,c372080a-1804-11ea-8add-98039bbedfad:1-16000"));
        byteBuf = getGtidEvent();
        fileManager.append(byteBuf);
        fileManager.rollLog();
        byteBuf.release();

        gtidManager.updateExecutedGtids(new GtidSet("c372080a-1804-11ea-8add-98039bbedf9c:1-4040,c372080a-1804-11ea-8add-98039bbedfad:1-16500"));
        byteBuf = getGtidEvent();
        fileManager.append(byteBuf);
        fileManager.rollLog();
        byteBuf.release();
    }



    public void createMultiUuidsFiles_2() throws Exception {
        File logDir = fileManager.getDataDir();
        deleteFiles(logDir);
        gtidManager.updateExecutedGtids(new GtidSet("c372080a-1804-11ea-8add-98039bbedf9c:1-1500,c372080a-1804-11ea-8add-98039bbedfad:1-15000"));
        ByteBuf byteBuf = getGtidEvent();
        fileManager.append(byteBuf);
        fileManager.rollLog();
        byteBuf.release();

        gtidManager.updateExecutedGtids(new GtidSet("c372080a-1804-11ea-8add-98039bbedf9c:1-1550,c372080a-1804-11ea-8add-98039bbedfad:1-15000"));
        byteBuf = getGtidEvent();
        fileManager.append(byteBuf);
        fileManager.rollLog();
        byteBuf.release();

        gtidManager.updateExecutedGtids(new GtidSet("c372080a-1804-11ea-8add-98039bbedf9c:1-2020,c372080a-1804-11ea-8add-98039bbedfad:1-16000"));
        byteBuf = getGtidEvent();
        fileManager.append(byteBuf);
        fileManager.rollLog();
        byteBuf.release();

        gtidManager.updateExecutedGtids(new GtidSet("c372080a-1804-11ea-8add-98039bbedf9c:1-2020,c372080a-1804-11ea-8add-98039bbedfad:1-16500"));
        byteBuf = getGtidEvent();
        fileManager.append(byteBuf);
        fileManager.rollLog();
        byteBuf.release();

        gtidManager.updateExecutedGtids(new GtidSet("c372080a-1804-11ea-8add-98039bbedf9c:1-2020,c372080a-1804-11ea-8add-98039bbedfad:1-18000"));
        byteBuf = getGtidEvent();
        fileManager.append(byteBuf);
        fileManager.rollLog();
        byteBuf.release();
    }
    
    

    public void createDrcTableMapLogEventFiles() throws Exception {
        File logDir = fileManager.getDataDir();
        deleteFiles(logDir);
        gtidManager.updateExecutedGtids(EXCLUDED_GTID);
        ByteBuf byteBuf = getGtidEvent();
        GtidLogEvent gtidLogEvent = new GtidLogEvent().read(byteBuf);
        String gtid = gtidLogEvent.getGtid();
        gtidManager.addExecutedGtid(gtid);
        fileManager.append(byteBuf);
        TableMapLogEvent tableMapLogEvent = getDrcTableMapLogEvent();
        fileManager.append(tableMapLogEvent.getLogEventHeader().getHeaderBuf());
        fileManager.append(tableMapLogEvent.getPayloadBuf());
        fileManager.append(getXidEvent());
        byteBuf.release();
        gtidLogEvent.release();
        tableMapLogEvent.release();
    }

    public void createDrcDdlLogEventFiles() throws Exception {
        File logDir = fileManager.getDataDir();
        deleteFiles(logDir);
        gtidManager.updateExecutedGtids(EXCLUDED_GTID);
        ByteBuf byteBuf = getGtidEvent();
        GtidLogEvent gtidLogEvent = new GtidLogEvent().read(byteBuf);
        String gtid = gtidLogEvent.getGtid();
        gtidManager.addExecutedGtid(gtid);
        fileManager.append(byteBuf);
        DrcDdlLogEvent drcDdlLogEvent = getDrcDdlLogEvent();
        fileManager.append(drcDdlLogEvent.getLogEventHeader().getHeaderBuf());
        fileManager.append(drcDdlLogEvent.getPayloadBuf());

        TableMapLogEvent tableMapLogEvent = getDrcTableMapLogEvent();
        fileManager.append(tableMapLogEvent.getLogEventHeader().getHeaderBuf());
        fileManager.append(tableMapLogEvent.getPayloadBuf());
        fileManager.append(getXidEvent());

        byteBuf.release();
        gtidLogEvent.release();
        drcDdlLogEvent.release();
        tableMapLogEvent.release();
    }

    public int createDrcIndexLogEventFiles() throws Exception {
        int interval = 1024 * 5;
        System.setProperty(SystemConfig.PREVIOUS_GTID_INTERVAL, String.valueOf(interval));
        fileManager = new DefaultFileManager(schemaManager, APPLIER_NAME);
        fileManager.initialize();
        fileManager.start();
        fileManager.setGtidManager(gtidManager);

        File logDir = fileManager.getDataDir();
        deleteFiles(logDir);

        GtidSet gtidSet1 = new GtidSet("c372080a-1804-11ea-8add-98039bbedf9c:1-2000");
        gtidManager.updateExecutedGtids(gtidSet1);

        int size = writeTransaction();
        int loop = interval / size;
        for (int i = 0; i < loop; ++i) {
            writeTransaction();
        }
        writeTransaction();


        for (int i = 0; i < loop / 2; ++i) {
            writeTransaction();
        }
        GtidSet gtidSet2 = new GtidSet("c372080a-1804-11ea-8add-98039bbedf9c:1-3000");
        gtidManager.updateExecutedGtids(gtidSet2);
        for (int i = 0; i < loop / 2; ++i) {
            writeTransaction();
        }
        writeTransaction();


        for (int i = 0; i < loop / 2; ++i) {
            writeTransaction();
        }
        GtidSet gtidSet3 = new GtidSet("c372080a-1804-11ea-8add-98039bbedf9c:1-4000");
        gtidManager.updateExecutedGtids(gtidSet3);
        for (int i = 0; i < loop / 2; ++i) {
            writeTransaction();
        }
        writeTransaction();

        fileManager.flush();

        return loop / 2 + 1;
    }

    /*
     * previous c372080a-1804-11ea-8add-98039bbedf9c:1-2000
     * ddl (4 events)
     * ddl (loop)
     * ddl (1)
     *
     * ddl (loop / 2)
     * executed_gtid c372080a-1804-11ea-8add-98039bbedf9c:1-3000
     * ddl (loop / 2)
     * ddl (1)
     *
     * ddl (loop / 2)
     * executed_gtid c372080a-1804-11ea-8add-98039bbedf9c:1-4000
     * ddl (loop / 2)
     * ddl (1)
     */
    public int createDrcIndexLogEventFilesWithDdl() throws Exception {
        int interval = 1024 * 5;
        System.setProperty(SystemConfig.PREVIOUS_GTID_INTERVAL, String.valueOf(interval));
        fileManager = new DefaultFileManager(schemaManager, APPLIER_NAME);
        fileManager.initialize();
        fileManager.start();
        fileManager.setGtidManager(gtidManager);

        File logDir = fileManager.getDataDir();
        deleteFiles(logDir);

        GtidSet gtidSet1 = new GtidSet("c372080a-1804-11ea-8add-98039bbedf9c:1-2000");
        gtidManager.updateExecutedGtids(gtidSet1);

        int size = writeDdlTransaction();
        int loop = interval / size;
        for (int i = 0; i < loop; ++i) {
            writeDdlTransaction();
        }
        writeDdlTransaction();


        for (int i = 0; i < loop / 2; ++i) {
            writeDdlTransaction();
        }
        GtidSet gtidSet2 = new GtidSet("c372080a-1804-11ea-8add-98039bbedf9c:1-3000");
        gtidManager.updateExecutedGtids(gtidSet2);
        for (int i = 0; i < loop / 2; ++i) {
            writeDdlTransaction();
        }
        writeDdlTransaction();


        for (int i = 0; i < loop / 2; ++i) {
            writeDdlTransaction();
        }
        GtidSet gtidSet3 = new GtidSet("c372080a-1804-11ea-8add-98039bbedf9c:1-4000");
        gtidManager.updateExecutedGtids(gtidSet3);
        for (int i = 0; i < loop / 2; ++i) {
            writeDdlTransaction();
        }
        writeDdlTransaction();

        fileManager.flush();

        return loop / 2 + 1;
    }

    public int testIndexFileWithDiffGtidSetAndDdl() throws Exception {
        int interval = 1024 * 5;
        System.setProperty(SystemConfig.PREVIOUS_GTID_INTERVAL, String.valueOf(interval));
        fileManager = new DefaultFileManager(schemaManager, APPLIER_NAME);
        fileManager.initialize();
        fileManager.start();
        fileManager.setGtidManager(gtidManager);
        gtidManager.updateExecutedGtids(new GtidSet(""));

        File logDir = fileManager.getDataDir();
        deleteFiles(logDir);
        int size = writeTransactionWithGtid(UUID_STRING + ":" + 1);
        gtidManager.addExecutedGtid(UUID_STRING + ":" + 1);
        int loop = interval / size;
        for (int i = 2; i <= loop; ++i) {
            if (i == loop / 2) {
                writeTransactionWithGtidAndDdl(UUID_STRING + ":" + i);
            } else {
                writeTransactionWithGtid(UUID_STRING + ":" + i);
            }
            gtidManager.addExecutedGtid(UUID_STRING + ":" + i);
        }
        writeTransactionWithGtid(UUID_STRING + ":" + (loop + 1));
        gtidManager.addExecutedGtid(UUID_STRING + ":" + (loop + 1));
        fileManager.flush();

        return loop + 1;
    }

    public int testDrcGtidLogEvent() throws Exception {
        int interval = 1024 * 5;
        System.setProperty(SystemConfig.PREVIOUS_GTID_INTERVAL, String.valueOf(interval));
        fileManager = new DefaultFileManager(schemaManager, APPLIER_NAME);
        fileManager.initialize();
        fileManager.start();
        fileManager.setGtidManager(gtidManager);
        gtidManager.updateExecutedGtids(new GtidSet(""));

        File logDir = fileManager.getDataDir();
        deleteFiles(logDir);
        int size = writeTransactionWithGtidAndTransactionOffset(UUID_STRING + ":" + 1);
        gtidManager.addExecutedGtid(UUID_STRING + ":" + 1);
        int loop = interval / size;
        String drcGtidLogEvent;
        for (int i = 2; i <= loop; ++i) {
            if (i == loop / 2) {
                writeTransactionWithGtidAndTransactionOffset(UUID_STRING + ":" + i);
                drcGtidLogEvent = DRC_UUID_STRING + ":" + i;
                writeDrcGtidLogEvent(drcGtidLogEvent);
                gtidManager.addExecutedGtid(drcGtidLogEvent);
            } else {
                writeTransactionWithGtidAndTransactionOffset(UUID_STRING + ":" + i);
            }
            gtidManager.addExecutedGtid(UUID_STRING + ":" + i);
        }
        writeTransactionWithGtidAndTransactionOffset(UUID_STRING + ":" + (loop + 1));

        gtidManager.setUuids(Sets.newHashSet(UUID_STRING));

        fileManager.flush();

        return loop + 1;
    }

}
