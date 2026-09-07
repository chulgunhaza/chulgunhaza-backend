package com.example.chulgunhazabackend.repository;

import com.example.chulgunhazabackend.domain.annual.AnnualRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AnnualRecordRepository extends JpaRepository<AnnualRecord, Long> {

    // 관리자 백로그 Epic 4 — 연차 결재(반려) 화면용 전체 사용 내역 조회.
    Page<AnnualRecord> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // #79: 본인 연차 사용 이력 조회(self-service). 프론트 캘린더가 신청한 날짜를
    // localStorage 대신 서버 데이터로 표시하는 데 쓴다.
    Page<AnnualRecord> findByEmployeeIdOrderByAnnualDateDesc(Long employeeId, Pageable pageable);
}
