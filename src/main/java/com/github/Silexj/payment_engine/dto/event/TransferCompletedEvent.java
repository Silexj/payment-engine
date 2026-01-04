package com.github.Silexj.payment_engine.dto.event;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Событие успешного завершения перевода средств между счетами.
 * Публикуется в Kafka после фиксации транзакции в БД.
 * Служит триггером для отправки уведомлений отправителю и получателю.
 */
public record TransferCompletedEvent(
        UUID transactionId,
        Long senderAccountId,
        Long receiverAccountId,
        BigDecimal amount,
        String currency
) {
}
