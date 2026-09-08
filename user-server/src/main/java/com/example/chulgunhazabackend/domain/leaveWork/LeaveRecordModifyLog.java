package com.example.chulgunhazabackend.domain.leaveWork;

import com.example.chulgunhazabackend.domain.member.Employee;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name="leave_record_modify_logs")
@Getter
public class LeaveRecordModifyLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attendance_record_log_id")
    private Long id;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leave_record_id")
    private LeaveRecord leaveRecord;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "at_request_employee_id")
    private Employee requestEmployee;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "app_employee_id")
    private Employee appEmployee;

    @Column(nullable = false)
    private LocalDateTime oldValue;

    @Column(nullable = false)
    private LocalDateTime modifyValue;

    @Column(nullable = false)
    private String modificationReason;

    @Column(nullable = false)
    private LocalDateTime acceptModifyDate;
}
