package com.example.chulgunhazabackend.dto.chat;

import com.example.chulgunhazabackend.domain.chat.ChatMessage;
import com.example.chulgunhazabackend.domain.chat.ChatRoom;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ChatMessageCreateRMQDto {

    private Long senderId;

    // #101: 발신자 이름/사번을 컨트롤러 단계(JWT 클레임)에서 채워서 큐에 실어 보낸다 —
    // 오프라인 수신자에게 SSE 알림(ChatNotificationDto)을 보낼 때 필요한데,
    // chatting-server는 Employee 리포지토리가 없어서 예전처럼 그 자리에서 조회할 수
    // 없다(#100의 employeeName 비정규화와 동일한 이유). 둘 다 JWT 클레임에 이미
    // 있는 값이라 내부 API 호출도 필요 없다.
    private String senderName;

    private Long senderEmployeeNo;

    // INFO : receiverId 제거 — 단체 채팅에서는 수신자가 여러 명이라 메시지 저장 단계에서는
    // 의미가 없다. 실시간 브로드캐스트 대상(WS/SSE)은 roomId로 방 참여자 전원을 조회해서 결정한다.
    private String message;

    private Long roomId;

    private LocalDateTime creatTime;

    public ChatMessage toEntity(ChatRoom chatRoom, Long employeeId){
        return ChatMessage.builder()
                .employeeId(employeeId)
                .chatRoom(chatRoom)
                .message(message)
                .createTime(creatTime)
                .build();
    }
}
