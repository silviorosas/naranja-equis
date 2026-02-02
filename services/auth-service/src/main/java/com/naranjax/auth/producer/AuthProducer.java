package com.naranjax.auth.producer;

import com.naranjax.common.event.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String TOPIC = "user.registered";

    public void sendUserRegisteredEvent(UserRegisteredEvent event) {
        log.info("Sending user registered event for user: {}", event.getEmail());
        kafkaTemplate.send(TOPIC, String.valueOf(event.getUserId()), event);
    }
}
