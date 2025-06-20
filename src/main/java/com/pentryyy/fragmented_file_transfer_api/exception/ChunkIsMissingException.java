package com.pentryyy.fragmented_file_transfer_api.exception;

import org.springframework.http.HttpStatus;

import com.pentryyy.fragmented_file_transfer_api.exception.custom.behaviour.CustomHttpException;

public class ChunkIsMissingException extends RuntimeException implements CustomHttpException {
    public ChunkIsMissingException(int  sequenceElement) {
        super("Пропущенный фрагмент #" + sequenceElement + " во время сборки");
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
