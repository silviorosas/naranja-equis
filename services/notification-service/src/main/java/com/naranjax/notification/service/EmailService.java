package com.naranjax.notification.service;

import com.naranjax.notification.entity.Notification;
import com.naranjax.notification.repository.NotificationRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final NotificationRepository notificationRepository;

    @CircuitBreaker(name = "emailService", fallbackMethod = "fallbackSendEmail")
    @Retry(name = "emailRetry")
    public void sendHtmlEmail(Long userId, String to, String name, String subject,
            Map<String, Object> variables) {
        log.info("[PASO 5/5] [NOTIF-SRV] 📧 Enviando Email a {} (Subject: {})", to, subject);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            Context context = new Context();
            context.setVariables(variables);
            String htmlContent = templateEngine.process("transaction-email", context);

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);

            saveAudit(userId, to, name, subject, htmlContent, "SENT", variables);
            log.info("[NOTIF-SRV] ✅ Email enviado y auditado correctamente.");

        } catch (Exception e) {
            log.error("[NOTIF-SRV] ❌ Error al enviar email: {}", e.getMessage());
            saveAudit(userId, to, name, subject, "Error: " + e.getMessage(), "FAILED", variables);
            throw new com.naranjax.common.exception.BusinessException("Error en envío de email: " + e.getMessage());
        }
    }

    public void fallbackSendEmail(Long userId, String to, String name, String subject,
            Map<String, Object> variables, Throwable t) {
        log.error("[NOTIF-SRV] 🛡️ FALLBACK: No se pudo enviar email a {}. Error: {}", to, t.getMessage());
        saveAudit(userId, to, name, subject, "FALLBACK: " + t.getMessage(), "FAILED_CIRCUIT_OPEN", variables);
    }

    private void saveAudit(Long userId, String to, String name, String subject, String content, String status,
            Map<String, Object> variables) {
        Notification notification = Notification.builder()
                .userId(userId)
                .recipientEmail(to)
                .recipientName(name)
                .transactionId((Long) variables.get("transactionId"))
                .amount((java.math.BigDecimal) variables.get("amount"))
                .subject(subject)
                .content(content)
                .type("TRANSACTION_EMAIL")
                .sentAt(LocalDateTime.now())
                .status(status)
                .build();
        notificationRepository.save(notification);
    }
}
