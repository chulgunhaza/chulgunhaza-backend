package com.example.chulgunhazabackend.dto.chat;

import com.example.chulgunhazabackend.domain.chat.ChatMessage;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@AllArgsConstructor
@Getter
@NoArgsConstructor
public class ChatMessageListResponseDto {

    private Long senderId;

    private String message;

    private Long RoomId;

    private LocalDateTime createdTime;

    private boolean isRead;


    public ChatMessageListResponseDto fromEntity(ChatMessage chatMessage) {
        return new ChatMessageListResponseDto(
                chatMessage.getEmployee().getId()
                , chatMessage.getMessage()
                , chatMessage.getChatRoom().getId()
                , chatMessage.getCreateTime()
                , chatMessage.isRead()
        );
    }
}
