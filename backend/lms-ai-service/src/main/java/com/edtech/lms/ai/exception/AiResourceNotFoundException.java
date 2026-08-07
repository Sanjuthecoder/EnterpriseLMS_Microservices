package com.edtech.lms.ai.exception;

/** Thrown when a requested AI resource (quiz session, insight report, course context) is not found. */
public class AiResourceNotFoundException extends RuntimeException {

    public AiResourceNotFoundException(final String message) {
        super(message);
    }
}
