package com.example.chulgunhazabackend.exception.employeeException;

import lombok.Getter;

@Getter
public class EmployeeException extends RuntimeException {

    private final EmployeeExceptionType employeeExceptionType;

    public EmployeeException(EmployeeExceptionType employeeExceptionType){
        super(employeeExceptionType.getMessage());
        this.employeeExceptionType = employeeExceptionType;
    }

}
