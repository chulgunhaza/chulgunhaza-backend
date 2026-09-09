package com.example.chulgunhazabackend.listener;

import com.example.chulgunhazabackend.config.RabbitMQConfig;
import com.example.chulgunhazabackend.dto.chat.ChatMessageCreateRMQDto;
import com.example.chulgunhazabackend.exception.chatException.ChatException;
import com.example.chulgunhazabackend.service.ChatMessageService;
import com.example.chulgunhazabackend.service.ChatRabbitMQMessageService;
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
    private final ChatRabbitMQMessageService chatRabbitMQMessageService;

    @RabbitListener(queues = RabbitMQConfig.CHAT_QUEUE_NAME)
    public void saveChatMessage(@Payload ChatMessageCreateRMQDto chatMessageCreateRMQDto,
                                Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        try{
            chatMessageService.saveChatMessage(chatMessageCreateRMQDto);
            log.info("save Message : " + chatMessageCreateRMQDto.getMessage() + " : " + chatMessageCreateRMQDto.getRoomId());

            // INFO : 저장이 실제로 성공한 뒤에만 실시간 전달한다(#57). 예전엔 저장(비동기,
            // 여기)과 실시간 전달(동기, 컨트롤러 쪽)이 따로 놀아서, 저장이 나중에 실패해도
            // 상대는 이미 메시지를 받아본 상태가 될 수 있었다.
            chatRabbitMQMessageService.deliverToReceivers(chatMessageCreateRMQDto);

        }catch(NullPointerException e){
            channel.basicNack(tag, false, false); // 큐에 있는 메세지 삭제
        }catch(ChatException e){
            // #101: EmployeeException(user-server 전용)은 chatting-server에 더 이상
            // 존재하지 않는다 — "존재하지 않는 사원" 판정도 이제 ChatException.EMPLOYEE_NOT_FOUND로
            // 통일했으므로 이 catch 하나로 충분하다.
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
            // chatQueue에 x-dead-letter-exchange를 걸어둬서(#73, RabbitMQConfig 참고)
            // 아래 basicNack(requeue=false)이 큐에서 그냥 드롭하는 게 아니라
            // chatDeadLetterQueue로 자동 라우팅된다 — ChatDeadLetterListener가
            // @Retryable(3회)로 재처리를 시도하고, 그래도 실패하면 @Recover에서
            // 에러 로그로 가시성을 남긴다.
            log.error("unexpected error while saving chat message via RabbitMQ, routing to DLQ: {}", e.getMessage(), e);
            channel.basicNack(tag, false, false);
        }
    }
}
