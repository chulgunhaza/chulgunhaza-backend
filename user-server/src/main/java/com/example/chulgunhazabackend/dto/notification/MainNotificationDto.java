package com.example.chulgunhazabackend.dto.notification;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * #46 근태(MAIN) SSE 알림. 채팅과 달리 출근 등록/연차 사용 등 여러 도메인 이벤트가
 * 공유하는 채널이라, {@link com.example.chulgunhazabackend.dto.chat.ChatNotificationDto}처럼
 * 도메인 전용 필드를 두지 않고 공통 알림 형태(수신자, 메시지, 발생 시각)로 통일했다.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MainNotificationDto implements Serializable {

    private Long receiverEmployeeNo;

    private String message;

    private LocalDateTime occurredAt;
}
