package com.naranjax.notification.service;

import com.naranjax.common.dto.UserDto;
import com.naranjax.common.event.TransactionCompletedEvent;
import com.naranjax.notification.repository.NotificationRepository;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private TemplateEngine templateEngine;

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private EmailService emailService;

    @Test
    void sendHtmlEmail_Success() {
        // [TEST-LOG] Iniciando test de envío de email exitoso
        Long userId = 1L;
        String to = "test@naranjax.com";
        Map<String, Object> variables = new HashMap<>();
        variables.put("transactionId", 123L);

        when(mailSender.createMimeMessage()).thenReturn(mock(MimeMessage.class));
        when(templateEngine.process(anyString(), any(Context.class))).thenReturn("<html></html>");

        assertDoesNotThrow(() -> emailService.sendHtmlEmail(userId, to, "User", "Subject", variables));

        verify(mailSender).send(any(MimeMessage.class));
        verify(notificationRepository).save(any());
        System.out.println("[TEST-LOG] ✅ Email enviado y auditado exitosamente.");
    }

    @Test
    void fallbackSendEmail_Execution() {
        // [TEST-LOG] Iniciando test de ejecución de Fallback de EmailService
        Long userId = 1L;
        String to = "fail@test.com";
        Map<String, Object> variables = new HashMap<>();
        
        emailService.fallbackSendEmail(userId, to, "FailUser", "FailSubject", variables, new RuntimeException("Circuit Open"));

        verify(notificationRepository).save(any());
        System.out.println("[TEST-LOG] ✅ Fallback de EmailService auditado correctamente.");
    }
}
