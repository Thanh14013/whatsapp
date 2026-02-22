package com.whatsapp.user.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ Configuration
 *
 * Configures RabbitMQ exchanges, queues, and bindings for user events.
 *
 * Event Flow:
 * Publisher (User Service) → Exchange → Queue → Consumer (Other Services)
 *
 * @author WhatsApp Clone Team
 */
@Slf4j
@Configuration
public class RabbitMQConfig {

    @Value("${app.rabbitmq.exchanges.user-events:user.events}")
    private String exchangeName;

    @Value("${app.rabbitmq.queues.user-created:user.created}")
    private String userCreatedQueue;

    @Value("${app.rabbitmq.queues.user-updated:user.updated}")
    private String userUpdatedQueue;

    @Value("${app.rabbitmq.queues.user-deleted:user.deleted}")
    private String userDeletedQueue;

    @Value("${app.rabbitmq.queues.user-status-changed:user.status.changed}")
    private String userStatusChangedQueue;

    @Value("${app.rabbitmq.routing-keys.user-created:user.created}")
    private String userCreatedRoutingKey;

    @Value("${app.rabbitmq.routing-keys.user-updated:user.updated}")
    private String userUpdatedRoutingKey;

    @Value("${app.rabbitmq.routing-keys.user-deleted:user.deleted}")
    private String userDeletedRoutingKey;

    @Value("${app.rabbitmq.routing-keys.user-status-changed:user.status.changed}")
    private String userStatusChangedRoutingKey;

    /**
     * Declare topic exchange for user events
     */
    @Bean
    public TopicExchange userEventsExchange() {
        log.info("Creating user events exchange: {}", exchangeName);
        return new TopicExchange(exchangeName, true, false);
    }

    /**
     * Declare queue for user created events
     */
    @Bean
    public Queue userCreatedQueue() {
        log.info("Creating user created queue: {}", userCreatedQueue);
        return new Queue(userCreatedQueue, true);
    }

    /**
     * Declare queue for user updated events
     */
    @Bean
    public Queue userUpdatedQueue() {
        log.info("Creating user updated queue: {}", userUpdatedQueue);
        return new Queue(userUpdatedQueue, true);
    }

    /**
     * Declare queue for user deleted events
     */
    @Bean
    public Queue userDeletedQueue() {
        log.info("Creating user deleted queue: {}", userDeletedQueue);
        return new Queue(userDeletedQueue, true);
    }

    /**
     * Declare queue for user status changed events
     */
    @Bean
    public Queue userStatusChangedQueue() {
        log.info("Creating user status changed queue: {}", userStatusChangedQueue);
        return new Queue(userStatusChangedQueue, true);
    }

    /**
     * Bind user created queue to exchange
     */
    @Bean
    public Binding userCreatedBinding(Queue userCreatedQueue, TopicExchange userEventsExchange) {
        return BindingBuilder.bind(userCreatedQueue)
                .to(userEventsExchange)
                .with(userCreatedRoutingKey);
    }

    /**
     * Bind user updated queue to exchange
     */
    @Bean
    public Binding userUpdatedBinding(Queue userUpdatedQueue, TopicExchange userEventsExchange) {
        return BindingBuilder.bind(userUpdatedQueue)
                .to(userEventsExchange)
                .with(userUpdatedRoutingKey);
    }

    /**
     * Bind user deleted queue to exchange
     */
    @Bean
    public Binding userDeletedBinding(Queue userDeletedQueue, TopicExchange userEventsExchange) {
        return BindingBuilder.bind(userDeletedQueue)
                .to(userEventsExchange)
                .with(userDeletedRoutingKey);
    }

    /**
     * Bind user status changed queue to exchange
     */
    @Bean
    public Binding userStatusChangedBinding(Queue userStatusChangedQueue, TopicExchange userEventsExchange) {
        return BindingBuilder.bind(userStatusChangedQueue)
                .to(userEventsExchange)
                .with(userStatusChangedRoutingKey);
    }

    /**
     * Configure message converter for JSON serialization
     */
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * Configure RabbitTemplate
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }
}