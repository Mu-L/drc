package com.ctrip.framework.drc.messenger.mq;

import com.ctrip.framework.drc.core.mq.Producer;

import java.util.List;

/**
 * Created by jixinwang on 2022/10/17
 */
public interface MqProvider {

    List<Producer> getProducers(String tableName);
}
