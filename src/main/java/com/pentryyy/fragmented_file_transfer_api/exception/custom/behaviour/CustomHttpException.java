package com.pentryyy.fragmented_file_transfer_api.exception.custom.behaviour;

import org.springframework.http.HttpStatus;

public interface CustomHttpException {
    public HttpStatus getHttpStatus();
    public String getErrorMessage();
}
