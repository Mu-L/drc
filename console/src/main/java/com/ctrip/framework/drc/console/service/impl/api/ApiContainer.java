package com.ctrip.framework.drc.console.service.impl.api;

import com.ctrip.framework.drc.core.mq.DelayMessageConsumer;
import com.ctrip.framework.drc.core.mq.IKafkaDelayMessageConsumer;
import com.ctrip.framework.drc.core.service.beacon.BeaconApiService;
import com.ctrip.framework.drc.core.service.ckafka.KafkaApiService;
import com.ctrip.framework.drc.core.service.dal.DbClusterApiService;
import com.ctrip.framework.drc.core.service.email.EmailService;
import com.ctrip.framework.drc.core.service.mysql.MySQLToolsApiService;
import com.ctrip.framework.drc.core.service.ops.ApprovalApiService;
import com.ctrip.framework.drc.core.service.ops.OPSApiService;
import com.ctrip.framework.drc.core.service.security.HeraldService;
import com.ctrip.framework.drc.core.service.statistics.traffic.TrafficStatisticsService;
import com.ctrip.framework.drc.core.service.user.UserService;

/**
 * @ClassName ServiceObtain
 * @Author haodongPan
 * @Date 2021/11/24 15:49
 * @Version: $
 */
public class ApiContainer extends com.ctrip.xpipe.utils.ServicesUtil {
    
    private static class DbClusterApiServiceHolder {
        public static final DbClusterApiService INSTANCE = load(DbClusterApiService.class);
    }
    public static DbClusterApiService getDbClusterApiServiceImpl() {
        return DbClusterApiServiceHolder.INSTANCE;
    }

    private static class KafkaApiServiceHolder {
        public static final KafkaApiService INSTANCE = load(KafkaApiService.class);
    }

    public static KafkaApiService getKafkaApiServiceImpl() {
        return KafkaApiServiceHolder.INSTANCE;
    }
    
    
    private static class BeaconApiServiceHolder {
        public static final BeaconApiService INSTANCE = load(BeaconApiService.class);
    }
    public static BeaconApiService getBeaconApiServiceImpl() {
        return BeaconApiServiceHolder.INSTANCE;
    }
    
    
    private static class MySQLToolsApiServiceHolder {
        public static final MySQLToolsApiService INSTANCE = load(MySQLToolsApiService.class);
    }
    public static MySQLToolsApiService getMySQLToolsApiServiceImpl() {
        return MySQLToolsApiServiceHolder.INSTANCE;
    }


    private static class OPSApiServiceHolder {
        public static final OPSApiService INSTANCE = load(OPSApiService.class);
    }
    public static OPSApiService getOPSApiServiceImpl() {
        return OPSApiServiceHolder.INSTANCE;
    }
    
    private static class UserServiceHolder {
        public static final UserService INSTANCE = load(UserService.class);
    }
    public static  UserService getUserServiceImpl(){
        return UserServiceHolder.INSTANCE;
    }

    private static class TrafficStatisticsServiceHolder {
        public static final TrafficStatisticsService INSTANCE = load(TrafficStatisticsService.class);
    }
    public static  TrafficStatisticsService getTrafficStatisticsService(){
        return TrafficStatisticsServiceHolder.INSTANCE;
    }

    private static class DelayMessageConsumerHolder {
        public static final DelayMessageConsumer INSTANCE = load(DelayMessageConsumer.class);
    }
    public static  DelayMessageConsumer getDelayMessageConsumer(){
        return DelayMessageConsumerHolder.INSTANCE;
    }

    private static class KafkaDelayMessageConsumerHolder {
        public static final IKafkaDelayMessageConsumer INSTANCE = load(IKafkaDelayMessageConsumer.class);
    }

    public static IKafkaDelayMessageConsumer getKafkaDelayMessageConsumer () {
        return KafkaDelayMessageConsumerHolder.INSTANCE;
    }

    private static class ApprovalApiServiceHolder {
        public static final ApprovalApiService INSTANCE = load(ApprovalApiService.class);
    }
    public static ApprovalApiService getApprovalApiServiceImpl() {
        return ApprovalApiServiceHolder.INSTANCE;
    }
    
    // EmailServiceHolder
    private static class EmailServiceHolder {
        public static final EmailService INSTANCE = load(EmailService.class);
    }
    public static EmailService getEmailServiceImpl() {
        return EmailServiceHolder.INSTANCE;
    }
    
    private static class HeraldServiceHolder {
        public static final HeraldService INSTANCE = load(HeraldService.class);
    }
    
    public static HeraldService getHeraldServiceImpl() {
        return HeraldServiceHolder.INSTANCE;
    }
    
}
