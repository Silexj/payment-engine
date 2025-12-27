package com.github.Silexj.payment_engine.dto.event;

import java.time.LocalDateTime;

public record AccountCreatedEvent(
        Long accountId,
        String number,
        String currency,
        LocalDateTime createdAt
) {
}
