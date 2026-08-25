package com.example.chulgunhazabackend.event.annual.event;

import com.example.chulgunhazabackend.event.common.Event;
import lombok.Getter;
import lombok.ToString;

// #46: 연차 사용 완료 시 MAIN 채널로 알림을 보내기 위한 이벤트
@Getter
@ToString
public class AnnualUseEvent extends Event {

    private final double remainingAnnualCount;

    public AnnualUseEvent(Long employeeNumber, double remainingAnnualCount) {
        super(employeeNumber);
        this.remainingAnnualCount = remainingAnnualCount;
    }
}
