package com.naranjax.transaction.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransactionRequest {
    @NotNull(message = "El ID del receptor es obligatorio")
    private Long receiverId;

    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "1.0", message = "El monto mínimo es 1.0")
    private BigDecimal amount;

    private String description;
}
