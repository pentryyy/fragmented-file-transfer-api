package com.pentryyy.fragmented_file_transfer_api.exception;

import org.springframework.http.HttpStatus;

import com.pentryyy.fragmented_file_transfer_api.exception.custom.behaviour.CustomHttpException;

public class FileNotSplitedException extends RuntimeException implements CustomHttpException {
    public FileNotSplitedException(){
        super("Во время разложения файла на чанки произошла ошибка");
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
