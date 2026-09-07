package com.example.chulgunhazabackend.listener.deadLetterListener;

import com.example.chulgunhazabackend.config.RabbitMQConfig;
import com.example.chulgunhazabackend.dto.chat.ChatMessageCreateRMQDto;
import com.example.chulgunhazabackend.service.ChatMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

// #73: AttendanceDeadLetterListener와 같은 패턴. ChatMessageListener가
// basicNack(tag, false, false)로 거부한 메시지는 이제 chatQueue에 걸어둔
// x-dead-letter-exchange를 통해 자동으로 여기로 들어온다.
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatDeadLetterListener {

    private final ChatMessageService chatMessageService;

    @RabbitListener(queues = RabbitMQConfig.C_DLQ)
    @Retryable(
            retryFor = {Exception.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000),
            recover = "chatDLQConsumeRecover"
    )
    public void chatDlqConsume(@Payload ChatMessageCreateRMQDto chatMessageCreateRMQDto) {
        log.info("채팅 DLQ 재처리 시도: {}", chatMessageCreateRMQDto);
        chatMessageService.saveChatMessage(chatMessageCreateRMQDto);
    }

    @Recover
    public void chatDLQConsumeRecover(Exception e, ChatMessageCreateRMQDto chatMessageCreateRMQDto) {
        log.error("채팅 DLQ 재처리 최종 실패, 메시지 유실: {}, 원인: {}", chatMessageCreateRMQDto, e.getMessage());
        // 어드민 알람 발행 — AttendanceDeadLetterListener와 동일하게 후속 과제로 남겨둠.
    }
}
