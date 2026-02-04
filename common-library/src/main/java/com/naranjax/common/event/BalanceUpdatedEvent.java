package com.naranjax.common.event;

import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BalanceUpdatedEvent {
    private Long userId;
    private BigDecimal newBalance;
}