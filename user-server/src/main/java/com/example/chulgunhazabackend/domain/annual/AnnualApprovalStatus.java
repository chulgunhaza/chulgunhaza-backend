package com.example.chulgunhazabackend.domain.annual;

import lombok.Getter;

@Getter
public enum AnnualApprovalStatus {
    PENDING("대기"),
    APPROVED("승인"),
    REJECTED("반려")
    ;

    private final String value;
    AnnualApprovalStatus(String value) {
        this.value = value;
    }
}
