package com.example.chulgunhazabackend.exception.annualException;

import lombok.Getter;

@Getter
public class AnnualException extends RuntimeException {

    private final AnnualExceptionType annualExceptionType;

    public AnnualException(AnnualExceptionType annualExceptionType) {
        super(annualExceptionType.getMessage());
        this.annualExceptionType = annualExceptionType;
    }
}
