package com.naranjax.auth.exception;

import com.naranjax.common.dto.ApiResponse;
import com.naranjax.common.exception.BusinessException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(org.springframework.security.authentication.BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentialsException(
            org.springframework.security.authentication.BadCredentialsException ex,
            jakarta.servlet.http.HttpServletRequest request) {

        // Log reducido para errores de negocio comunes
        // No logueamos stacktrace completo
        return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Email o contraseña incorrectos", request.getRequestURI()));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(
            BusinessException ex,
            jakarta.servlet.http.HttpServletRequest request) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationExceptions(
            MethodArgumentNotValidException ex,
            jakarta.servlet.http.HttpServletRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return ResponseEntity.badRequest().body(ApiResponse.<Map<String, String>>builder()
                .success(false)
                .message("Error de validación")
                .data(errors)
                .path(request.getRequestURI())
                .timestamp(java.time.LocalDateTime.now())
                .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneralException(
            Exception ex,
            jakarta.servlet.http.HttpServletRequest request) {

        // Aquí sí es útil el stacktrace completo si es un error 500 real
        ex.printStackTrace();

        return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Error interno del servidor: " + ex.getMessage(), request.getRequestURI()));
    }
}
