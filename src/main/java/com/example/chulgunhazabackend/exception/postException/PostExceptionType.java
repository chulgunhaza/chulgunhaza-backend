package com.example.chulgunhazabackend.exception.postException;

import lombok.Getter;

@Getter
public enum PostExceptionType {
    POST_NOT_FOUND(404,"존재하지 않는 게시글 입니다."),
    NOT_POST_AUTHOR(403, "본인이 작성한 게시글만 삭제·수정할 수 있습니다.");

    private final int status;
    private final String message;

    PostExceptionType(int status ,String message){
        this.status = status;
        this.message = message;
    }

}
