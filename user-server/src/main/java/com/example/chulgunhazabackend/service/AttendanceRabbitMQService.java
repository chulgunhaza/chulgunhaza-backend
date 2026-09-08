package com.example.chulgunhazabackend.service;

import com.example.chulgunhazabackend.dto.attendance.AttendanceCreateRequestDto;

public interface AttendanceRabbitMQService {
    void sendAttendanceDto (AttendanceCreateRequestDto attendanceCreateRequestDto);
}
