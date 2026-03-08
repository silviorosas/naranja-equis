package com.naranjax.transaction.service;

import com.naranjax.common.dto.ApiResponse;
import com.naranjax.transaction.dto.TransactionRequest;
import com.naranjax.transaction.entity.Transaction;
import com.naranjax.transaction.entity.TransactionType;
import com.naranjax.transaction.exception.InsufficientFundsException;
import com.naranjax.transaction.repository.TransactionAuditRepository;
import com.naranjax.transaction.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.client.RestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpMethod;
import com.naranjax.common.dto.UserDto;
import com.naranjax.common.event.TransactionCompletedEvent;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransactionAuditRepository auditRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @SuppressWarnings("unchecked")
    void processTransfer_Successful_WithCacheHit() {
        Long senderId = 1L;
        Long receiverId = 2L;
        BigDecimal amount = new BigDecimal("100.00");
        TransactionRequest request = new TransactionRequest();
        request.setReceiverId(receiverId);
        request.setAmount(amount);
        request.setDescription("Test transfer");

        // 1. Mock Balance (String)
        when(valueOperations.get(ArgumentMatchers.contains("wallet_balance"))).thenReturn("500.00");

        // 2. Mock Identidades (UserDto) - Evita ClassCastException
        UserDto sender = UserDto.builder().id(senderId).firstName("Juan").lastName("Perez").email("juan@test.com")
                .build();
        UserDto receiver = UserDto.builder().id(receiverId).firstName("Maria").lastName("Gomez").email("maria@test.com")
                .build();
        when(valueOperations.get(ArgumentMatchers.contains("user_identity:1"))).thenReturn(sender);
        when(valueOperations.get(ArgumentMatchers.contains("user_identity:2"))).thenReturn(receiver);

        // 3. Mock Wallet data (REST) para Alias/CVU
        TransactionService.WalletDto walletDto = new TransactionService.WalletDto();
        walletDto.setCvu("CVU123");
        walletDto.setAlias("juan.equis");
        ResponseEntity<ApiResponse<TransactionService.WalletDto>> walletResponse = ResponseEntity
                .ok(ApiResponse.success(walletDto));

        lenient().when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(),
                any(ParameterizedTypeReference.class)))
                .thenReturn(walletResponse);

        Transaction transaction = Transaction.builder()
                .id(100L)
                .senderId(senderId)
                .receiverId(receiverId)
                .amount(amount)
                .type(TransactionType.TRANSFER)
                .status("COMPLETED")
                .build();

        when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);

        Transaction result = transactionService.processTransfer(senderId, request);

        assertNotNull(result);
        // Verificamos las 3 llamadas a Redis: 1 Saldo, 1 Emisor, 1 Receptor
        verify(valueOperations, times(3)).get(anyString());

        // Verificamos Enriquecimiento de Eventos
        ArgumentCaptor<TransactionCompletedEvent> eventCaptor = ArgumentCaptor.forClass(TransactionCompletedEvent.class);
        verify(kafkaTemplate).send(eq("transaction.events"), eventCaptor.capture());
        TransactionCompletedEvent capturedEvent = eventCaptor.getValue();

        assertEquals("Juan Perez", capturedEvent.getSenderName());
        assertEquals("Maria Gomez", capturedEvent.getReceiverName());
        assertEquals("juan.equis", capturedEvent.getSenderAlias());
    }

    @Test
    @SuppressWarnings("unchecked")
    void processTransfer_Successful_WithCacheMiss() {
        Long senderId = 3L;
        Long receiverId = 4L;
        BigDecimal amount = new BigDecimal("100.00");
        TransactionRequest request = new TransactionRequest();
        request.setReceiverId(receiverId);
        request.setAmount(amount);

        when(valueOperations.get(anyString())).thenReturn(null);

        // Mock Wallet REST (Balance + Alias)
        TransactionService.WalletDto walletDto = new TransactionService.WalletDto();
        walletDto.setBalance(new BigDecimal("500.00"));
        walletDto.setAlias("user.miss");
        ResponseEntity<ApiResponse<TransactionService.WalletDto>> walletResponse = ResponseEntity
                .ok(ApiResponse.success(walletDto));

        // Mock Identity REST
        UserDto userMiss = UserDto.builder().id(senderId).firstName("Miss").lastName("User").build();
        ResponseEntity<ApiResponse<UserDto>> userResponse = ResponseEntity.ok(ApiResponse.success(userMiss));

        // Stubbing condicional por tipo de retorno esperado
        doReturn(walletResponse).when(restTemplate).exchange(contains("wallets"), any(), any(),
                any(ParameterizedTypeReference.class));
        doReturn(userResponse).when(restTemplate).exchange(contains("auth/users"), any(), any(),
                any(ParameterizedTypeReference.class));

        Transaction transaction = Transaction.builder().id(101L).senderId(senderId).receiverId(receiverId).amount(amount)
                .type(TransactionType.TRANSFER).status("COMPLETED").build();
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);

        Transaction result = transactionService.processTransfer(senderId, request);

        assertNotNull(result);
        // Verifica que se intentó guardar en caché tras el miss
        verify(valueOperations, atLeastOnce()).set(anyString(), any(), any(Duration.class));
    }

    @Test
    void processDeposit_Successful() {
        Long userId = 1L;
        BigDecimal amount = new BigDecimal("50.00");
        Transaction transaction = Transaction.builder().id(102L).senderId(0L).receiverId(userId).amount(amount)
                .type(TransactionType.DEPOSIT).status("COMPLETED").build();
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);

        Transaction result = transactionService.processDeposit(userId, amount);

        assertNotNull(result);
        verify(auditRepository).save(any(com.naranjax.transaction.entity.TransactionAudit.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void validateBalance_RedisError_FallsBackToRest() {
        Long userId = 1L;
        BigDecimal amount = new BigDecimal("100.00");

        when(valueOperations.get(anyString())).thenThrow(new RuntimeException("Redis error"));

        TransactionService.WalletDto walletDto = new TransactionService.WalletDto();
        walletDto.setBalance(new BigDecimal("500.00"));
        ResponseEntity<ApiResponse<TransactionService.WalletDto>> responseEntity = ResponseEntity
                .ok(ApiResponse.success(walletDto));

        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(), any(ParameterizedTypeReference.class)))
                .thenReturn(responseEntity);

        assertDoesNotThrow(() -> transactionService.validateBalance(userId, amount));
    }

    @Test
    @SuppressWarnings("unchecked")
    void validateBalance_RestError_ThrowsException() {
        Long userId = 1L;
        BigDecimal amount = new BigDecimal("100.00");

        when(valueOperations.get(anyString())).thenReturn(null);
        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(), any(ParameterizedTypeReference.class)))
                .thenThrow(new RuntimeException("Service down"));

        assertThrows(RuntimeException.class, () -> transactionService.validateBalance(userId, amount));
    }

    @Test
    void fallbackValidateBalance_CacheHit() {
        Long userId = 1L;
        BigDecimal amount = new BigDecimal("100.00");
        when(valueOperations.get(anyString())).thenReturn("500.00");

        // El log "NUEVO Cache-Aside: Fallback activado" ocurre aquí al llamar al
        // fallback
        assertDoesNotThrow(
                () -> transactionService.fallbackValidateBalance(userId, amount, new RuntimeException("Service down")));
        verify(valueOperations).get(ArgumentMatchers.contains("wallet_balance"));
    }

    @Test
    void processTransfer_WithRedisError_FallbackEnabled() {
        Long userId = 1L;
        BigDecimal amount = new BigDecimal("100.00");
        when(valueOperations.get(anyString())).thenReturn("500.00");

        // Verificamos que el fallback cumpla con la lógica de negocio v3.2
        assertDoesNotThrow(
                () -> transactionService.fallbackValidateBalance(userId, amount, new RuntimeException("Redis Down")));
    }

    @Test
    void fallbackValidateBalance_CacheMiss_ThrowsException() {
        Long userId = 1L;
        BigDecimal amount = new BigDecimal("100.00");
        when(valueOperations.get(anyString())).thenReturn(null);

        assertThrows(RuntimeException.class,
                () -> transactionService.fallbackValidateBalance(userId, amount, new RuntimeException("Service down")));
    }

    @Test
    void fallbackValidateBalance_InsufficientFunds() {
        Long userId = 1L;
        BigDecimal amount = new BigDecimal("1000.00");
        when(valueOperations.get(anyString())).thenReturn("500.00");

        assertThrows(InsufficientFundsException.class,
                () -> transactionService.fallbackValidateBalance(userId, amount, new RuntimeException("Service down")));
    }
}
