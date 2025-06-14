package com.pentryyy.fragmented_file_transfer_api.exception;

import org.springframework.http.HttpStatus;

import com.pentryyy.fragmented_file_transfer_api.exception.custom.behaviour.CustomHttpException;

public class FileProcessNotFoundException extends RuntimeException implements CustomHttpException {
    public FileProcessNotFoundException(String processingId){
        super("Процесс с id " + processingId + " не найден");
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
