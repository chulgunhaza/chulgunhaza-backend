package com.example.chulgunhazabackend.exception.annualException;

import lombok.Getter;

@Getter
public enum AnnualExceptionType {
    INSUFFICIENT_BALANCE(400, "잔여 연차가 부족합니다.")
    ;

    AnnualExceptionType(int status, String message) {
        this.status = status;
        this.message = message;
    }

    private final int status;
    private final String message;
}
