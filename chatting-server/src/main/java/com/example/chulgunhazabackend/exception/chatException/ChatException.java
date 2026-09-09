package com.example.chulgunhazabackend.exception.chatException;

import lombok.Getter;

@Getter
public class ChatException extends RuntimeException {

    private final ChatExceptionType chatExceptionType;

    public ChatException(ChatExceptionType chatExceptionType) {
        super(chatExceptionType.getMessage());
        this.chatExceptionType = chatExceptionType;
    }

}
