package com.example.chulgunhazabackend.exception.employeeException;

import lombok.Getter;

@Getter
public enum EmployeeExceptionType {
    ALREADY_EXIST_EMAIL(404,"이미 존재하는 이메일입니다."),
    NOT_EXIST_USER(404, "존재하지 않는 사원입니다."),
    ALREADY_CHANGED(404, "사원 정보가 다른 사용자에 의해 변경되었습니다."),
    // #98: refresh 토큰 쿠키가 없거나, 서명/만료 검증에 실패했거나, Redis에 저장된
    // jti와 일치하지 않는 경우(로그아웃됐거나 이미 회전된 refresh 토큰 재사용 시도) 전부 이걸로 통일
    INVALID_REFRESH_TOKEN(401, "인증이 만료되었습니다. 다시 로그인해 주세요.")
    ;

    EmployeeExceptionType(int status, String message) {
        this.status = status;
        this.message = message;
    }

    private final int status;
    private final String message;

}
