package com.pentryyy.fragmented_file_transfer_api.exception;

import org.springframework.http.HttpStatus;

import com.pentryyy.fragmented_file_transfer_api.exception.custom.behaviour.CustomHttpException;

public class ReceiverNotRegisteredException extends RuntimeException implements CustomHttpException {
    public ReceiverNotRegisteredException(String errorMessage){
        super(errorMessage);
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    @Override
    public String getErrorMessage() {
        return getMessage();
    }
}
