package com.pentryyy.fragmented_file_transfer_api.exception;

public class UsernameAlreadyExistsException extends RuntimeException {
    public UsernameAlreadyExistsException(String username) {
        super("Пользователь с именем - " + username + " уже существует");
    }
}
