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


@Configuration
@EnableRabbit
public class RabbitMQConfig {

    // INFO: Domain Queue - MAIN
    // #100: ATTENDANCE_QUEUE_NAME/큐/바인딩은 attendance-server로 이전됐다
    // (attendance-server/config/RabbitMQConfig.java) — 여긴 더 이상 소비하지 않음.
    public static final String LEAVE_WORK_QUEUE_NAME = "chulgunhazabackend_leave_work_queue";

    // INFO: Notification Queue - CHAT
    // #101: chatQueue/chatBinding/C_DLQ 등 채팅 도메인 큐는 chatting-server로 이전됐다
    // (chatting-server/config/RabbitMQConfig.java) — 여긴 chatting-server가 발행하는
    // 교차 서비스 알림(ChatNotificationDto)만 소비한다(ChatNotificationListener 참고).
    public static final String CHAT_NOTIFICATION_QUEUE_NAME = "chulgunhazabackend_notification_queue";

    // INFO: Notification Queue - MAIN
    public static final String MAIN_NOTIFICATION_QUEUE_NAME = "chulgunhazabackend_record_notification_queue";


    // INFO: Exchange - Chat
    // #101: 큐/바인딩은 chatting-server로 이전됐지만, exchange 자체는 여기서도 계속
    // 선언해야 한다 — chatNotificationBinding이 이 exchange에 바인딩되기 때문
    // (발행자인 chatting-server도 같은 이름으로 자기 쪽에서 선언한다).
    public static final String CHAT_EXCHANGE_NAME = "chulgunhazabackend_chat";

    // INFO: Exchange - Main
    public static final String MAIN_EXCHANGE_NAME = "chulgunhazabackend_main";

    // INFO: Routing Key - Chat
    public static final String CHAT_NOTIFICATION_ROUTING_KEY = "chat_notification_queue_key";

    // INFO: Routing Key - Main
    public static final String LEAVE_WORK_ROUTING_KEY = "leave_work_queue_key";
    // #100: attendance-server가 이 exchange/키로 MainNotificationDto를 발행하고,
    // 아래 mainNotificationQueue/mainNotificationBinding이 그걸 소비한다
    // (MainNotificationListener 참고) — 예전엔 아무도 안 쓰던 빈 배관이었다.
    public static final String MAIN_NOTIFICATION_ROUTING_KEY = "main_notification_queue_key";

    // #101: DLX/C_DLQ(데드레터)는 chatQueue 전용이었는데 그 큐 자체가 chatting-server로
    // 옮겨갔다 — attendance의 데드레터(#100)도 이미 attendance-server로 옮겨져 있어서,
    // 이제 user-server엔 데드레터를 쓰는 큐가 하나도 안 남는다. 그래서 DLX 자체를 제거한다.


    @Bean
    public Queue chatNotificationQueue() {
        return new Queue(CHAT_NOTIFICATION_QUEUE_NAME,true);
    }

    @Bean
    public Queue leaveWorkQueue() {
        return new Queue(LEAVE_WORK_QUEUE_NAME,true);
    }

    @Bean
    public Queue mainNotificationQueue() {
        return new Queue(MAIN_NOTIFICATION_QUEUE_NAME,true);
    }

    @Bean
    public DirectExchange chatExchange() {
        return new DirectExchange(CHAT_EXCHANGE_NAME);
    }

    @Bean
    public DirectExchange mainExchange() {
        return new DirectExchange(MAIN_EXCHANGE_NAME);
    }

    @Bean
    public Binding chatNotificationBinding() {
        return BindingBuilder.bind(chatNotificationQueue()).to(chatExchange()).with(CHAT_NOTIFICATION_ROUTING_KEY);
    }

    @Bean
    public Binding mainLeaveWorkBinding() {
        return BindingBuilder.bind(leaveWorkQueue()).to(mainExchange()).with(LEAVE_WORK_ROUTING_KEY);
    }

    @Bean
    public Binding mainNotificationBinding() {
        return BindingBuilder.bind(mainNotificationQueue()).to(mainExchange()).with(MAIN_NOTIFICATION_ROUTING_KEY);
    }

    // 메시지 송신 빈 등록
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }

    // 메세지 수신 빈 등록
    @Bean
    public SimpleRabbitListenerContainerFactory simpleRabbitListenerContainerFactory(
            ConnectionFactory connectionFactory){
        SimpleRabbitListenerContainerFactory listenerContainerFactory
                = new SimpleRabbitListenerContainerFactory();
        listenerContainerFactory.setConnectionFactory(connectionFactory);
        listenerContainerFactory.setDefaultRequeueRejected(false);
        listenerContainerFactory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        listenerContainerFactory.setMessageConverter(jsonMessageConverter());

        return listenerContainerFactory;
    }


    // JSON 변환기
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

}
