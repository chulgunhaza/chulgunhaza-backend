package com.example.chulgunhazabackend.domain.attendance;

import com.example.chulgunhazabackend.domain.common.BaseEntity;
import com.example.chulgunhazabackend.domain.member.Employee;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name="attendance_record_modify_logs")
@Getter
public class AttendanceRecordModifyLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attendance_record_log_id")
    private Long id;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attendance_record_id")
    private AttendanceRecord attendanceRecord;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "at_request_employee_id")
    private Employee requestEmployee; // 수정 요청 사원

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "app_employee_id")
    private Employee appEmployee; // 승인 근태 관리자

    @Column(nullable = false)
    private LocalDateTime oldValue;

    @Column(nullable = false)
    private LocalDateTime modifyValue;

    @Column(nullable = false)
    private String modificationReason;

    @Column(nullable = false)
    private LocalDateTime acceptModifyDate;

}
