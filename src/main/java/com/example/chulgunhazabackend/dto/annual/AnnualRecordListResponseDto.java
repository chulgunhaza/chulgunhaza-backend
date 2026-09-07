package com.example.chulgunhazabackend.dto.annual;

import com.example.chulgunhazabackend.domain.annual.AnnualApprovalStatus;
import com.example.chulgunhazabackend.domain.annual.AnnualRecord;
import com.example.chulgunhazabackend.domain.annual.AnnualType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

// 관리자 백로그 Epic 4 — 연차 결재(반려) 화면용. AnnualRecord는 채팅/근태 기록과 같은
// 이유로 employeeId만 갖고 있어서(애그리게이트 경계 — Employee를 FK로 참조하지 않음),
// 이름은 서비스 계층에서 별도로 배치 조회해 채워 넣는다.
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class AnnualRecordListResponseDto {

    private Long annualRecordId;
    private Long employeeId;
    private String employeeName;
    private LocalDate annualDate;
    private AnnualType annualType;
    private String annualReason;
    private AnnualApprovalStatus annualApprovalStatus;

    public static AnnualRecordListResponseDto fromEntity(AnnualRecord record, String employeeName) {
        return new AnnualRecordListResponseDto(
                record.getId(),
                record.getEmployeeId(),
                employeeName,
                record.getAnnualDate(),
                record.getAnnualType(),
                record.getAnnualReason(),
                record.getAnnualApprovalStatus()
        );
    }
}
