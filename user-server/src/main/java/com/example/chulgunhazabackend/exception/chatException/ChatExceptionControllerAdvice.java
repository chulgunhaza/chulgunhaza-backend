package com.example.chulgunhazabackend.exception.chatException;

import com.example.chulgunhazabackend.controller.ChatController;
import com.example.chulgunhazabackend.exception.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackageClasses = ChatController.class)
public class ChatExceptionControllerAdvice {

    @ExceptionHandler(ChatException.class)
    public ResponseEntity<ErrorResponse> handlePostException(ChatException ex) {
        ErrorResponse errorResponse = new ErrorResponse(ex.getChatExceptionType().getStatus(), ex.getChatExceptionType().getMessage());
        return ResponseEntity.status(ex.getChatExceptionType().getStatus()).body(errorResponse);
    }

}
