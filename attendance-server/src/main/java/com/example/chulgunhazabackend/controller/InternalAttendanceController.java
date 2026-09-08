package com.example.chulgunhazabackend.controller;

import com.example.chulgunhazabackend.repository.AttendanceRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

// #100: 스키마가 분리되면서 user-server의 DashboardStatsServiceImpl이 더 이상
// AttendanceRecordRepository를 직접 조회할 수 없게 됐다 — 이 엔드포인트가 그
// 자리를 대신한다. "/internal" 접두어는 SecurityConfig에서 JWT 없이 열려있고,
// 아직 네트워크 경계가 없는 개발 단계라 별도 서비스 간 인증 없이 호출한다
// (실제 서비스 분리 시 클러스터 내부망으로만 열어두는 것으로 대체 예정).
@RestController
@RequestMapping("/internal/attendance")
@RequiredArgsConstructor
public class InternalAttendanceController {

    private final AttendanceRecordRepository attendanceRecordRepository;

    @GetMapping("/today-count")
    public Map<String, Long> getTodayCheckInCount() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd = todayStart.plusDays(1);
        long count = attendanceRecordRepository.countByCheckInTimeBetween(todayStart, todayEnd);
        return Map.of("count", count);
    }
}
