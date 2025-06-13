package com.pentryyy.fragmented_file_transfer_api.exception;

import org.springframework.http.HttpStatus;

import com.pentryyy.fragmented_file_transfer_api.exception.custom.behaviour.CustomHttpException;

public class EmailAlreadyExistsException extends RuntimeException implements CustomHttpException {
    public EmailAlreadyExistsException(String email) {
        super("Пользователь с email - " + email + " уже существует");
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.CONFLICT;
    }

    @Override
    public String getErrorMessage() {
        return getMessage();
    }
}
