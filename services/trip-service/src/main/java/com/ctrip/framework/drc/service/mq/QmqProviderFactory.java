package com.ctrip.framework.drc.service.mq;

import com.ctrip.xpipe.utils.MapUtils;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qunar.tc.qmq.producer.MessageProducerProvider;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by dengquanliang
 * 2024/5/27 16:33
 */
public class QmqProviderFactory {

    private static final Logger loggerMsg = LoggerFactory.getLogger("MESSENGER");

    private static Map<String, MessageProducerProvider> topicToProvide = Maps.newConcurrentMap();
    private static Map<String, AtomicInteger> refCountMap = Maps.newConcurrentMap();

    public static MessageProducerProvider createProvider(String topic) {
        MessageProducerProvider provider = MapUtils.getOrCreate(topicToProvide, topic, () -> {
            MessageProducerProvider value = new MessageProducerProvider();
            value.init();
            refCountMap.put(topic, new AtomicInteger(0));
            return value;
        });

        int refCount = refCountMap.get(topic).incrementAndGet();
        loggerMsg.info("[MQ] topic {}, refCount: {}", topic, refCount);
        return provider;
    }

    public static void destroy(String topic) {
        if (refCountMap.get(topic).decrementAndGet() == 0) {
            MessageProducerProvider provider = topicToProvide.remove(topic);
            refCountMap.remove(topic);
            provider.destroy();
            loggerMsg.info("[MQ] destroy provider for topic: {}", topic);
        }
    }
}
