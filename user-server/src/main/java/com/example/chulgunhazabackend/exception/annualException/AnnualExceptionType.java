package com.example.chulgunhazabackend.exception.annualException;

import lombok.Getter;

@Getter
public enum AnnualExceptionType {
    INSUFFICIENT_BALANCE(400, "잔여 연차가 부족합니다."),
    ANNUAL_RECORD_NOT_FOUND(404, "연차 사용 내역을 찾을 수 없습니다."),
    ALREADY_REJECTED(400, "이미 반려된 연차 사용 내역입니다.")
    ;

    AnnualExceptionType(int status, String message) {
        this.status = status;
        this.message = message;
    }

    private final int status;
    private final String message;
}
