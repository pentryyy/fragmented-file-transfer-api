package com.pentryyy.fragmented_file_transfer_api.exception;

import org.springframework.http.HttpStatus;

import com.pentryyy.fragmented_file_transfer_api.exception.custom.behaviour.CustomHttpException;

public class UserAlreadyEnabledException extends RuntimeException implements CustomHttpException {
    public UserAlreadyEnabledException() {
        super("Пользователь уже активен");
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.LOCKED;
    }

    @Override
    public String getErrorMessage() {
        return getMessage();
    }
}

