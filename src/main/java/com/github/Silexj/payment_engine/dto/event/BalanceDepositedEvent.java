package com.github.Silexj.payment_engine.dto.event;

import java.math.BigDecimal;
import java.util.UUID;

public record BalanceDepositedEvent(
        Long accountId,
        BigDecimal amount,
        String currency,
        UUID transactionId
) {
}
