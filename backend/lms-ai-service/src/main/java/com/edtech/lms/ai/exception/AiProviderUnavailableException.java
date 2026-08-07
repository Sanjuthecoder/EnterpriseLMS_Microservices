package com.edtech.lms.ai.exception;

/** Thrown when the Gemini API is unreachable or returns an unusable response after all retries. */
public class AiProviderUnavailableException extends RuntimeException {

    public AiProviderUnavailableException(final String message) {
        super(message);
    }
}
