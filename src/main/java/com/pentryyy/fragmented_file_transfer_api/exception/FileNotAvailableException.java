package com.pentryyy.fragmented_file_transfer_api.exception;

import org.springframework.http.HttpStatus;

import com.pentryyy.fragmented_file_transfer_api.exception.custom.behaviour.CustomHttpException;

public class FileNotAvailableException extends RuntimeException implements CustomHttpException {
    public FileNotAvailableException(){
        super("Файл не доступен для скачивания");
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
