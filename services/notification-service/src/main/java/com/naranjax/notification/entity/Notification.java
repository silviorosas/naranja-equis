package com.naranjax.notification.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "notifications")
public class Notification {
    @Id
    private String id;
    private Long userId;
    private String recipientEmail;
    private String recipientName;
    private Long transactionId;
    private java.math.BigDecimal amount;
    private String subject;
    private String content;
    private String type;
    private LocalDateTime sentAt;
    private String status;
}
