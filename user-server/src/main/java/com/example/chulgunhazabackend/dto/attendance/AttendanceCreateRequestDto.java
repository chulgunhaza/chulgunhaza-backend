package com.example.chulgunhazabackend.dto.attendance;

import com.example.chulgunhazabackend.domain.attendance.AttendanceRecord;
import com.example.chulgunhazabackend.domain.attendance.AttendanceType;
import com.example.chulgunhazabackend.domain.member.Employee;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.LocalTime;


@Getter
@AllArgsConstructor
@ToString
public class AttendanceCreateRequestDto implements Serializable {


    @NotNull(message = "출근자의 사원 번호가 누락되었습니다.")
    private Long employeeNo; // 사원 번호

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @NotNull(message = "출근 시간이 누락되었습니다.")
    private LocalDateTime checkInTime;

    @Setter
    private String mqFailMessage;


    public AttendanceRecord toEntity(Employee employee){
        return AttendanceRecord.builder()
                .employee(employee)
                .checkInTime(checkInTime)
                .attendanceType(setAttendanceType(checkInTime))
                .build();
    }

    private AttendanceType setAttendanceType(LocalDateTime checkInTime){

        // 기준 시간 (09 : 00)
        LocalTime standardTime = LocalTime.of(9, 0);

        return checkInTime.toLocalTime().isAfter(standardTime)
                ? AttendanceType.LATE : AttendanceType.NORMAL;
        // 결근 처리 추가 예정
    }

}
