package com.example.chulgunhazabackend.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AttendanceStatsClientImpl implements AttendanceStatsClient {

    private final RestTemplate restTemplate;

    @Value("${attendance-server.internal-url}")
    private String attendanceServerInternalUrl;

    @Override
    public long getTodayCheckInCount() {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Number> response = restTemplate.getForObject(
                    attendanceServerInternalUrl + "/internal/attendance/today-count", Map.class);
            return response == null ? 0L : response.getOrDefault("count", 0L).longValue();
        } catch (RestClientException e) {
            // attendance-server가 아직 안 떠 있거나 응답이 늦어도 대시보드 통계
            // 카드 하나 때문에 /v1/dashboard/stats 전체가 500나면 안 된다 — 0으로
            // 우아하게 degrade하고 경고만 남긴다.
            log.warn("attendance-server 내부 API 호출 실패, 오늘 출근 수는 0으로 대체합니다: {}", e.getMessage());
            return 0L;
        }
    }
}
