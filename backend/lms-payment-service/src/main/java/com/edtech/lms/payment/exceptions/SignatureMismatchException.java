package com.edtech.lms.payment.exceptions;
public class SignatureMismatchException extends RuntimeException {
    public SignatureMismatchException(String message) {
        super(message);
    }
}
