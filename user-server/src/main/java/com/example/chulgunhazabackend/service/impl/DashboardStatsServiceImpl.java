package com.example.chulgunhazabackend.service.impl;

import com.example.chulgunhazabackend.client.AttendanceStatsClient;
import com.example.chulgunhazabackend.dto.dashboard.DashboardStatsResponseDto;
import com.example.chulgunhazabackend.repository.EmployeeRepository;
import com.example.chulgunhazabackend.repository.PostRepository;
import com.example.chulgunhazabackend.service.DashboardStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardStatsServiceImpl implements DashboardStatsService {

    private final EmployeeRepository employeeRepository;
    private final PostRepository postRepository;
    // #100: attendance-server 물리 분리로 AttendanceRecordRepository를 직접 못
    // 쓰게 돼서, 내부 API를 호출하는 클라이언트로 교체했다.
    private final AttendanceStatsClient attendanceStatsClient;

    @Override
    public DashboardStatsResponseDto getStats() {
        long totalEmployees = employeeRepository.countByDelFlagFalse();

        // 부서명 오름차순으로 안정적인 순서를 유지 — 매번 순서가 바뀌면 카드 UI가 깜빡여 보인다.
        Map<String, Long> departmentCounts = employeeRepository.countActiveEmployeesByDepartment().stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> (Long) row[1],
                        (a, b) -> a,
                        LinkedHashMap::new
                ));

        long todayAttendanceCount = attendanceStatsClient.getTodayCheckInCount();

        long totalPosts = postRepository.countByDelFlagFalse();

        return new DashboardStatsResponseDto(totalEmployees, departmentCounts, todayAttendanceCount, totalPosts);
    }
}
