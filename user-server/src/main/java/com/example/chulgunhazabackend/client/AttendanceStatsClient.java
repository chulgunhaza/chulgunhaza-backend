package com.example.chulgunhazabackend.client;

// #100: attendance-server 물리 분리로 DashboardStatsServiceImpl이 더 이상
// AttendanceRecordRepository를 직접 조회할 수 없어서, attendance-server의
// 내부 API(GET /internal/attendance/today-count)를 호출하는 걸로 대체했다.
public interface AttendanceStatsClient {

    // attendance-server가 응답하지 않거나 에러를 내도 예외를 던지지 않고 0을
    // 반환한다 — 대시보드 카드 하나 때문에 통계 API 전체가 죽으면 안 된다.
    long getTodayCheckInCount();
}
