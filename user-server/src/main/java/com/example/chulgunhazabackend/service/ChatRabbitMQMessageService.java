package com.example.chulgunhazabackend.service;

import com.example.chulgunhazabackend.dto.chat.ChatMessageCreateRMQDto;
import com.example.chulgunhazabackend.dto.chat.ChatMessageCreateRequestDto;
import com.example.chulgunhazabackend.dto.chat.ChatNotificationDto;

public interface ChatRabbitMQMessageService {

    void sendNotification(ChatNotificationDto chatNotificationDto);

    // INFO : 큐에 발행만 한다(더 이상 실시간 전달까지 같이 안 함 — #57). 반환값도 의미
    // 없는 고정 문자열이었던 걸 없앴다.
    void sendChatMessage(ChatMessageCreateRequestDto chatMessageCreateRequestDto, Long senderId);

    // INFO : 방 참여자 전원에게 실시간 전달(WS 접속 중이면 WS, 아니면 SSE 폴백). 예전엔
    // sendChatMessage 안에서 큐 발행과 "동시에" 실행돼서 DB 저장 성공 여부와 무관하게
    // 나갔는데, 이제는 ChatMessageListener가 저장에 실제로 성공한 뒤에만 호출한다(#57).
    void deliverToReceivers(ChatMessageCreateRMQDto chatMessageCreateRMQDto);
}
