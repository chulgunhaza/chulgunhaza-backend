package com.example.chulgunhazabackend.exception.chatException;

import lombok.Getter;

@Getter
public enum ChatExceptionType {

    ALREADY_CHAT_ROOM(404, "이미 존재하는 채팅방입니다."),
    NOT_FOUND_CHAT_ROOM(404, "존재하지 않는 채팅방입니다."),
    NOT_FOUND_CHAT_USER(404, "존재하지 않는 채팅방 유저입니다."),
    NO_CHAT_PARTNER(400, "대화 상대를 선택해야 합니다.");

    private final int status;
    private final String message;

    ChatExceptionType(int status, String message) {
        this.status = status;
        this.message = message;
    }
}
