package com.example.chulgunhazabackend.service;

import com.example.chulgunhazabackend.dto.PageDto;
import com.example.chulgunhazabackend.dto.annual.AnnualHistoryResponseDto;
import com.example.chulgunhazabackend.dto.annual.AnnualRecordListResponseDto;
import com.example.chulgunhazabackend.dto.annual.AnnualUsageRequestDto;
import com.example.chulgunhazabackend.dto.annual.AnnualUsageResponseDto;
import org.springframework.data.domain.Pageable;

public interface AnnualLeaveService {

    /**
     * 사원 본인의 연차를 사용 처리한다. 동시에 여러 요청이 들어와도 잔여 연차가
     * 요청 수보다 더 많이 차감되거나 음수가 되지 않아야 한다 (#48).
     */
    AnnualUsageResponseDto useAnnualLeave(Long employeeId, AnnualUsageRequestDto requestDto);

    // 관리자 백로그 Epic 4 — 전사 연차 사용 내역 조회 (결재 화면).
    PageDto<AnnualRecordListResponseDto> getAllAnnualRecords(Pageable pageable);

    // 관리자가 이미 사용 처리된 연차를 반려 — 잔여 연차를 되돌려준다. 이미 반려된
    // 건을 다시 반려하면 중복 환급되므로 멱등성 가드가 있다.
    void rejectAnnualRecord(Long annualRecordId);

    // #79: 본인 연차 사용 이력 조회 (self-service).
    PageDto<AnnualHistoryResponseDto> getMyAnnualHistory(Long employeeId, Pageable pageable);
}
