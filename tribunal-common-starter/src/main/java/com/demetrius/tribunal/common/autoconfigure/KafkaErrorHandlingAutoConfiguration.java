package com.demetrius.tribunal.common.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

/**
 * Kafka 错误处理自动配置（M3 异步化：统一重试 + 死信队列）。
 *
 * <p>所有消费 spring-kafka 的服务自动获得：</p>
 * <ul>
 *   <li>指数退避重试：1s → 2s → 4s（约 3 次重试后进入 DLT）</li>
 *   <li>超限投递死信队列：{topic}.DLT（Dead Letter Topic，由对账任务/人工处理）</li>
 * </ul>
 *
 * <p>仅在 classpath 存在 spring-kafka 且容器中有 KafkaTemplate 时装配。</p>
 */
@AutoConfiguration
@ConditionalOnClass(name = "org.springframework.kafka.listener.DefaultErrorHandler")
public class KafkaErrorHandlingAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(KafkaTemplate.class)
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<Object, Object> template) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(template);

        // 指数退避：初始 1s，倍率 2.0，最大间隔 10s
        // maxElapsedTime=7s → 约 3 次重试（1+2+4=7s）后进入死信队列
        ExponentialBackOff backOff = new ExponentialBackOff(1000L, 2.0);
        backOff.setMaxInterval(10000L);
        backOff.setMaxElapsedTime(7000L);

        return new DefaultErrorHandler(recoverer, backOff);
    }
}
