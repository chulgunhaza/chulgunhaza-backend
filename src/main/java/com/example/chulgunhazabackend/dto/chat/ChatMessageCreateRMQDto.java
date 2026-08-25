package com.example.chulgunhazabackend.dto.chat;

import com.example.chulgunhazabackend.domain.chat.ChatMessage;
import com.example.chulgunhazabackend.domain.chat.ChatRoom;
import com.example.chulgunhazabackend.domain.member.Employee;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ChatMessageCreateRMQDto {

    private Long senderId;

    // INFO : receiverId 제거 — 단체 채팅에서는 수신자가 여러 명이라 메시지 저장 단계에서는
    // 의미가 없다. 실시간 브로드캐스트 대상(WS/SSE)은 roomId로 방 참여자 전원을 조회해서 결정한다.
    private String message;

    private Long roomId;

    private LocalDateTime creatTime;

    public ChatMessage toEntity(ChatRoom chatRoom, Employee employee){
        return ChatMessage.builder()
                .employee(employee)
                .chatRoom(chatRoom)
                .message(message)
                .createTime(creatTime)
                .build();
    }
}
