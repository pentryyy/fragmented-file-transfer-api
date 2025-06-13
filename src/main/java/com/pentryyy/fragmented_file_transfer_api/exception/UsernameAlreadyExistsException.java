package com.pentryyy.fragmented_file_transfer_api.exception;

import org.springframework.http.HttpStatus;

import com.pentryyy.fragmented_file_transfer_api.exception.custom.behaviour.CustomHttpException;

public class UsernameAlreadyExistsException extends RuntimeException implements CustomHttpException {
    public UsernameAlreadyExistsException(String username) {
        super("Пользователь с именем - " + username + " уже существует");
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
