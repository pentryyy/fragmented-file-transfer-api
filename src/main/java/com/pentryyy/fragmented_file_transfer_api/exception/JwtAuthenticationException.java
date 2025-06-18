package com.pentryyy.fragmented_file_transfer_api.exception;

import org.springframework.http.HttpStatus;

import com.pentryyy.fragmented_file_transfer_api.exception.custom.behaviour.CustomHttpException;

public class JwtAuthenticationException extends RuntimeException implements CustomHttpException {
    public JwtAuthenticationException(String errorMessage){
        super(errorMessage);
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.UNAUTHORIZED;
    }

    @Override
    public String getErrorMessage() {
        return getMessage();
    }
}
