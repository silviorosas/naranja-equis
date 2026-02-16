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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.client.RestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;

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
    void processTransfer_Successful_WithCacheHit() {
        Long senderId = 1L;
        Long receiverId = 2L;
        BigDecimal amount = new BigDecimal("100.00");
        TransactionRequest request = new TransactionRequest();
        request.setReceiverId(receiverId);
        request.setAmount(amount);
        request.setDescription("Test transfer");

        when(valueOperations.get("wallet_balance:" + senderId)).thenReturn("500.00");

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
        verify(valueOperations, times(1)).get("wallet_balance:" + senderId);
    }

    @Test
    void processTransfer_Successful_WithCacheMiss() {
        Long senderId = 1L;
        BigDecimal amount = new BigDecimal("100.00");
        TransactionRequest request = new TransactionRequest();
        request.setReceiverId(2L);
        request.setAmount(amount);

        when(valueOperations.get("wallet_balance:" + senderId)).thenReturn(null);

        TransactionService.WalletDto walletDto = new TransactionService.WalletDto();
        walletDto.setBalance(new BigDecimal("500.00"));
        ApiResponse<TransactionService.WalletDto> apiResponse = ApiResponse.success(walletDto);

        ResponseEntity<ApiResponse<TransactionService.WalletDto>> responseEntity = ResponseEntity.ok(apiResponse);

        when(restTemplate.exchange(anyString(), any(), any(), any(ParameterizedTypeReference.class)))
                .thenReturn(responseEntity);

        Transaction transaction = Transaction.builder().id(101L).senderId(senderId).receiverId(2L).amount(amount)
                .type(TransactionType.TRANSFER).status("COMPLETED").build();
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);

        Transaction result = transactionService.processTransfer(senderId, request);

        assertNotNull(result);
        verify(valueOperations, times(1)).set(eq("wallet_balance:" + senderId), anyString(), any(), any());
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
        verify(auditRepository, times(1)).save(any());
    }

    @Test
    void validateBalance_RedisError_FallsBackToRest() {
        Long userId = 1L;
        BigDecimal amount = new BigDecimal("100.00");

        when(valueOperations.get(anyString())).thenThrow(new RuntimeException("Redis error"));

        TransactionService.WalletDto walletDto = new TransactionService.WalletDto();
        walletDto.setBalance(new BigDecimal("500.00"));
        ResponseEntity<ApiResponse<TransactionService.WalletDto>> responseEntity = ResponseEntity
                .ok(ApiResponse.success(walletDto));

        when(restTemplate.exchange(anyString(), any(), any(), any(ParameterizedTypeReference.class)))
                .thenReturn(responseEntity);

        assertDoesNotThrow(() -> transactionService.validateBalance(userId, amount));
    }

    @Test
    void validateBalance_RestError_ThrowsException() {
        Long userId = 1L;
        BigDecimal amount = new BigDecimal("100.00");

        when(valueOperations.get(anyString())).thenReturn(null);
        when(restTemplate.exchange(anyString(), any(), any(), any(ParameterizedTypeReference.class)))
                .thenThrow(new RuntimeException("Service down"));

        assertThrows(RuntimeException.class, () -> transactionService.validateBalance(userId, amount));
    }

    @Test
    void fallbackValidateBalance_CacheHit() {
        Long userId = 1L;
        BigDecimal amount = new BigDecimal("100.00");
        when(valueOperations.get("wallet_balance:" + userId)).thenReturn("500.00");

        assertDoesNotThrow(
                () -> transactionService.fallbackValidateBalance(userId, amount, new RuntimeException("Service down")));
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
