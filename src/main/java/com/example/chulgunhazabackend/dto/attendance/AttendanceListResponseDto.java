package com.example.chulgunhazabackend.dto.attendance;

import com.example.chulgunhazabackend.domain.attendance.AttendanceRecord;
import com.example.chulgunhazabackend.domain.attendance.AttendanceType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class AttendanceListResponseDto {

    private Long employeeNo;

    private String employeeName;

    private LocalDateTime checkInTime;

    private AttendanceType attendanceType;

    public static AttendanceListResponseDto fromEntity(AttendanceRecord record) {
        return new AttendanceListResponseDto(
                record.getEmployee().getEmployeeNo(),
                record.getEmployee().getName(),
                record.getCheckInTime(),
                record.getAttendanceType()
        );
    }
}
