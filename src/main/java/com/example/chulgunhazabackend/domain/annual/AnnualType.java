package com.example.chulgunhazabackend.domain.annual;

import lombok.Getter;

@Getter
public enum AnnualType {
    ANNUAL("연차", 1.0),
    ANNUAL_AM("오전 반차", 0.5),
    ANNUAL_PM("오후 반차", 0.5)
    ;

    private final String value;
    private final double dayCost; // #48: 이 종류의 연차를 쓰면 잔여 연차에서 며칠 차감할지

    AnnualType(String value, double dayCost) {
        this.value = value;
        this.dayCost = dayCost;
    }
}
