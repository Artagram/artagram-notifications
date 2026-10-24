package com.artagram.notifications.events;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Esta conexion vive en el vhost dedicado "notifications" (ver spring.rabbitmq.virtual-host
 * en application.yml), no en el "/" que usan el resto de servicios. El exchange
 * "artagram.events" declarado aqui es por lo tanto una copia LOCAL a ese vhost — no es el
 * mismo objeto que el "artagram.events" de "/". El puente entre ambos (para que los eventos
 * publicados por auth/users/feed/chat lleguen hasta aqui) es un RabbitMQ Shovel configurado
 * en artagram-infra (ver artagram-infra/rabbitmq/definitions.json), no código de este servicio.
 */
@Configuration
public class NotificationsEventsRabbitMqConfig {

    public static final String EXCHANGE = "artagram.events";
    public static final String DLX_EXCHANGE = "artagram.dlx";

    public static final String QUEUE_FOLLOW_CREATED = "notifications.follow.created";
    public static final String QUEUE_LIKE_CREATED = "notifications.like.created";
    public static final String QUEUE_COMMENT_CREATED = "notifications.comment.created";
    public static final String QUEUE_SUBSCRIPTION_UPDATED = "notifications.subscription.updated";
    public static final String QUEUE_AI_COMPLETED = "notifications.ai.completed";
    public static final String QUEUE_VERIFICATION_REQUESTED = "notifications.verification.requested";

    @Bean
    public TopicExchange eventsExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public Queue followCreatedQueue() {
        return QueueBuilder.durable(QUEUE_FOLLOW_CREATED)
                .deadLetterExchange(DLX_EXCHANGE)
                .deadLetterRoutingKey(QUEUE_FOLLOW_CREATED + ".dlq")
                .build();
    }

    @Bean
    public Binding followCreatedBinding(Queue followCreatedQueue, TopicExchange eventsExchange) {
        return BindingBuilder.bind(followCreatedQueue).to(eventsExchange).with("follow.created");
    }

    @Bean
    public Queue likeCreatedQueue() {
        return QueueBuilder.durable(QUEUE_LIKE_CREATED)
                .deadLetterExchange(DLX_EXCHANGE)
                .deadLetterRoutingKey(QUEUE_LIKE_CREATED + ".dlq")
                .build();
    }

    @Bean
    public Binding likeCreatedBinding(Queue likeCreatedQueue, TopicExchange eventsExchange) {
        return BindingBuilder.bind(likeCreatedQueue).to(eventsExchange).with("like.created");
    }

    @Bean
    public Queue commentCreatedQueue() {
        return QueueBuilder.durable(QUEUE_COMMENT_CREATED)
                .deadLetterExchange(DLX_EXCHANGE)
                .deadLetterRoutingKey(QUEUE_COMMENT_CREATED + ".dlq")
                .build();
    }

    @Bean
    public Binding commentCreatedBinding(Queue commentCreatedQueue, TopicExchange eventsExchange) {
        return BindingBuilder.bind(commentCreatedQueue).to(eventsExchange).with("comment.created");
    }

    @Bean
    public Queue subscriptionUpdatedQueue() {
        return QueueBuilder.durable(QUEUE_SUBSCRIPTION_UPDATED)
                .deadLetterExchange(DLX_EXCHANGE)
                .deadLetterRoutingKey(QUEUE_SUBSCRIPTION_UPDATED + ".dlq")
                .build();
    }

    @Bean
    public Binding subscriptionUpdatedBinding(Queue subscriptionUpdatedQueue, TopicExchange eventsExchange) {
        return BindingBuilder.bind(subscriptionUpdatedQueue).to(eventsExchange).with("subscription.updated");
    }

    @Bean
    public Queue aiCompletedQueue() {
        return QueueBuilder.durable(QUEUE_AI_COMPLETED)
                .deadLetterExchange(DLX_EXCHANGE)
                .deadLetterRoutingKey(QUEUE_AI_COMPLETED + ".dlq")
                .build();
    }

    @Bean
    public Binding aiCompletedBinding(Queue aiCompletedQueue, TopicExchange eventsExchange) {
        return BindingBuilder.bind(aiCompletedQueue).to(eventsExchange).with("ai.completed");
    }

    @Bean
    public Queue verificationRequestedQueue() {
        return QueueBuilder.durable(QUEUE_VERIFICATION_REQUESTED)
                .deadLetterExchange(DLX_EXCHANGE)
                .deadLetterRoutingKey(QUEUE_VERIFICATION_REQUESTED + ".dlq")
                .build();
    }

    @Bean
    public Binding verificationRequestedBinding(Queue verificationRequestedQueue, TopicExchange eventsExchange) {
        return BindingBuilder.bind(verificationRequestedQueue).to(eventsExchange).with("user.verification.requested");
    }
}
