package com.naranjax.wallet.consumer;

import com.naranjax.common.event.TransactionCompletedEvent;
import com.naranjax.common.event.UserRegisteredEvent;
import com.naranjax.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class WalletConsumer {

    private final WalletService walletService;

    @KafkaListener(topics = "user.registered", groupId = "wallet-service-group")
    public void consumeUserRegistered(UserRegisteredEvent event) {
        log.info("[WLT-SRV] 📥 KAFKA-RECV: Nuevo usuario registrado (ID: {})", event.getUserId());
        try {
            walletService.createWallet(event.getUserId(), event.getEmail());
        } catch (Exception e) {
            log.error("Error creating wallet for user {}: {}", event.getUserId(), e.getMessage());
        }
    }

    @KafkaListener(topics = "transaction.events", groupId = "wallet-service-group")
    public void consumeTransactionCompleted(TransactionCompletedEvent event) {
        log.info("[WLT-SRV] 📥 KAFKA-RECV: Transacción completada recibida (ID: {})", event.getTransactionId());

        try {
            if ("DEPOSIT".equals(event.getType())) {
                walletService.updateBalance(event.getReceiverId(), event.getAmount(), "DEPOSIT");
            } else if ("TRANSFER".equals(event.getType())) {
                // Actualizar emisor (egreso)
                walletService.updateBalance(event.getSenderId(), event.getAmount(), "TRANSFER_OUT");
                // Actualizar receptor (ingreso)
                walletService.updateBalance(event.getReceiverId(), event.getAmount(), "TRANSFER_IN");
            }
        } catch (Exception e) {
            log.error("Error updating balances for transaction {}: {}",
                    event.getTransactionId(), e.getMessage());
            // En un sistema real, aquí gatillaríamos eventos de compensación / Sagas
        }
    }
}
