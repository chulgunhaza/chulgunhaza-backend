package com.example.chulgunhazabackend.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

// 관리자 백로그 Epic 6 — 대시보드 통계 카드. 사원/근태/게시판 세 애그리게이트를
// 가로질러 집계하는 읽기 전용 뷰라 특정 도메인 서비스에 안 붙이고 별도로 뺐다.
@Getter
@AllArgsConstructor
public class DashboardStatsResponseDto {

    private long totalEmployees;
    private Map<String, Long> departmentCounts;
    private long todayAttendanceCount;
    private long totalPosts;
}
