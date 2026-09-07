package com.example.chulgunhazabackend.dto.annual;

import com.example.chulgunhazabackend.domain.annual.AnnualApprovalStatus;
import com.example.chulgunhazabackend.domain.annual.AnnualRecord;
import com.example.chulgunhazabackend.domain.annual.AnnualType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

// #79: 본인 연차 사용 이력(self-service) 응답. 관리자용 AnnualRecordListResponseDto와
// 달리 항상 "나"의 기록만 내려주므로 employeeId/employeeName은 안 담는다.
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class AnnualHistoryResponseDto {

    private Long annualRecordId;
    private LocalDate annualDate;
    private AnnualType annualType;
    private String annualReason;
    private AnnualApprovalStatus annualApprovalStatus;

    public static AnnualHistoryResponseDto fromEntity(AnnualRecord record) {
        return new AnnualHistoryResponseDto(
                record.getId(),
                record.getAnnualDate(),
                record.getAnnualType(),
                record.getAnnualReason(),
                record.getAnnualApprovalStatus()
        );
    }
}
