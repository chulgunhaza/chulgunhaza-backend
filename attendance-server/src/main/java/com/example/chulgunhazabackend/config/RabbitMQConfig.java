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

// #100: user-server의 RabbitMQConfig에서 근태 관련 부분만 그대로 옮겼다.
// mainExchange는 여기서도 선언해야 한다 — attendance-server가 이 exchange로
// MainNotificationDto를 발행(MainAlarmService를 대신하는 user-server 쪽 신규
// MainNotificationListener가 소비)하기 때문. 큐/바인딩까지는 필요 없다 —
// 발행자는 exchange+routing key만 알면 되고, 큐/바인딩 선언은 소비자(user-server)의 몫이다.
@Configuration
@EnableRabbit
public class RabbitMQConfig {

    public static final String ATTENDANCE_QUEUE_NAME = "chulgunhazabackend_attendance_queue";
    public static final String MAIN_EXCHANGE_NAME = "chulgunhazabackend_main";
    public static final String ATTENDANCE_ROUTING_KEY = "attendance_queue_key";
    public static final String MAIN_NOTIFICATION_ROUTING_KEY = "main_notification_queue_key";

    public static final String DLX = "deadLetterExchange";
    public static final String A_DLQ = "attendanceDeadLetterQueue";

    @Bean
    public Queue attendanceQueue() {
        return QueueBuilder.durable(ATTENDANCE_QUEUE_NAME)
                .withArgument("x-dead-letter-exchange", DLX)
                .withArgument("x-dead-letter-routing-key", A_DLQ)
                .build();
    }

    @Bean
    public Queue attendanceDeadLetterQueue() {
        return QueueBuilder.durable(A_DLQ).build();
    }

    @Bean
    public DirectExchange mainExchange() {
        return new DirectExchange(MAIN_EXCHANGE_NAME);
    }

    @Bean
    public TopicExchange deadLetterExchange() {
        return new TopicExchange(DLX);
    }

    @Bean
    public Binding mainAttendanceBinding() {
        return BindingBuilder.bind(attendanceQueue()).to(mainExchange()).with(ATTENDANCE_ROUTING_KEY);
    }

    @Bean
    public Binding attendanceDeadLetterBinding() {
        return BindingBuilder.bind(attendanceDeadLetterQueue()).to(deadLetterExchange()).with(A_DLQ);
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
