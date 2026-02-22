package com.whatsapp.messageprocessor.config;

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
 * RabbitMQ Configuration for Message Processor
 *
 * Configures message consumers for processing message events.
 *
 * Queues:
 * - message.sent: New messages to process
 * - message.delivered: Message delivery confirmations
 * - message.read: Message read receipts
 *
 * @author WhatsApp Clone Team
 */
@Slf4j
@Configuration
public class RabbitMQConfig {

    @Value("${app.rabbitmq.queues.message-sent:message.sent}")
    private String messageSentQueue;

    @Value("${app.rabbitmq.queues.message-delivered:message.delivered}")
    private String messageDeliveredQueue;

    @Value("${app.rabbitmq.queues.message-read:message.read}")
    private String messageReadQueue;

    /**
     * Declare message.sent queue
     */
    @Bean
    public Queue messageSentQueue() {
        log.info("Creating message.sent queue: {}", messageSentQueue);
        return new Queue(messageSentQueue, true);
    }

    /**
     * Declare message.delivered queue
     */
    @Bean
    public Queue messageDeliveredQueue() {
        log.info("Creating message.delivered queue: {}", messageDeliveredQueue);
        return new Queue(messageDeliveredQueue, true);
    }

    /**
     * Declare message.read queue
     */
    @Bean
    public Queue messageReadQueue() {
        log.info("Creating message.read queue: {}", messageReadQueue);
        return new Queue(messageReadQueue, true);
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
     *
     * Settings:
     * - Concurrent consumers: 3-10
     * - Prefetch count: 5 (fetch 5 messages at a time)
     * - Auto-acknowledge: false (manual ack after processing)
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter) {

        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        factory.setConcurrentConsumers(3);
        factory.setMaxConcurrentConsumers(10);
        factory.setPrefetchCount(5);

        log.info("RabbitMQ listener container factory configured");

        return factory;
    }
}
