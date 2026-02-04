package com.naranjax.transaction.consumer;

import com.naranjax.common.event.BalanceUpdatedEvent; // <--- USA EL DE LA LIBRERIA
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import java.time.Duration;

@Service
@Slf4j
@RequiredArgsConstructor
public class WalletBalanceConsumer {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String BALANCE_CACHE_KEY = "wallet_balance:";

    @KafkaListener(topics = "wallet.balance.updated", groupId = "transaction-service-group")
    public void handleBalanceUpdate(BalanceUpdatedEvent event) {
        // Asegúrate de usar el nombre de método que tenga la clase en COMMON
        // Probablemente es event.getNewBalance()
        log.info("Actualizando caché de Redis para usuario: {} - Nuevo Saldo: {}",
                event.getUserId(), event.getNewBalance());

        if (event.getNewBalance() != null) {
            redisTemplate.opsForValue().set(
                    BALANCE_CACHE_KEY + event.getUserId(),
                    event.getNewBalance().toString(),
                    Duration.ofMinutes(10)
            );
        }
    }

    // BORRA LA CLASE INTERNA BalanceUpdatedEvent QUE TENÍAS AQUÍ ABAJO
}