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
        log.info("[TX-SRV] 📥 KAFKA-RECV: Sincronizando caché desde 'wallet.balance.updated'");

        if (event.getNewBalance() != null) {
            redisTemplate.opsForValue().set(
                    BALANCE_CACHE_KEY + event.getUserId(),
                    event.getNewBalance().toString(),
                    Duration.ofMinutes(10));
            log.info("[TX-SRV] ⚡ REDIS: Caché actualizada para User {}", event.getUserId());
        }
    }

    // BORRA LA CLASE INTERNA BalanceUpdatedEvent QUE TENÍAS AQUÍ ABAJO
}