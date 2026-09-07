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
}
