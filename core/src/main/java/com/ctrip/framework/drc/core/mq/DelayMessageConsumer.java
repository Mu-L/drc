package com.ctrip.framework.drc.core.mq;

import com.ctrip.xpipe.api.lifecycle.Ordered;

import java.util.Map;
import java.util.Set;

public interface DelayMessageConsumer extends Ordered {
    
    void initConsumer(String subject,String consumerGroup,Set<String> dcs);

    boolean stopListen();

    boolean resumeListen();

    void mhasRefresh(Map<String, String> mhas2Dc);

    Map<String, Long> getMhaDelay();

    void refreshMhaDelayFromOtherDc(Map<String, Long> mhaDelayMap);
}
