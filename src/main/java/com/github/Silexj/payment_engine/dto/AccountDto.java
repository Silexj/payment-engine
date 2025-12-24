package com.github.Silexj.payment_engine.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public class AccountDto {
    public record CreateRequest (
            @NotBlank(message = "Currency code is required")
            String currency
    ) {}

    public record Response(
            Long id,
            String number,
            BigDecimal balance,
            String currency
    ) {}

    public record TopUpRequest (
            @NotNull(message = "Account ID is required")
            Long accountId,

            @NotNull(message = "Amount is required")
            @Positive(message = "Amount must be greater than zero")
            BigDecimal amount
    ) {}
}
