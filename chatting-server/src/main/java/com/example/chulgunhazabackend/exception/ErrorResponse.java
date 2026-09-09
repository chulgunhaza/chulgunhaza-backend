package com.example.chulgunhazabackend.exception;

import lombok.Getter;

// #101: user-server의 ErrorResponse와 동일 — BaseEntity/PageDto와 같은 이유로
// common에 두지 않고 그대로 복제한다(서비스 간 wire 계약이 아니라 이 서비스가
// 자기 REST 응답을 직렬화하는 내부 형태일 뿐).
@Getter
public class ErrorResponse {

    private final int status;
    private final String message;

    public ErrorResponse(int status, String message) {
        this.status = status;
        this.message = message;
    }
}
