package com.naranjax.wallet.service;

import com.naranjax.wallet.entity.Wallet;
import com.naranjax.wallet.repository.WalletRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private WalletService walletService;

    @Test
    void createWallet_Successful() {
        Long userId = 1L;
        String email = "test@naranjax.com";
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.empty());

        walletService.createWallet(userId, email);

        verify(walletRepository, times(1)).save(any(Wallet.class));
    }

    @Test
    void createWallet_AlreadyExists() {
        Long userId = 1L;
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(new Wallet()));

        walletService.createWallet(userId, "test@naranjax.com");

        verify(walletRepository, never()).save(any());
    }

    @Test
    void updateBalance_Deposit_Successful() {
        Long userId = 1L;
        BigDecimal amount = new BigDecimal("100.00");
        Wallet wallet = Wallet.builder().userId(userId).balance(new BigDecimal("500.00")).build();
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));

        walletService.updateBalance(userId, amount, "DEPOSIT");

        assertEquals(new BigDecimal("600.00"), wallet.getBalance());
        verify(walletRepository, times(1)).save(wallet);
        verify(kafkaTemplate, times(1)).send(eq("wallet.balance.updated"), anyString(), any());
    }

    @Test
    void updateBalance_TransferOut_Successful() {
        Long userId = 1L;
        BigDecimal amount = new BigDecimal("100.00");
        Wallet wallet = Wallet.builder().userId(userId).balance(new BigDecimal("500.00")).build();
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));

        walletService.updateBalance(userId, amount, "TRANSFER_OUT");

        assertEquals(new BigDecimal("400.00"), wallet.getBalance());
    }

    @Test
    void updateBalance_InsufficientFunds() {
        Long userId = 1L;
        BigDecimal amount = new BigDecimal("1000.00");
        Wallet wallet = Wallet.builder().userId(userId).balance(new BigDecimal("500.00")).build();
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));

        assertThrows(RuntimeException.class, () -> walletService.updateBalance(userId, amount, "TRANSFER_OUT"));
    }
}
