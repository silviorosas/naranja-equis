package com.naranjax.transaction.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Document(collection = "transaction_audit")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionAudit {
    @Id
    private String id;
    private Long transactionId;
    private Long senderId;
    private Long receiverId;
    private BigDecimal amount;
    private String type;
    private String status;
    private String description;
    private LocalDateTime timestamp;
    private String auditRole; // Para el rol @security-auditor
}
