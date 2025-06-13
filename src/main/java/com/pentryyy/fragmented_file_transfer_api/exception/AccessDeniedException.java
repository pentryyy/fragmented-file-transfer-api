package com.pentryyy.fragmented_file_transfer_api.exception;

public class AccessDeniedException extends RuntimeException {
    public AccessDeniedException(String errorMessage) {
        super(errorMessage);
    }
}
