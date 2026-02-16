package com.naranjax.transaction.service;

import com.naranjax.common.event.TransactionCompletedEvent;
import com.naranjax.transaction.dto.TransactionRequest;
import com.naranjax.transaction.entity.Transaction;
import com.naranjax.transaction.entity.TransactionAudit;
import com.naranjax.transaction.entity.TransactionType;
import com.naranjax.transaction.exception.InsufficientFundsException;
import com.naranjax.transaction.repository.TransactionAuditRepository;
import com.naranjax.transaction.repository.TransactionRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import com.naranjax.common.dto.ApiResponse;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final TransactionAuditRepository auditRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private final RestTemplate restTemplate;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String BALANCE_CACHE_KEY = "wallet_balance:";

    @Transactional
    public Transaction processDeposit(Long userId, BigDecimal amount) {
        log.info("[PASO 1/5] [TX-SRV] 📥 RECIBIENDO SOLICITUD: Depósito para User {} ($ {})", userId, amount);
        Transaction transaction = Transaction.builder()
                .senderId(0L)
                .receiverId(userId)
                .amount(amount)
                .type(TransactionType.DEPOSIT)
                .status("COMPLETED")
                .description("Deposito de saldo")
                .build();

        Transaction saved = transactionRepository.save(transaction);
        log.info("[PASO 3/5] [TX-SRV] ✅ MYSQL/MONGO: Transacción persistida y Auditada.");
        auditTransaction(saved);
        emitTransactionEvent(saved);
        return saved;
    }

    @Transactional
    public Transaction processTransfer(Long senderId, TransactionRequest request) {
        log.info("[PASO 1/5] [TX-SRV] 📥 RECIBIENDO SOLICITUD: Transferencia de User {} a User {} ($ {})",
                senderId, request.getReceiverId(), request.getAmount());

        if (senderId.equals(request.getReceiverId())) {
            throw new IllegalArgumentException("No puedes transferirte a ti mismo");
        }

        validateBalance(senderId, request.getAmount());

        Transaction transaction = Transaction.builder()
                .senderId(senderId)
                .receiverId(request.getReceiverId())
                .amount(request.getAmount())
                .type(TransactionType.TRANSFER)
                .status("COMPLETED")
                .description(request.getDescription())
                .build();

        Transaction saved = transactionRepository.save(transaction);
        log.info("[PASO 3/5] [TX-SRV] ✅ MYSQL/MONGO: Transacción persistida y Auditada.");

        // === NUEVO: Auditoría en MongoDB para transferencias ===
        auditTransaction(saved);

        emitTransactionEvent(saved);
        return saved;
    }

    // CAMBIO: Se cambió a 'public' para que el proxy de AOP de Resilience4j
    // funcione correctamente
    @CircuitBreaker(name = "walletServiceCB", fallbackMethod = "fallbackValidateBalance")
    public void validateBalance(Long userId, BigDecimal amount) {
        String cacheKey = BALANCE_CACHE_KEY + userId;
        BigDecimal balance = null;
        boolean isFromCache = false; // NUEVA BANDERA

        try {
            String cachedBalance = (String) redisTemplate.opsForValue().get(cacheKey);
            if (cachedBalance != null) {
                log.info("[PASO 2/5] [TX-SRV] ⚡ REDIS: Cache Hit (Saldo: $ {})", cachedBalance);
                balance = new BigDecimal(cachedBalance);
                isFromCache = true; // Marcamos que ya lo tenemos
            }
        } catch (Exception e) {
            log.warn("NUEVO Cache-Aside: Error al leer de Redis... {}", e.getMessage());
        }

        if (balance == null) {
            String url = "http://wallet-service:8082/wallets/" + userId;
            log.info("[PASO 2/5] [TX-SRV] ⚡ REDIS: Cache Miss -> Consultando Wallet Service...");

            try {
                // Usamos una respuesta genérica para mapear el ApiResponse<WalletDto>
                ParameterizedTypeReference<ApiResponse<WalletDto>> responseType = new ParameterizedTypeReference<>() {
                };

                ApiResponse<WalletDto> response = restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        null,
                        responseType).getBody();

                if (response == null || !response.isSuccess() || response.getData() == null) {
                    throw new RuntimeException("Billetera no encontrada para el usuario: " + userId);
                }

                balance = response.getData().getBalance();
            } catch (Exception e) {
                log.error("Error al consultar Wallet Service: {}", e.getMessage());
                throw new RuntimeException("Error al validar saldo: " + e.getMessage());
            }
        }

        // NUEVO: Solo actualizamos Redis si NO vino de la caché (evitamos el SETEX
        // redundante)
        if (!isFromCache) {
            try {
                redisTemplate.opsForValue().set(cacheKey, balance.toString(), Duration.ofMinutes(10));
                log.info("[TX-SRV] ⚡ REDIS: Sincronizado tras consulta REST");
            } catch (Exception e) {
                log.warn("NUEVO Cache-Aside: Error al escribir en Redis");
            }
        }

        if (balance.compareTo(amount) < 0) {
            throw new InsufficientFundsException("Saldo insuficiente. Disponible: " + balance);
        }
    }

    // El Fallback se mantiene pero ahora es el último recurso (Nivel 3)
    public void fallbackValidateBalance(Long userId, BigDecimal amount, Throwable t) {
        log.warn("NUEVO Cache-Aside: Fallback activado por error: {}. Intentando última lectura de Redis para user: {}",
                t.getMessage(), userId);

        String cachedBalance = (String) redisTemplate.opsForValue().get(BALANCE_CACHE_KEY + userId);

        if (cachedBalance == null) {
            log.error("NUEVO Cache-Aside: Fallback fallido - Sin datos en caché ni servicio disponible.");
            throw new RuntimeException("Servicio no disponible y sin datos en caché. No se puede validar saldo.");
        }

        BigDecimal balance = new BigDecimal(cachedBalance);
        if (balance.compareTo(amount) < 0) {
            throw new InsufficientFundsException("Saldo insuficiente (Validado por caché de emergencia)");
        }
        log.info("[PASO 2/5] [TX-SRV] ⚡ REDIS: Cache Hit (EMERGENCIA) (Saldo: $ {})", balance);
    }

    public java.util.List<Transaction> getTransactionsByUser(Long userId) {
        return transactionRepository.findBySenderIdOrReceiverId(userId, userId);
    }

    private void emitTransactionEvent(Transaction transaction) {
        TransactionCompletedEvent event = TransactionCompletedEvent.builder()
                .transactionId(transaction.getId())
                .senderId(transaction.getSenderId())
                .receiverId(transaction.getReceiverId())
                .amount(transaction.getAmount())
                .type(transaction.getType().name())
                .build();

        log.info("==================== [KAFKA-EMIT] ====================");
        log.info("Enviando evento de transferencia -> Topic: transaction.events");
        log.info("======================================================");
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
        log.info("✅ Audit record saved in MongoDB for Transaction ID: {}", transaction.getId());
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