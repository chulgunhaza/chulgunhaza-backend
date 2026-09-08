package com.example.chulgunhazabackend.domain.attendance;

import com.example.chulgunhazabackend.domain.common.BaseEntity;
import com.example.chulgunhazabackend.domain.member.Employee;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name="attendance_record_summarys")
@Getter
public class AttendanceRecordSummary extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attendance_record_summary_id")
    private Long id;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @Column(nullable = false)
    private LocalDate summaryDate;

    @Column(nullable = false)
    private double totalWorkHours;

    @Column(nullable = false)
    private Long totalWorkDays;

    @Column(nullable = false)
    private double overTimeHours;

    @Column(nullable = false)
    private Long tardinessCount;

    @Column(nullable = false)
    private Long absentCount;

}
