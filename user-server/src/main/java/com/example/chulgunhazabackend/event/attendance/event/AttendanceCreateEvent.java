package com.example.chulgunhazabackend.event.attendance.event;

import com.example.chulgunhazabackend.event.common.Event;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class AttendanceCreateEvent extends Event {

    private final String message;

    public AttendanceCreateEvent(Long employeeNumber) {
        super(employeeNumber);
        this.message = "출근등록이 완료되었습니다.";
    }
}
