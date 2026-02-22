package com.whatsapp.chat.infrastructure.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ Configuration
 *
 * Configures RabbitMQ exchanges, queues, and bindings for message events.
 *
 * @author WhatsApp Clone Team
 */
@Configuration
public class RabbitMQConfig {

    public static final String MESSAGE_EXCHANGE = "message.exchange";
    public static final String MESSAGE_SENT_QUEUE = "message.sent.queue";
    public static final String MESSAGE_DELIVERED_QUEUE = "message.delivered.queue";
    public static final String MESSAGE_READ_QUEUE = "message.read.queue";

    public static final String MESSAGE_SENT_ROUTING_KEY = "message.sent";
    public static final String MESSAGE_DELIVERED_ROUTING_KEY = "message.delivered";
    public static final String MESSAGE_READ_ROUTING_KEY = "message.read";

    @Bean
    public TopicExchange messageExchange() {
        return new TopicExchange(MESSAGE_EXCHANGE);
    }

    @Bean
    public Queue messageSentQueue() {
        return QueueBuilder.durable(MESSAGE_SENT_QUEUE)
                .withArgument("x-message-ttl", 86400000) // 24 hours
                .build();
    }

    @Bean
    public Queue messageDeliveredQueue() {
        return QueueBuilder.durable(MESSAGE_DELIVERED_QUEUE)
                .withArgument("x-message-ttl", 86400000)
                .build();
    }

    @Bean
    public Queue messageReadQueue() {
        return QueueBuilder.durable(MESSAGE_READ_QUEUE)
                .withArgument("x-message-ttl", 86400000)
                .build();
    }

    @Bean
    public Binding messageSentBinding(Queue messageSentQueue, TopicExchange messageExchange) {
        return BindingBuilder.bind(messageSentQueue)
                .to(messageExchange)
                .with(MESSAGE_SENT_ROUTING_KEY);
    }

    @Bean
    public Binding messageDeliveredBinding(Queue messageDeliveredQueue, TopicExchange messageExchange) {
        return BindingBuilder.bind(messageDeliveredQueue)
                .to(messageExchange)
                .with(MESSAGE_DELIVERED_ROUTING_KEY);
    }

    @Bean
    public Binding messageReadBinding(Queue messageReadQueue, TopicExchange messageExchange) {
        return BindingBuilder.bind(messageReadQueue)
                .to(messageExchange)
                .with(MESSAGE_READ_ROUTING_KEY);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }
}
