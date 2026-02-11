package com.naranjax.auth.controller;

import com.naranjax.auth.dto.AuthResponse;
import com.naranjax.auth.dto.LoginRequest;
import com.naranjax.auth.dto.RegisterRequest;
import com.naranjax.auth.service.AuthService;
import com.naranjax.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@io.swagger.v3.oas.annotations.tags.Tag(name = "Authentication", description = "Endpoints de autenticación y registro de usuarios")
public class AuthController {

    private final AuthService authService;

    @io.swagger.v3.oas.annotations.Operation(summary = "Registrar nuevo usuario", description = "Crea una cuenta nueva en el sistema")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Usuario registrado exitosamente"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Datos de registro inválidos")
    })
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Usuario registrado exitosamente"));
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Iniciar sesión", description = "Autentica al usuario y devuelve un token JWT")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Login exitoso"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Credenciales inválidas")
    })
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Login exitoso"));
    }
}
