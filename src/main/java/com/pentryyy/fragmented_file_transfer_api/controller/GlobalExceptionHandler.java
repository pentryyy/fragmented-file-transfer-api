package com.pentryyy.fragmented_file_transfer_api.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.pentryyy.fragmented_file_transfer_api.exception.AccessDeniedException;
import com.pentryyy.fragmented_file_transfer_api.exception.EmailAlreadyExistsException;
import com.pentryyy.fragmented_file_transfer_api.exception.RoleDoesNotExistException;
import com.pentryyy.fragmented_file_transfer_api.exception.UserAlreadyDisabledException;
import com.pentryyy.fragmented_file_transfer_api.exception.UserAlreadyEnabledException;
import com.pentryyy.fragmented_file_transfer_api.exception.UserDoesNotExistException;
import com.pentryyy.fragmented_file_transfer_api.exception.UsernameAlreadyExistsException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private Map<String, String> errorResponse;

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) { 
        errorResponse = new HashMap<>();
        
        ex.getBindingResult().getAllErrors().forEach(error -> {

            String fieldName    = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            
            errorResponse.put("error", "Некорректное значение для поля: " + fieldName);
            errorResponse.put("message", errorMessage);
        });

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(InvalidFormatException.class)
    public ResponseEntity<?> handleInvalidFormatException(InvalidFormatException ex) {
        errorResponse = new HashMap<>();

        // Проверяем, связано ли исключение с enum
        if (ex.getTargetType().isEnum()) {
            Class<?> enumType      = ex.getTargetType();
            Object[] enumConstants = enumType.getEnumConstants(); 

            // Преобразуем значения enum в строку
            String allowedValues = String.join(", ", 
                Arrays.stream(enumConstants)
                      .map(Object::toString)
                      .toArray(String[]::new)
            );

            errorResponse.put("error", "Некорректное значение для поля: " + ex.getPath().get(0).getFieldName());
            errorResponse.put("message", "Допустимые значения: [" + allowedValues + "]");
        } else {
            errorResponse.put("error", "Неверный формат данных");
            errorResponse.put("message", ex.getOriginalMessage());
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                             .contentType(MediaType.APPLICATION_JSON)
                             .body(errorResponse.toString());
    }

    @ExceptionHandler(UserDoesNotExistException.class)
    public ResponseEntity<Map<String, String>> handleUserDoesNotExistException(UserDoesNotExistException ex) {
        errorResponse = new HashMap<>();
        errorResponse.put("error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(UserAlreadyDisabledException.class)
    public ResponseEntity<Map<String, String>> handleUserAlreadyDisabledException(UserAlreadyDisabledException ex) {
        errorResponse = new HashMap<>();
        errorResponse.put("error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(UserAlreadyEnabledException.class)
    public ResponseEntity<Map<String, String>> handleUserAlreadyEnabledException(UserAlreadyEnabledException ex) {
        errorResponse = new HashMap<>();
        errorResponse.put("error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(UsernameAlreadyExistsException.class)
    public ResponseEntity<Map<String, String>> handleUsernameAlreadyExistsException(UsernameAlreadyExistsException ex) {
        errorResponse = new HashMap<>();
        errorResponse.put("error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<Map<String, String>> handleEmailAlreadyExistsException(EmailAlreadyExistsException ex) {
        errorResponse = new HashMap<>();
        errorResponse.put("error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    @ExceptionHandler(RoleDoesNotExistException.class)
    public ResponseEntity<Map<String, String>> handleRoleDoesNotExistException(RoleDoesNotExistException ex) {
        errorResponse = new HashMap<>();
        errorResponse.put("error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDeniedException(AccessDeniedException ex) {
        errorResponse = new HashMap<>();
        errorResponse.put("error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }
}
