package com.github.Silexj.payment_engine.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public class AccountDto {
    /**
     * Запрос на открытие нового банковского счета.
     * Требует обязательного указания валюты.
     */
    public record CreateRequest (
            @NotBlank(message = "Currency code is required")
            String currency
    ) {}

    /**
     * Публичное представление счета для клиентов API.
     * Скрывает внутренние поля БД (version, tech keys) и возвращает только бизнес-данные.
     */
    public record Response(
            Long id,
            String number,
            BigDecimal balance,
            String currency
    ) {}

    /**
     * Запрос на пополнение баланса (Deposit).
     * Сумма операции должна быть строго положительной.
     */
    public record TopUpRequest (
            @NotNull(message = "Account ID is required")
            Long accountId,

            @NotNull(message = "Amount is required")
            @Positive(message = "Amount must be greater than zero")
            BigDecimal amount
    ) {}
}
