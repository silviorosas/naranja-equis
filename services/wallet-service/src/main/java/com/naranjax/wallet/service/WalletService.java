package com.naranjax.wallet.service;

import com.naranjax.common.event.BalanceUpdatedEvent;
import com.naranjax.wallet.entity.Wallet;
import com.naranjax.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;

@Service
@Slf4j
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    public void createWallet(Long userId, String email) {
        if (walletRepository.findByUserId(userId).isPresent()) {
            log.warn("Wallet already exists for user: {}. Skipping creation.", userId);
            return;
        }

        log.info("Creating wallet for user: {}", userId);

        String cvu = "00000031" + (10000000000000L + (long) (secureRandom.nextDouble() * 90000000000000L));
        String alias = email.split("@")[0] + ".nx." + (secureRandom.nextInt(900) + 100);

        Wallet wallet = Wallet.builder()
                .userId(userId)
                .cvu(cvu)
                .alias(alias)
                .balance(BigDecimal.ZERO)
                .currency("ARS")
                .build();

        walletRepository.save(wallet);
        log.info("Wallet created successfully with CVU: {}", cvu);
    }

    @Transactional
    public void updateBalance(Long userId, BigDecimal amount, String type) {
        log.info("Updating balance for user: {}. Amount: {}. Type: {}", userId, amount, type);

        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Cartera no encontrada para el usuario: " + userId));

        if ("TRANSFER_OUT".equals(type) || "WITHDRAWAL".equals(type)) {
            if (wallet.getBalance().compareTo(amount) < 0) {
                throw new RuntimeException("Saldo insuficiente en la billetera de ID: " + userId);
            }
            wallet.setBalance(wallet.getBalance().subtract(amount));
        } else {
            // DEPOSIT or TRANSFER_IN
            wallet.setBalance(wallet.getBalance().add(amount));
        }

        walletRepository.save(wallet);
        log.info("Balance updated successfully for user: {}. New balance: {}", userId, wallet.getBalance());

        // Notificamos al mundo el nuevo saldo
        BalanceUpdatedEvent balanceEvent = BalanceUpdatedEvent.builder()
                .userId(userId)
                .newBalance(wallet.getBalance())
                .build();

        kafkaTemplate.send("wallet.balance.updated", userId.toString(), balanceEvent);
    }

    public java.util.Optional<Wallet> getWalletByUserId(Long userId) {
        return walletRepository.findByUserId(userId);
    }

    public java.util.Optional<Wallet> lookupWallet(String identifier) {
        return walletRepository.findByCvu(identifier)
                .or(() -> walletRepository.findByAlias(identifier));
    }
}
