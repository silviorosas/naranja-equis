package com.naranjax.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionCompletedEvent {
    private Long transactionId;
    private Long senderId;
    private Long receiverId;
    private BigDecimal amount;
    private String type; // DEPOSIT, TRANSFER
}
