package com.example.chulgunhazabackend.listener;

import com.example.chulgunhazabackend.dto.chat.ChatMessageCreateRMQDto;
import com.example.chulgunhazabackend.exception.chatException.ChatException;
import com.example.chulgunhazabackend.exception.employeeException.EmployeeException;
import com.example.chulgunhazabackend.service.ChatMessageService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatMessageListener {

    private final ChatMessageService chatMessageService;

    @RabbitListener(queues = "chulgunhazabackend_chat_queue")
    public void saveChatMessage(@Payload ChatMessageCreateRMQDto chatMessageCreateRMQDto,
                                Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        try{
            chatMessageService.saveChatMessage(chatMessageCreateRMQDto);
            log.info("save Message : " + chatMessageCreateRMQDto.getMessage() + " : " + chatMessageCreateRMQDto.getRoomId());

        }catch(NullPointerException e){
            channel.basicNack(tag, false, false); // 큐에 있는 메세지 삭제
        }catch(ChatException | EmployeeException e){
            channel.basicNack(tag, false, false);
        }catch(Exception e){
            // INFO : 위 3개로 분류 안 되는 "예상 못 한" 예외(예: employee_chatroom에 중복 행이
            // 있어서 findByEmployeeIdAndChatRoomId가 IncorrectResultSizeDataAccessException을
            // 던지는 경우)를 여기서 못 잡으면, 컨테이너까지 예외가 그대로 전파돼서 메시지가
            // ack도 nack(no-requeue)도 안 된 채로 계속 재전달(requeue)된다. 그 메시지가 매번
            // 똑같은 이유로 또 실패하는 "poison message"면 초당 수백 번씩 무한 재시도하는
            // 루프에 빠진다 — 실측으로 로그 파일이 몇 초 만에 7900만 줄까지 불어나는 것으로
            // 확인(#72에서 원인이 된 데이터 버그 자체는 고쳤지만, "예상 못 한 예외가 나면
            // 무한 재시도"라는 이 리스너의 구조적 위험은 남아 있어서 별도로 방어한다).
            // 큐 자체에 데드레터가 없어서(다른 큐들과 달리) 일단 드롭 + 에러 로그로 가시성은
            // 남긴다 — 데드레터 큐 추가는 후속 이슈로 분리.
            log.error("unexpected error while saving chat message via RabbitMQ, dropping message: {}", e.getMessage(), e);
            channel.basicNack(tag, false, false);
        }
    }
}
