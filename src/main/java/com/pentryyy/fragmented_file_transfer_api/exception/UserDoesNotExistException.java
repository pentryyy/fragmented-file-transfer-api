package com.pentryyy.fragmented_file_transfer_api.exception;

import org.springframework.http.HttpStatus;

import com.pentryyy.fragmented_file_transfer_api.exception.custom.behaviour.CustomHttpException;

public class UserDoesNotExistException extends RuntimeException implements CustomHttpException {
    
    public UserDoesNotExistException(Long id){
        super("Пользователь с id " + id + " не найден");
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.NOT_FOUND;
    }

    @Override
    public String getErrorMessage() {
        return getMessage();
    }
}
