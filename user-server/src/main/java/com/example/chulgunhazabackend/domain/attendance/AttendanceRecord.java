package com.example.chulgunhazabackend.domain.attendance;

import com.example.chulgunhazabackend.domain.common.BaseEntity;
import com.example.chulgunhazabackend.domain.member.Employee;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name="AttendanceRecords")
@ToString
@Getter
public class AttendanceRecord extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attendance_record_id")
    private Long id;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @Column(nullable = false)
    private LocalDateTime checkInTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttendanceType attendanceType;


    @Builder
    public AttendanceRecord(Employee employee, LocalDateTime checkInTime, AttendanceType attendanceType) {
        this.employee = employee;
        this.checkInTime = checkInTime;
        this.attendanceType = attendanceType;
    }
}
