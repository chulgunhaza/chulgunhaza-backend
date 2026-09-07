package com.example.chulgunhazabackend.controller;

import com.example.chulgunhazabackend.dto.dashboard.DashboardStatsResponseDto;
import com.example.chulgunhazabackend.service.DashboardStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 관리자 백로그 Epic 6 — 대시보드 통계. 사원/근태/게시판을 가로지르는 집계라
// 특정 도메인 컨트롤러에 안 붙이고 별도로 뺐다.
@RestController
@RequestMapping("/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardStatsService dashboardStatsService;

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ROLE_MANAGER')")
    public ResponseEntity<DashboardStatsResponseDto> getStats() {
        return ResponseEntity.ok(dashboardStatsService.getStats());
    }
}
