package com.pentryyy.fragmented_file_transfer_api.exception;

public class RoleDoesNotExistException extends RuntimeException  {
    public RoleDoesNotExistException(String rolename){
        super("Роль с названием " + rolename + " не найдена");
    }
}
