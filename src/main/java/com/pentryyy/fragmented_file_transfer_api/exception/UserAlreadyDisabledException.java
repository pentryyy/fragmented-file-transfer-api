package com.pentryyy.fragmented_file_transfer_api.exception;

public class UserAlreadyDisabledException extends RuntimeException {
    public UserAlreadyDisabledException() {
        super("Пользователь уже отключен");
    }
}
