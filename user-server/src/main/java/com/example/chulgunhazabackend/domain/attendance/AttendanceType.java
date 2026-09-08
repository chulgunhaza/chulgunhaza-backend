package com.example.chulgunhazabackend.domain.attendance;

import lombok.Getter;

@Getter
public enum AttendanceType {
    NORMAL("출근"),
    LATE("지각"),
    ABSENT("결근"),
    ANNUAL("연차")
    ;

    private final String value;

    AttendanceType(String value) {
        this.value = value;
    }
}
