package com.example.chulgunhazabackend.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * 누군가 방을 읽었을 때 WebSocket으로 방의 다른 참여자들에게 실시간으로 쏘는 이벤트.
 * "메시지별 안읽은 인원 수"가 즉시 줄어들어야 한다는 요구(카카오톡류 read receipt)라서,
 * 새로 전송하는 채팅 메시지 payload({@code message} 필드가 있음)와 구분되도록
 * {@code type: "read"} 마커를 둔다.
 */
@Getter
@AllArgsConstructor
public class ChatReadEventDto {

    private final String type = "read";

    private Long roomId;

    private Long readerId;

    // INFO : 이번에 새로 읽힌 것으로 바뀐 메시지들의 갱신된 안읽은 인원 수.
    private List<MessageUnreadUpdate> updates;

    @Getter
    @AllArgsConstructor
    public static class MessageUnreadUpdate {
        private Long messageId;
        private long unReadCount;
    }
}
