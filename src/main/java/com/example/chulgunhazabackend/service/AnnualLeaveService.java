package com.example.chulgunhazabackend.service;

import com.example.chulgunhazabackend.dto.annual.AnnualUsageRequestDto;
import com.example.chulgunhazabackend.dto.annual.AnnualUsageResponseDto;

public interface AnnualLeaveService {

    /**
     * 사원 본인의 연차를 사용 처리한다. 동시에 여러 요청이 들어와도 잔여 연차가
     * 요청 수보다 더 많이 차감되거나 음수가 되지 않아야 한다 (#48).
     */
    AnnualUsageResponseDto useAnnualLeave(Long employeeId, AnnualUsageRequestDto requestDto);
}
