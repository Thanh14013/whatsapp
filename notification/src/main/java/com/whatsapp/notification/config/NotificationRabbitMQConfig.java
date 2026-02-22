package com.whatsapp.notification.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ Configuration for Notification Service
 *
 * Configures message consumers for notification events.
 *
 * @author WhatsApp Clone Team
 */
@Slf4j
@Configuration
public class NotificationRabbitMQConfig {

    @Value("${app.rabbitmq.queues.message-sent:message.sent}")
    private String messageSentQueue;

    @Value("${app.rabbitmq.queues.user-status-changed:user.status.changed}")
    private String userStatusChangedQueue;

    /**
     * Declare message.sent queue
     */
    @Bean
    public Queue messageSentQueue() {
        log.info("Creating message.sent queue: {}", messageSentQueue);
        return new Queue(messageSentQueue, true);
    }

    /**
     * Declare user.status.changed queue
     */
    @Bean
    public Queue userStatusChangedQueue() {
        log.info("Creating user.status.changed queue: {}", userStatusChangedQueue);
        return new Queue(userStatusChangedQueue, true);
    }

    /**
     * Configure message converter for JSON serialization
     */
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * Configure listener container factory
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter) {

        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        factory.setConcurrentConsumers(3);
        factory.setMaxConcurrentConsumers(5);
        factory.setPrefetchCount(10);

        log.info("RabbitMQ listener container factory configured");

        return factory;
    }
}