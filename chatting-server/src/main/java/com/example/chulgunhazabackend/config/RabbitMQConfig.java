package com.example.chulgunhazabackend.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// #101: user-server의 RabbitMQConfig에서 채팅 관련 부분만 그대로 옮겼다.
// chatExchange는 여기서도 선언해야 한다 — chatting-server가 이 exchange로
// ChatNotificationDto를 발행(user-server의 신규 ChatNotificationListener가
// 소비)하기 때문. CHAT_NOTIFICATION_QUEUE_NAME 큐/바인딩까지는 필요 없다 —
// 발행자는 exchange+routing key만 알면 되고, 큐/바인딩 선언은 소비자(user-server)의 몫이다.
@Configuration
@EnableRabbit
public class RabbitMQConfig {

    public static final String CHAT_QUEUE_NAME = "chulgunhazabackend_chat_queue";
    public static final String CHAT_EXCHANGE_NAME = "chulgunhazabackend_chat";
    public static final String CHAT_ROUTING_KEY = "chat_queue_key";
    public static final String CHAT_NOTIFICATION_ROUTING_KEY = "chat_notification_queue_key";

    public static final String DLX = "deadLetterExchange";
    public static final String C_DLQ = "chatDeadLetterQueue";

    @Bean
    public Queue chatQueue() {
        return QueueBuilder.durable(CHAT_QUEUE_NAME)
                .withArgument("x-dead-letter-exchange", DLX)
                .withArgument("x-dead-letter-routing-key", C_DLQ)
                .build();
    }

    @Bean
    public Queue chatDeadLetterQueue() {
        return QueueBuilder.durable(C_DLQ).build();
    }

    @Bean
    public DirectExchange chatExchange() {
        return new DirectExchange(CHAT_EXCHANGE_NAME);
    }

    @Bean
    public TopicExchange deadLetterExchange() {
        return new TopicExchange(DLX);
    }

    @Bean
    public Binding chatBinding() {
        return BindingBuilder.bind(chatQueue()).to(chatExchange()).with(CHAT_ROUTING_KEY);
    }

    @Bean
    public Binding chatDeadLetterBinding() {
        return BindingBuilder.bind(chatDeadLetterQueue()).to(deadLetterExchange()).with(C_DLQ);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory simpleRabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory listenerContainerFactory
                = new SimpleRabbitListenerContainerFactory();
        listenerContainerFactory.setConnectionFactory(connectionFactory);
        listenerContainerFactory.setDefaultRequeueRejected(false);
        listenerContainerFactory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        listenerContainerFactory.setMessageConverter(jsonMessageConverter());
        return listenerContainerFactory;
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
