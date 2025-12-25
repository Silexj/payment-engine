package com.github.Silexj.payment_engine.dto;

import com.github.Silexj.payment_engine.model.TransactionStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class TransferDTO {

    /**
     * Запрос на перевод денег.
     * Требуем обязательно все поля кроме описания.
     */
    public record PerformRequest(
                @NotNull(message = "Idempotency key is required (externalId)")
                UUID externalId,

                @NotNull(message = "Sender account ID is required")
                Long fromAccountId,

                @NotNull(message = "Receiver account ID is required")
                Long toAccountId,

                @NotNull(message = "Amount is required")
                @Positive(message = "Transfer amount must be positive")
                BigDecimal amount
    ) {}

    /**
     * Ответ сервиса перевода.
     * Возвращаем статус и финальные данные транзакции
     */
    public record Response(
            UUID transactionId,
            UUID externalId,
            Long senderId,
            Long receiverId,
            BigDecimal amount,
            String currency,
            TransactionStatus status,
            LocalDateTime timestamp,
            String errorMessage
    ) {}


}
