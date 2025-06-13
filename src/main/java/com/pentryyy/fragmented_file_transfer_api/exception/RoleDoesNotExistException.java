package com.pentryyy.fragmented_file_transfer_api.exception;

import org.springframework.http.HttpStatus;

import com.pentryyy.fragmented_file_transfer_api.exception.custom.behaviour.CustomHttpException;

public class RoleDoesNotExistException extends RuntimeException implements CustomHttpException {
    public RoleDoesNotExistException(String rolename){
        super("Роль с названием " + rolename + " не найдена");
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
