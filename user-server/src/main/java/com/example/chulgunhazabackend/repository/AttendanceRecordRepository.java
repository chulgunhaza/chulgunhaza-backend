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

    Page<AttendanceRecord> findByEmployee_EmployeeNoOrderByCheckInTimeDesc(Long employeeNo, Pageable pageable);

    // 관리자 백로그 Epic 6 — 대시보드 통계 카드용(오늘 출근 인원).
    long countByCheckInTimeBetween(LocalDateTime start, LocalDateTime end);
}
