package com.pentryyy.fragmented_file_transfer_api.exception;

public class UserAlreadyEnabledException extends RuntimeException {
    public UserAlreadyEnabledException() {
        super("Пользователь уже активен");
    }
}
