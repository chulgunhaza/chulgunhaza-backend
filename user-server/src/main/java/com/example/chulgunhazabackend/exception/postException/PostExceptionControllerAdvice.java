package com.example.chulgunhazabackend.exception.postException;

import com.example.chulgunhazabackend.controller.PostController;
import com.example.chulgunhazabackend.exception.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackageClasses = PostController.class)
public class PostExceptionControllerAdvice {

    @ExceptionHandler(PostException.class)
    public ResponseEntity<ErrorResponse> handlePostException(PostException ex) {
        ErrorResponse errorResponse = new ErrorResponse(ex.getPostExceptionType().getStatus(), ex.getPostExceptionType().getMessage());
        return ResponseEntity.status(ex.getPostExceptionType().getStatus()).body(errorResponse);
    }

}
