package com.pentryyy.fragmented_file_transfer_api.controller;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pentryyy.fragmented_file_transfer_api.dto.PasswordChangeRequest;
import com.pentryyy.fragmented_file_transfer_api.dto.RoleUpdateRequest;
import com.pentryyy.fragmented_file_transfer_api.dto.UserUpdateRequest;
import com.pentryyy.fragmented_file_transfer_api.model.User;
import com.pentryyy.fragmented_file_transfer_api.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/users")
@Validated
@Tag(name = "Пользователи", description = "Управление пользователями")
public class UserController {
    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Operation(summary = "Получить всех пользователей", description = "Возвращает страницу пользователей с возможностью сортировки и пагинации.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Успешно получен список пользователей",
                     content = @Content(mediaType = "application/json",
                     schema = @Schema(implementation = Page.class))),
        @ApiResponse(responseCode = "403", description = "Доступ запрещен")
    })
    @GetMapping("/get-all-users")
    public ResponseEntity<Page<User>> getAllUsers(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int limit,
        @RequestParam(defaultValue = "id") String sortBy,
        @RequestParam(defaultValue = "ASC") String sortOrder
    ) {
        
        Page<User> users = userService.getAllUsers(
            page, 
            limit, 
            sortBy, 
            sortOrder
        );
        return ResponseEntity.ok(users);
    }

    @Operation(summary = "Получить пользователя по ID", description = "Возвращает данные пользователя по его ID.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Успешно получен пользователь",
                     content = @Content(mediaType = "application/json",
                     schema = @Schema(implementation = User.class))),
        @ApiResponse(responseCode = "404", description = "Пользователь не найден")
    })
    @GetMapping("/get-user/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        User user = userService.findById(id);
        return ResponseEntity.ok(user);
    }

    @Operation(summary = "Изменить роль пользователя", description = "Изменяет роль пользователя по его ID.")
    @PatchMapping("/change-role/{id}")
    public ResponseEntity<?> changeRole(
        @PathVariable Long id,  
        @RequestBody @Valid RoleUpdateRequest request
    ) {
          
        userService.changeRole(id, request.getRolename());

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("rolename", request.getRolename())
                  .put("userId", id);

        return ResponseEntity.ok()
                             .contentType(MediaType.APPLICATION_JSON)
                             .body(jsonObject.toString());
    }

    @Operation(summary = "Отключить пользователя", description = "Деактивирует учетную запись пользователя.")
    @PatchMapping("/disable-user/{id}")
    public ResponseEntity<?> disableUser(@PathVariable Long id) {
        userService.disableUser(id);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("message", "Пользователь отключен");
        return ResponseEntity.ok()
                             .contentType(MediaType.APPLICATION_JSON)
                             .body(jsonObject.toString());
    }

    @Operation(summary = "Активировать пользователя", description = "Активирует учетную запись пользователя.")
    @PatchMapping("/enable-user/{id}")
    public ResponseEntity<?> enableUser(@PathVariable Long id) {
        userService.enableUser(id);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("message", "Пользователь активен");
        return ResponseEntity.ok()
                             .contentType(MediaType.APPLICATION_JSON)
                             .body(jsonObject.toString());
    }

    @Operation(summary = "Обновить данные пользователя", description = "Обновляет данные пользователя по его ID.")
    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateUser(
        @PathVariable Long id, 
        @RequestBody @Valid UserUpdateRequest request
    ) {   
        
        userService.updateUser(id, request);
        
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("message", "Данные пользователя обновлены");
        return ResponseEntity.ok()
                             .contentType(MediaType.APPLICATION_JSON)
                             .body(jsonObject.toString());
    }

    @Operation(summary = "Изменить пароль пользователя", description = "Обновляет пароль пользователя по его ID.")
    @PatchMapping("/change-pass/{id}")
    public ResponseEntity<?> changePassword(
        @PathVariable Long id, 
        @RequestBody @Valid PasswordChangeRequest request
    ) {
        
        String encryptedPassword = passwordEncoder.encode(request.getPassword());
        userService.changePassword(id, encryptedPassword);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("message", "Пароль успешно обновлен");
        return ResponseEntity.ok()
                             .contentType(MediaType.APPLICATION_JSON)
                             .body(jsonObject.toString());
    }
}
