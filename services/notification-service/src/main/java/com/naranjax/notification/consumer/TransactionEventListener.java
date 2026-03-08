package com.naranjax.notification.consumer;

import com.naranjax.common.event.TransactionCompletedEvent;
import com.naranjax.notification.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class TransactionEventListener {

    private final EmailService emailService;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final java.time.ZoneId ART_ZONE = java.time.ZoneId.of("America/Argentina/Buenos_Aires");

    @KafkaListener(topics = "transaction.events", groupId = "notification-service-group")
    public void handleTransactionCompleted(TransactionCompletedEvent event) {
        log.info("[PASO 4.5/5] [NOTIF-SRV] 📥 EVENTO OPTIMIZADO: Transacción ID: {} ({} -> {})",
                event.getTransactionId(), event.getSenderEmail(), event.getReceiverEmail());

        try {
            String humanType = "TRANSFER".equals(event.getType()) ? "Transferencia" : "Depósito";

            // Forzar Fecha y Hora en zona horaria de Argentina (ART - UTC-3)
            LocalDateTime nowArt = LocalDateTime.now(ART_ZONE);
            String formattedDate = event.getTimestamp() != null
                    ? event.getTimestamp().atZone(java.time.ZoneOffset.UTC).withZoneSameInstant(ART_ZONE)
                            .format(formatter)
                    : nowArt.format(formatter);

            // 1. Notificar al remitente (si no es el sistema)
            if (event.getSenderId() != null && event.getSenderId() != 0) {
                Map<String, Object> variables = new HashMap<>();
                variables.put("userName", event.getSenderName());
                variables.put("senderName", event.getSenderName());
                variables.put("senderCbu", event.getSenderCvu());
                variables.put("amount", event.getAmount());
                variables.put("type", humanType);
                variables.put("transactionId", event.getTransactionId());
                variables.put("status", "EXITOSA");
                variables.put("date", formattedDate);
                variables.put("receiverName", event.getReceiverName());
                variables.put("receiverCbu", event.getReceiverCvu());
                variables.put("receiverAlias", event.getReceiverAlias());
                variables.put("senderAlias", event.getSenderAlias());
                variables.put("timestamp", formattedDate);

                String subject = "Naranja Equis - Comprobante de " + humanType;
                emailService.sendHtmlEmail(event.getSenderId(), event.getSenderEmail(), event.getSenderName(), subject,
                        "transaction-email", variables);

                // Delay estratégico aumentado a 10s para evitar saturar Mailtrap (550 Too many
                // emails)
                if ("TRANSFER".equals(event.getType())) {
                    log.info(
                            "[NOTIF-SRV] ⏳ Esperando 10s para enviar notificación al receptor (Estrategia Anti-Bloqueo 2.0)...");
                    Thread.sleep(10000);
                }
            }

            // 2. Si es transferencia, notificar al receptor
            if ("TRANSFER".equals(event.getType()) && event.getReceiverId() != null) {
                Map<String, Object> recVariables = new HashMap<>();
                recVariables.put("userName", event.getReceiverName());
                recVariables.put("amount", event.getAmount());
                recVariables.put("senderName", event.getSenderName());
                recVariables.put("senderCbu", event.getSenderCvu());
                recVariables.put("senderAlias", event.getSenderAlias());
                recVariables.put("receiverName", event.getReceiverName());
                recVariables.put("receiverCbu", event.getReceiverCvu());
                recVariables.put("receiverAlias", event.getReceiverAlias());
                recVariables.put("transactionId", event.getTransactionId());
                recVariables.put("date", formattedDate);
                recVariables.put("timestamp", formattedDate);

                emailService.sendHtmlEmail(event.getReceiverId(), event.getReceiverEmail(), event.getReceiverName(),
                        "¡Recibiste dinero en Naranja Equis!", "receiving-transfer-email", recVariables);
            }

        } catch (InterruptedException e) {
            log.error("[NOTIF-SRV] ❌ Proceso interrumpido inesperadamente: {}", e.getMessage());
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("[NOTIF-SRV] ❌ Error enviando notificaciones: {}", e.getMessage());
        }
    }
}
