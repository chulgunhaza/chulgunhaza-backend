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
        }
    }
}
