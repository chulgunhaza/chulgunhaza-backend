package com.example.chulgunhazabackend.domain.leaveWork;

import com.example.chulgunhazabackend.domain.common.BaseEntity;
import com.example.chulgunhazabackend.domain.member.Employee;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name="leave_records")
@Getter
public class LeaveRecord extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "leave_record_id")
    private Long id;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @Column(nullable = false)
    private Long attendanceRecordId; // 논리적 키 값

    @Column(nullable = false)
    private LocalDateTime checkOutTime;

    @Column(nullable = false)
    private double dayTotalWorkHours;

}
