package com.github.Silexj.payment_engine.dto.event;

import java.time.LocalDateTime;

/**
 * Событие открытия нового банковского счета.
 * Генерируется после успешного сохранения счета с уникальным номером.
 */
public record AccountCreatedEvent(
        Long accountId,
        String number,
        String currency,
        LocalDateTime createdAt
) {
}
