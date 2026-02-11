package com.naranjax.transaction.controller;

import com.naranjax.common.dto.ApiResponse;
import com.naranjax.transaction.dto.TransactionRequest;
import com.naranjax.transaction.entity.Transaction;
import com.naranjax.transaction.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.naranjax.common.security.UserPrincipal;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;

import java.util.List;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
@io.swagger.v3.oas.annotations.tags.Tag(name = "Transactions", description = "Operaciones financieras y consulta de movimientos")
public class TransactionController {

    private final TransactionService transactionService;

    @io.swagger.v3.oas.annotations.Operation(summary = "Obtener historial de transacciones", description = "Devuelve la lista de transacciones de un usuario ordenadas por fecha reciente")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Historial obtenido correctamente"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "No autorizado para ver estas transacciones")
    })
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<Transaction>>> getTransactions(
            @PathVariable Long userId,
            Authentication authentication) {

        Long currentUserId = getUserId(null, authentication);
        if (!currentUserId.equals(userId)) {
            // Permitir si es ADMIN (opcional, pero buena práctica)
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            if (!isAdmin) {
                throw new com.naranjax.common.exception.BusinessException(
                        "No tienes permisos para ver estas transacciones");
            }
        }

        return ResponseEntity.ok(ApiResponse.success(transactionService.getTransactionsByUser(userId),
                "Historial obtenido exitosamente"));
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Realizar depósito", description = "Incrementa el saldo de una cuenta mediante un depósito")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Depósito procesado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Monto inválido")
    })
    @PostMapping("/deposit")
    public ResponseEntity<ApiResponse<Transaction>> deposit(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestParam BigDecimal amount,
            Authentication authentication) {

        Long finalUserId = getUserId(userId, authentication);
        Transaction transaction = transactionService.processDeposit(finalUserId, amount);
        return ResponseEntity.ok(ApiResponse.success(transaction, "Depósito realizado exitosamente"));
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Realizar transferencia P2P", description = "Transfiere dinero entre cuentas internas")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Transferencia exitosa"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Saldo insuficiente o destinatario inválido")
    })
    @PostMapping("/transfer")
    public ResponseEntity<ApiResponse<Transaction>> transfer(
            @RequestHeader(value = "X-User-Id", required = false) Long senderId,
            @Valid @RequestBody TransactionRequest request,
            Authentication authentication) {

        Long finalSenderId = getUserId(senderId, authentication);
        Transaction transaction = transactionService.processTransfer(finalSenderId, request);
        return ResponseEntity.ok(ApiResponse.success(transaction, "Transferencia realizada exitosamente"));
    }

    private Long getUserId(Long headerId, Authentication authentication) {
        if (headerId != null)
            return headerId;
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            return ((UserPrincipal) authentication.getPrincipal()).getId();
        }
        throw new IllegalArgumentException("El ID de usuario es obligatorio");
    }
}
