package com.factorx.news;

public class NewsSourceException extends RuntimeException {

    private final boolean retryable;

    public NewsSourceException(String message, boolean retryable) {
        super(message);
        this.retryable = retryable;
    }

    public NewsSourceException(String message, Throwable cause, boolean retryable) {
        super(message, cause);
        this.retryable = retryable;
    }

    public boolean retryable() {
        return retryable;
    }
}
