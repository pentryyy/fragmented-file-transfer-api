package com.pentryyy.fragmented_file_transfer_api.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pentryyy.fragmented_file_transfer_api.dto.JwtAuthenticationResponse;
import com.pentryyy.fragmented_file_transfer_api.dto.SignInRequest;
import com.pentryyy.fragmented_file_transfer_api.dto.SignUpRequest;
import com.pentryyy.fragmented_file_transfer_api.exception.JwtAuthenticationException;
import com.pentryyy.fragmented_file_transfer_api.service.AuthenticationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Аутентификация", description = "Эндпоинты для регистрации и входа пользователей")
@Validated
public class AuthController {
    @Autowired
    private AuthenticationService authenticationService;
    
    @Operation(
        summary = "Регистрация пользователя",
        description = "Позволяет зарегистрировать нового пользователя, принимая его учетные данные"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Пользователь успешно зарегистрирован",
                     content = @Content(mediaType = "application/json", schema = @Schema(implementation = JwtAuthenticationResponse.class))),
        @ApiResponse(responseCode = "400", description = "Некорректный запрос",
                     content = @Content(mediaType = "application/json",
                     schema = @Schema(example = "{ \"error\": \"Некорректное значение для поля: username\", \"message\": \"Имя пользователя не может быть пустым\" }"))),
        @ApiResponse(responseCode = "409", description = "Запрос на регистрацию по уже имеющимся данным",
                     content = @Content(mediaType = "application/json",
                     schema = @Schema(example = "{ \"error\": \"Пользователь уже существует\", \"message\": \"Имя пользователя уже занято\" }")))
    })
    @PostMapping("/sign-up")
    public ResponseEntity<?> signUp(@RequestBody @Valid SignUpRequest request) {
        return ResponseEntity.ok(authenticationService.signUp(request));
    }

    @Operation(
        summary = "Авторизация пользователя",
        description = "Позволяет авторизовать пользователя, проверяя его учетные данные"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Успешный вход",
                     content = @Content(mediaType = "application/json", schema = @Schema(implementation = JwtAuthenticationResponse.class))),
        @ApiResponse(responseCode = "403", description = "Неверные учетные данные",
                     content = @Content(mediaType = "application/json", 
                     schema = @Schema(example = "{}")))
    })
    @PostMapping("/sign-in")
    public ResponseEntity<?> signIn(@RequestBody @Valid SignInRequest request) {
        try {
            return ResponseEntity.ok(authenticationService.signIn(request));
        } catch (BadCredentialsException e) {
           throw new JwtAuthenticationException("Неверные данные пользователя");
        } catch (DisabledException e) {
           throw new JwtAuthenticationException("Аккаунт пользователя отключен");
        }
    }
}