package com.example.chulgunhazabackend.domain.annual;

import com.example.chulgunhazabackend.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


import java.time.LocalDate;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name="annual_records")
@Getter
public class AnnualRecord extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "annual_record_id")
    private Long id;

    // 연차를 사용한 사원. 기존 필드(approvedId)만으로는 "누구의 연차 기록인지"를
    // 알 수 없어서 #48 작업 중 추가했다 (논리적 키, LeaveRecord.attendanceRecordId와 동일한 패턴).
    @Column(nullable = false)
    private Long employeeId;

    @Column(nullable = false)
    private Long approvedId;

    @Column(nullable = false)
    private LocalDate annualDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AnnualType annualType;

    private String annualReason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AnnualApprovalStatus annualApprovalStatus;

    @Builder
    public AnnualRecord(Long employeeId, Long approvedId, LocalDate annualDate,
                         AnnualType annualType, String annualReason,
                         AnnualApprovalStatus annualApprovalStatus) {
        this.employeeId = employeeId;
        this.approvedId = approvedId;
        this.annualDate = annualDate;
        this.annualType = annualType;
        this.annualReason = annualReason;
        this.annualApprovalStatus = annualApprovalStatus;
    }

}
