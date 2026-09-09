package com.example.chulgunhazabackend.websocket;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

// #101: Redis Pub/Sub(chat:fanout) 채널로 오가는 봉투. payloadJson은 실제
// WebSocket으로 그대로 전달할, 이미 직렬화된 JSON 문자열이다(ChatMessageCreateRMQDto나
// ChatReadEventDto를 굳이 이 채널에서 또 알 필요 없게, 발행 시점에 이미 문자열로
// 만들어서 담는다 — 소비 측(ChatFanoutSubscriber)은 그대로 세션에 실어 보내기만 함).
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ChatFanoutMessage {

    private Long receiverId;

    private Long roomId;

    private String payloadJson;
}
