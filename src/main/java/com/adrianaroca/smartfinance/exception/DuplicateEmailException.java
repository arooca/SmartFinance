package com.adrianaroca.smartfinance.exception;

public class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException(
            String message) {

        super(message);
    }
}