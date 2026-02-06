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
public class TransactionController {

    private final TransactionService transactionService;

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

    @PostMapping("/deposit")
    public ResponseEntity<ApiResponse<Transaction>> deposit(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestParam BigDecimal amount,
            Authentication authentication) {

        Long finalUserId = getUserId(userId, authentication);
        Transaction transaction = transactionService.processDeposit(finalUserId, amount);
        return ResponseEntity.ok(ApiResponse.success(transaction, "Depósito realizado exitosamente"));
    }

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
