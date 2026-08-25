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

    // INFO : 이 메시지를 "아직" 안 읽은 방 참여자 수 (발신자 본인 제외). 카카오톡 단톡방처럼
    // 각자 읽으면 하나씩 줄어들다가 전원 읽으면 0. 예전엔 메시지당 boolean 하나(isRead)로
    // 관리해서 한 명만 읽어도 전원 읽음 처리되는 버그가 있었다 — 방 참여자별
    // lastReadMessageId(EmployeeChatRoom)를 기준으로 매번 다시 계산한다.
    private long unReadCount;

    public static ChatMessageListResponseDto fromEntity(ChatMessage chatMessage, long unReadCount) {
        return new ChatMessageListResponseDto(
                chatMessage.getEmployee().getId()
                , chatMessage.getMessage()
                , chatMessage.getChatRoom().getId()
                , chatMessage.getCreateTime()
                , unReadCount
        );
    }
}
