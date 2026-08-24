package com.example.chulgunhazabackend.exception.annualException;

import com.example.chulgunhazabackend.controller.AnnualController;
import com.example.chulgunhazabackend.exception.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackageClasses = AnnualController.class)
public class AnnualExceptionControllerAdvice {
    @ExceptionHandler(AnnualException.class)
    public ResponseEntity<ErrorResponse> handleAnnualException(AnnualException ex) {
        ErrorResponse errorResponse = new ErrorResponse(ex.getAnnualExceptionType().getStatus(), ex.getAnnualExceptionType().getMessage());
        return ResponseEntity.status(ex.getAnnualExceptionType().getStatus()).body(errorResponse);
    }
}
