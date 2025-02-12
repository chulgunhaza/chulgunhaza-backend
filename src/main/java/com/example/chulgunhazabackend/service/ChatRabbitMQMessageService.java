package com.example.chulgunhazabackend.service;

import com.example.chulgunhazabackend.dto.chat.ChatMessageCreateRequestDto;
import com.example.chulgunhazabackend.dto.chat.ChatNotificationDto;

public interface ChatRabbitMQMessageService {

    void sendNotification(ChatNotificationDto chatNotificationDto);

    String sendChatMessage(ChatMessageCreateRequestDto chatMessageCreateRequestDto, Long senderId);
}
