package com.example.chulgunhazabackend.service;

import com.example.chulgunhazabackend.dto.attendance.AttendanceCreateRequestDto;

public interface AttendanceRabbitMQService {
    void sendAttendanceDto (AttendanceCreateRequestDto attendanceCreateRequestDto);

    // #100: 저장 성공 후 user-server에 "출근 등록 완료" SSE 알림을 대신 띄워달라고
    // 요청하는 교차 서비스 알림. attendance-server는 SseEmitterManager를 직접
    // 호출할 수 없어서(그 인프라는 user-server에만 있고 연차 등 다른 도메인과도
    // 공유) RabbitMQ로 대신 전달한다.
    void notifyRegistered(AttendanceCreateRequestDto attendanceCreateRequestDto);
}
