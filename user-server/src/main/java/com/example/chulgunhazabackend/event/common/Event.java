package com.example.chulgunhazabackend.event.common;

import lombok.Getter;

@Getter
public abstract class Event {

    // 이벤트 발행 시간
    private final long timestamp;

    // 진행자 사원번호
    private final Long employeeNumber;



    public Event(Long employeeNumber) {
        this.timestamp = System.currentTimeMillis();
        this.employeeNumber = employeeNumber;
    }
}
