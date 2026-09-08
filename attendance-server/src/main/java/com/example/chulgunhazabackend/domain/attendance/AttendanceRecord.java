package com.example.chulgunhazabackend.domain.attendance;

import com.example.chulgunhazabackend.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

// #100: attendance-server 물리 분리로 employee_id FK/@ManyToOne 대신 employeeNo +
// employeeName을 등록 시점에 그대로 저장(비정규화)한다. 스키마가 user-server와
// 갈라져서 더 이상 JOIN이 불가능하고, 이력 데이터라 "등록 당시 이름"이 남는 게
// 오히려 자연스럽다(사원 개명 시에도 과거 기록은 그때 이름 그대로) —
// docs/aggregate-boundaries.md 참고.
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "AttendanceRecords")
@ToString
@Getter
public class AttendanceRecord extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attendance_record_id")
    private Long id;

    @Column(nullable = false)
    private Long employeeNo;

    @Column(nullable = false)
    private String employeeName;

    @Column(nullable = false)
    private LocalDateTime checkInTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttendanceType attendanceType;

    @Builder
    public AttendanceRecord(Long employeeNo, String employeeName, LocalDateTime checkInTime, AttendanceType attendanceType) {
        this.employeeNo = employeeNo;
        this.employeeName = employeeName;
        this.checkInTime = checkInTime;
        this.attendanceType = attendanceType;
    }
}
