package com.naranjax.transaction.controller;

import com.naranjax.common.dto.ApiResponse;
import com.naranjax.transaction.dto.TransactionRequest;
import com.naranjax.transaction.entity.Transaction;
import com.naranjax.transaction.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/deposit")
    public ResponseEntity<ApiResponse<Transaction>> deposit(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam BigDecimal amount) {
        Transaction transaction = transactionService.processDeposit(userId, amount);
        return ResponseEntity.ok(ApiResponse.success(transaction, "Depósito realizado exitosamente"));
    }

    @PostMapping("/transfer")
    public ResponseEntity<ApiResponse<Transaction>> transfer(
            @RequestHeader("X-User-Id") Long senderId,
            @Valid @RequestBody TransactionRequest request) {
        Transaction transaction = transactionService.processTransfer(senderId, request);
        return ResponseEntity.ok(ApiResponse.success(transaction, "Transferencia realizada exitosamente"));
    }
}
