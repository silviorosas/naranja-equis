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
import com.naranjax.common.dto.UserDto;
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
    private static final String USER_CACHE_KEY = "user_identity:";

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
        UserDto sender = getUserData(transaction.getSenderId());
        UserDto receiver = getUserData(transaction.getReceiverId());
        WalletDto senderWallet = getWalletData(transaction.getSenderId());
        WalletDto receiverWallet = getWalletData(transaction.getReceiverId());

        String finalSenderName;
        if (transaction.getSenderId() == null || transaction.getSenderId() == 0L) {
            finalSenderName = "Sistema";
        } else {
            finalSenderName = (sender != null) ? sender.getFirstName() + " " + sender.getLastName()
                    : "Usuario Desconocido";
        }

        TransactionCompletedEvent event = TransactionCompletedEvent.builder()
                .transactionId(transaction.getId())
                .senderId(transaction.getSenderId())
                .senderName(finalSenderName)
                .senderEmail(sender != null ? sender.getEmail() : "no-reply@naranjax.com")
                .senderCvu(senderWallet != null ? senderWallet.getCvu() : "N/A")
                .senderAlias(senderWallet != null ? senderWallet.getAlias() : "N/A")
                .receiverId(transaction.getReceiverId())
                .receiverName(receiver != null ? receiver.getFirstName() + " " + receiver.getLastName()
                        : "Usuario Desconocido")
                .receiverEmail(receiver != null ? receiver.getEmail() : "no-reply@naranjax.com")
                .receiverCvu(receiverWallet != null ? receiverWallet.getCvu() : "N/A")
                .receiverAlias(receiverWallet != null ? receiverWallet.getAlias() : "N/A")
                .amount(transaction.getAmount())
                .type(transaction.getType().name())
                .timestamp(java.time.LocalDateTime.now())
                .build();

        log.info("[TX-SRV] 📦 Evento Final -> Emisor: {} | Receptor: {}",
                event.getSenderName(), event.getReceiverName());

        log.info("[PASO 4/5] [TX-SRV] 🚀 KAFKA-EMIT: Enviando evento enriquecido ({} -> {})",
                event.getSenderEmail(), event.getReceiverEmail());
        kafkaTemplate.send("transaction.events", event);
    }

    private UserDto getUserData(Long userId) {
        if (userId == null || userId == 0)
            return null;

        String cacheKey = USER_CACHE_KEY + userId;
        try {
            // 1. Intento recuperar de Redis (Cache-Aside) con deserialización JSON
            Object cachedUser = redisTemplate.opsForValue().get(cacheKey);
            if (cachedUser != null) {
                log.info("[TX-SRV] ⚡ CACHE-HIT: Recuperando identidad del usuario {} desde Redis", userId);
                return (UserDto) cachedUser;
            }

            // 2. Si no está en caché, buscamos en el microservicio
            log.info("[TX-SRV] 🔍 CACHE-MISS: Buscando datos del usuario {} en AUTH-SERVICE...", userId);
            String url = "http://auth-service/auth/users/" + userId;
            ApiResponse<UserDto> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<ApiResponse<UserDto>>() {
                    }).getBody();

            if (response != null && response.isSuccess() && response.getData() != null) {
                UserDto user = response.getData();
                log.info("[TX-SRV] ✅ Datos obtenidos para {}: {} {}", userId, user.getFirstName(), user.getLastName());

                // 3. Persistimos en Redis por 24 horas para evitar latencia futura
                redisTemplate.opsForValue().set(cacheKey, user, java.time.Duration.ofHours(24));
                return user;
            }
        } catch (Exception e) {
            log.warn("[TX-SRV] ⚠️ Error al obtener identidad de user {}: {}", userId, e.getMessage());
        }
        return null;
    }

    private WalletDto getWalletData(Long userId) {
        if (userId == null || userId == 0)
            return null;
        try {
            log.info("[TX-SRV] 🔍 Buscando billetera del usuario {} en WALLET-SERVICE...", userId);
            String url = "http://wallet-service/wallets/" + userId;
            ApiResponse<WalletDto> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<ApiResponse<WalletDto>>() {
                    }).getBody();

            if (response != null && response.isSuccess()) {
                log.info("[TX-SRV] ✅ Datos de billetera obtenidos para {}", userId);
                return response.getData();
            }
        } catch (Exception e) {
            log.warn("[TX-SRV] ⚠️ No se pudo obtener datos de la billetera del usuario {}: {}", userId, e.getMessage());
        }
        return null;
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
        log.info("[PASO 3/5] [TX-SRV] ✅ Persistencia completa (MySQL/MongoDB) para Transacción ID: {}",
                transaction.getId());
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    static class WalletDto {
        private Long id;
        private Long userId;
        private String cvu;
        private String alias;
        private BigDecimal balance;
        private String currency;
    }
}