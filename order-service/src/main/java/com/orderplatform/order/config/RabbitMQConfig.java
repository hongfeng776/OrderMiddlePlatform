package com.orderplatform.order.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitMQConfig {

    public static final String ORDER_STATUS_EXCHANGE = "order.status.exchange";
    public static final String ORDER_STATUS_QUEUE = "order.status.queue";
    public static final String ORDER_STATUS_ROUTING_KEY = "order.status";
    public static final String ORDER_STATUS_DLQ = "order.status.dlq";

    public static final String NOTIFICATION_EXCHANGE = "notification.exchange";
    public static final String NOTIFICATION_QUEUE = "notification.queue";
    public static final String NOTIFICATION_ROUTING_KEY = "notification";
    public static final String NOTIFICATION_DLQ = "notification.dlq";

    @Bean
    public DirectExchange orderStatusExchange() {
        return new DirectExchange(ORDER_STATUS_EXCHANGE, true, false);
    }

    @Bean
    public Queue orderStatusQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", ORDER_STATUS_EXCHANGE);
        args.put("x-dead-letter-routing-key", ORDER_STATUS_DLQ);
        return new Queue(ORDER_STATUS_QUEUE, true, false, false, args);
    }

    @Bean
    public Queue orderStatusDlq() {
        return new Queue(ORDER_STATUS_DLQ, true);
    }

    @Bean
    public Binding orderStatusBinding() {
        return BindingBuilder.bind(orderStatusQueue()).to(orderStatusExchange()).with(ORDER_STATUS_ROUTING_KEY);
    }

    @Bean
    public Binding orderStatusDlqBinding() {
        return BindingBuilder.bind(orderStatusDlq()).to(orderStatusExchange()).with(ORDER_STATUS_DLQ);
    }

    @Bean
    public DirectExchange notificationExchange() {
        return new DirectExchange(NOTIFICATION_EXCHANGE, true, false);
    }

    @Bean
    public Queue notificationQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", NOTIFICATION_EXCHANGE);
        args.put("x-dead-letter-routing-key", NOTIFICATION_DLQ);
        return new Queue(NOTIFICATION_QUEUE, true, false, false, args);
    }

    @Bean
    public Queue notificationDlq() {
        return new Queue(NOTIFICATION_DLQ, true);
    }

    @Bean
    public Binding notificationBinding() {
        return BindingBuilder.bind(notificationQueue()).to(notificationExchange()).with(NOTIFICATION_ROUTING_KEY);
    }

    @Bean
    public Binding notificationDlqBinding() {
        return BindingBuilder.bind(notificationDlq()).to(notificationExchange()).with(NOTIFICATION_DLQ);
    }

    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                System.err.println("消息发送失败: " + cause);
            }
        });
        rabbitTemplate.setReturnsCallback(returned -> {
            System.err.println("消息被退回: " + returned.getMessage());
        });
        rabbitTemplate.setRetryTemplate(retryTemplate());
        return rabbitTemplate;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setConcurrentConsumers(2);
        factory.setMaxConcurrentConsumers(5);
        factory.setPrefetchCount(1);
        return factory;
    }

    @Bean
    public RetryTemplate retryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();
        
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(3);
        retryTemplate.setRetryPolicy(retryPolicy);
        
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(1000L);
        backOffPolicy.setMultiplier(2.0);
        backOffPolicy.setMaxInterval(10000L);
        retryTemplate.setBackOffPolicy(backOffPolicy);
        
        return retryTemplate;
    }
}
