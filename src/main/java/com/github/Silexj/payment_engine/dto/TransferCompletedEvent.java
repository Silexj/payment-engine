package com.github.Silexj.payment_engine.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferCompletedEvent(
        UUID transactionId,
        Long senderAccountId,
        Long receiverAccountId,
        BigDecimal amount,
        String currency
) {
}
