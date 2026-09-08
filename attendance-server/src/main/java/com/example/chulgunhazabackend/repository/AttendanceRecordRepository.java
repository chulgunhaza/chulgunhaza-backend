package com.example.chulgunhazabackend.repository;

import com.example.chulgunhazabackend.domain.attendance.AttendanceRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {

    // 관리자 페이지 근태 관리 화면 — 등록(register)만 있고 조회 API가 아예 없던 걸
    // 메워준다(#83 백로그의 Epic 3). 최신 출근 기록이 위로 오게 정렬.
    Page<AttendanceRecord> findAllByOrderByCheckInTimeDesc(Pageable pageable);

    // #100: employeeNo가 이제 employee_id를 통한 JOIN 경로(Employee_EmployeeNo)가
    // 아니라 이 엔티티 자신의 컬럼이라 중첩 경로 없이 바로 접근한다.
    Page<AttendanceRecord> findByEmployeeNoOrderByCheckInTimeDesc(Long employeeNo, Pageable pageable);

    // 관리자 백로그 Epic 6 — 대시보드 통계 카드용(오늘 출근 인원).
    // #100: user-server의 DashboardStatsServiceImpl이 더 이상 이 리포지토리를
    // 직접 못 써서, InternalAttendanceController(GET /internal/attendance/today-count)가
    // 이 메서드로 계산한 값을 내부 API로 내려준다.
    long countByCheckInTimeBetween(LocalDateTime start, LocalDateTime end);
}
