package com.ctrip.framework.drc.core.mq;

import java.util.Map;
import java.util.Set;

/**
 * Created by shiruixin
 * 2025/1/21 20:34
 */
public class BlankKafkaDelayMessageConsumer implements IKafkaDelayMessageConsumer {
    @Override
    public void initConsumer(String subject, String consumerGroup, Set<String> dcs) {

    }

    @Override
    public void mhasRefresh(Map<String, String> mhas2Dc) {

    }

    @Override
    public void addMhas(Map<String, String> mhas2Dc) {

    }

    @Override
    public void removeMhas(Map<String, String> mhas2Dc) {

    }

    @Override
    public boolean stopConsume() {
        return false;
    }

    @Override
    public boolean resumeConsume() {
        return false;
    }

    @Override
    public int getOrder() {
        return 1;
    }
}
