package com.naranjax.transaction.service;

import com.naranjax.common.event.TransactionCompletedEvent;
import com.naranjax.transaction.dto.TransactionRequest;
import com.naranjax.transaction.entity.Transaction;
import com.naranjax.transaction.entity.TransactionAudit;
import com.naranjax.transaction.entity.TransactionType;
import com.naranjax.transaction.repository.TransactionAuditRepository;
import com.naranjax.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final TransactionAuditRepository auditRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private final org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();

    @Transactional
    public Transaction processDeposit(Long userId, BigDecimal amount) {
        log.info("Processing deposit for user: {} with amount: {}", userId, amount);

        Transaction transaction = Transaction.builder()
                .senderId(0L) // 0 indicates system/external deposit
                .receiverId(userId)
                .amount(amount)
                .type(TransactionType.DEPOSIT)
                .status("COMPLETED")
                .description("Deposito de saldo")
                .build();

        Transaction saved = transactionRepository.save(transaction);

        auditTransaction(saved);
        emitTransactionEvent(saved);

        return saved;
    }

    @Transactional
    public Transaction processTransfer(Long senderId, TransactionRequest request) {
        log.info("Processing transfer from: {} to: {} with amount: {}",
                senderId, request.getReceiverId(), request.getAmount());

        if (senderId.equals(request.getReceiverId())) {
            throw new RuntimeException("No puedes transferirte a ti mismo");
        }

        // Validar saldo del emisor llamando a WalletService (comunicación síncrona)
        validateBalance(senderId, request.getAmount());

        Transaction transaction = Transaction.builder()
                .senderId(senderId)
                .receiverId(request.getReceiverId())
                .amount(request.getAmount())
                .type(TransactionType.TRANSFER)
                .status("COMPLETED") // In a real system, this might start as PENDING
                .description(request.getDescription())
                .build();

        Transaction saved = transactionRepository.save(transaction);

        emitTransactionEvent(saved);

        return saved;
    }

    private void validateBalance(Long userId, BigDecimal amount) {
        try {
            String url = "http://wallet-service:8082/wallets/" + userId;
            WalletDto wallet = restTemplate.getForObject(url, WalletDto.class);

            if (wallet == null) {
                throw new RuntimeException("Billetera no encontrada para el usuario: " + userId);
            }

            if (wallet.getBalance().compareTo(amount) < 0) {
                throw new RuntimeException(
                        "Saldo insuficiente. Tienes: " + wallet.getBalance() + ", intentas enviar: " + amount);
            }
        } catch (org.springframework.web.client.RestClientException e) {
            log.error("Error validando saldo con WalletService", e);
            throw new RuntimeException("Error verificando saldo: " + e.getMessage());
        }
    }

    private void emitTransactionEvent(Transaction transaction) {
        TransactionCompletedEvent event = TransactionCompletedEvent.builder()
                .transactionId(transaction.getId())
                .senderId(transaction.getSenderId())
                .receiverId(transaction.getReceiverId())
                .amount(transaction.getAmount())
                .type(transaction.getType().name())
                .build();

        log.info("Emitting transaction completed event for ID: {}", transaction.getId());
        kafkaTemplate.send("transaction.events", event);
    }

    private void auditTransaction(Transaction transaction) {
        TransactionAudit audit = TransactionAudit.builder()
                .transactionId(transaction.getId())
                .senderId(transaction.getSenderId())
                .receiverId(transaction.getReceiverId())
                .amount(transaction.getAmount())
                .type(transaction.getType().name())
                .status(transaction.getStatus())
                .description(transaction.getDescription())
                .timestamp(LocalDateTime.now())
                .auditRole("SYSTEM_AUDIT")
                .build();

        auditRepository.save(audit);
        log.info("Transaction audited in MongoDB for ID: {}", transaction.getId());
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    private static class WalletDto {
        private Long id;
        private Long userId;
        private BigDecimal balance;
        private String currency;
    }
}
