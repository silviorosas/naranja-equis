package com.naranjax.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionCompletedEvent {
    private Long transactionId;
    private Long senderId;
    private String senderName;
    private String senderEmail;
    private String senderCvu;
    private String senderAlias;
    private Long receiverId;
    private String receiverName;
    private String receiverEmail;
    private String receiverCvu;
    private String receiverAlias;
    private BigDecimal amount;
    private String type; // DEPOSIT, TRANSFER
    private LocalDateTime timestamp;
}
