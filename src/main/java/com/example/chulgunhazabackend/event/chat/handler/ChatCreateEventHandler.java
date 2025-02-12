package com.example.chulgunhazabackend.event.chat.handler;

import com.example.chulgunhazabackend.dto.chat.ChatNotificationDto;
import com.example.chulgunhazabackend.event.chat.event.ChatCreateEvent;
import com.example.chulgunhazabackend.repository.ChatMessageRepository;
import com.example.chulgunhazabackend.service.ChatAlarmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatCreateEventHandler {

    private final ChatAlarmService chatAlarmService;

    @EventListener
    public void handleChatCreateEvent(ChatCreateEvent event) throws IOException {

        ChatNotificationDto dto = new ChatNotificationDto(
                event.getRoomId()
                , event.getSenderId()
                , event.getSenderName()
                , event.getReceiverId()
                , event.getMessage()
                , event.getUnReadMessageCount() + 1
        );

        chatAlarmService.sendSseEvent(dto);
    }
}
