package com.pentryyy.fragmented_file_transfer_api.exception;

import org.springframework.http.HttpStatus;

import com.pentryyy.fragmented_file_transfer_api.exception.custom.behaviour.CustomHttpException;

public class FileNotAssembledException extends RuntimeException implements CustomHttpException {
    public FileNotAssembledException(){
        super("Во время сбора файла из чанков произошла ошибка");
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.EXPECTATION_FAILED;
    }

    @Override
    public String getErrorMessage() {
        return getMessage();
    }
}
