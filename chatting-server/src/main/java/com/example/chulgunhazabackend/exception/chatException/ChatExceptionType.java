package com.example.chulgunhazabackend.exception.chatException;

import lombok.Getter;

@Getter
public enum ChatExceptionType {

    ALREADY_CHAT_ROOM(404, "이미 존재하는 채팅방입니다."),
    NOT_FOUND_CHAT_ROOM(404, "존재하지 않는 채팅방입니다."),
    NOT_FOUND_CHAT_USER(404, "존재하지 않는 채팅방 유저입니다."),
    NO_CHAT_PARTNER(400, "대화 상대를 선택해야 합니다."),
    // #101: 예전엔 EmployeeException(user-server 전용 타입)을 썼는데, chatting-server가
    // 물리 분리되면서 그 타입 자체가 없어졌다 — EmployeeDirectoryClient로 조회했는데
    // 요청한 id가 응답에 없으면(=진짜 존재하지 않음) 이걸로 통일한다.
    EMPLOYEE_NOT_FOUND(404, "존재하지 않는 사원입니다."),
    // user-server 내부 API 호출 자체가 실패한 경우(네트워크/타임아웃 등) — "존재하지
    // 않음"과는 다른 문제라 구분한다. 대시보드 통계(#100)처럼 조용히 기본값으로
    // 대체하지 않는 이유는 방 목록/생성이 사원 정보 없이는 의미가 없기 때문.
    EMPLOYEE_SERVICE_UNAVAILABLE(503, "사원 정보를 확인할 수 없습니다. 잠시 후 다시 시도해주세요.");

    private final int status;
    private final String message;

    ChatExceptionType(int status, String message) {
        this.status = status;
        this.message = message;
    }
}
