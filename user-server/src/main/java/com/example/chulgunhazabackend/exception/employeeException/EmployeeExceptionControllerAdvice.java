package com.example.chulgunhazabackend.exception.employeeException;

import com.example.chulgunhazabackend.controller.EmployeeController;
import com.example.chulgunhazabackend.exception.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackageClasses = EmployeeController.class)
public class EmployeeExceptionControllerAdvice {
    @ExceptionHandler(EmployeeException.class)
    public ResponseEntity<ErrorResponse> handlePostException(EmployeeException ex) {
        ErrorResponse errorResponse = new ErrorResponse(ex.getEmployeeExceptionType().getStatus(), ex.getEmployeeExceptionType().getMessage());
        return ResponseEntity.status(ex.getEmployeeExceptionType().getStatus()).body(errorResponse);
    }
}
