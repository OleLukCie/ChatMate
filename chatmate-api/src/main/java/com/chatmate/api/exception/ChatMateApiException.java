package com.chatmate.api.exception;

/**
 * Exception thrown when ChatMate API call fails.
 */
public class ChatMateApiException extends RuntimeException {
    
    private final int statusCode;
    
    public ChatMateApiException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }
    
    public ChatMateApiException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = -1;
    }
    
    public int statusCode() {
        return statusCode;
    }
}
