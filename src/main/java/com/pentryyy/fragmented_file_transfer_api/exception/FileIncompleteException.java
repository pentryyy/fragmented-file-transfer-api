package com.pentryyy.fragmented_file_transfer_api.exception;

import org.springframework.http.HttpStatus;

import com.pentryyy.fragmented_file_transfer_api.exception.custom.behaviour.CustomHttpException;

public class FileIncompleteException extends RuntimeException implements CustomHttpException {
    public FileIncompleteException(int missingCount) {
        super("Файл неполон. Пропущенные фрагменты: " + missingCount);
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.UNPROCESSABLE_ENTITY;
    }

    @Override
    public String getErrorMessage() {
        return getMessage();
    }
}
