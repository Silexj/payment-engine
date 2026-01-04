package com.github.Silexj.payment_engine.dto.event;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Событие пополнения баланса из внешнего источника.
 * Сигнализирует о поступлении новых средств в систему.
 * Используется для уведомления владельца счета о зачислении.
 */
public record BalanceDepositedEvent(
        Long accountId,
        BigDecimal amount,
        String currency,
        UUID transactionId
) {
}
