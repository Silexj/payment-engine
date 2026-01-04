package com.github.Silexj.payment_engine.controller;

import com.github.Silexj.payment_engine.dto.TransferDTO;
import com.github.Silexj.payment_engine.service.TransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/transfers")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

    /**
     * Инициирует операцию перевода средств между счетами.
     * Метод является идемпотентным: если клиент отправит повторный запрос
     * с тем же externalId (например, при retry после сбоя сети),
     * система вернет результат ранее сохраненной транзакции без повторного списания средств.
     *
     * Возвращает статус 200 OK< как для новой, так и для идемпотентно возвращенной транзакции.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public TransferDTO.Response performTransfer(@RequestBody @Valid TransferDTO.PerformRequest request) {
        return transferService.performTransfer(request);
    }
}
