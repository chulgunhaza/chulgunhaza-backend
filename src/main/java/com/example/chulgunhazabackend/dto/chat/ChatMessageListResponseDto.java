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

    // INFO : 메시지당 읽음 여부(isRead)는 제거했다 — 그룹 채팅에서는 "누가 읽었는지"가
    // 사람마다 달라서 메시지 하나에 boolean 하나로는 표현이 안 된다. 읽음/안읽음은 이제
    // 방 목록 단위(ChatRoomListResponseDto.unReadMessageCount, 사람별로 정확히 계산)로만
    // 노출한다.

    public ChatMessageListResponseDto fromEntity(ChatMessage chatMessage) {
        return new ChatMessageListResponseDto(
                chatMessage.getEmployee().getId()
                , chatMessage.getMessage()
                , chatMessage.getChatRoom().getId()
                , chatMessage.getCreateTime()
        );
    }
}
