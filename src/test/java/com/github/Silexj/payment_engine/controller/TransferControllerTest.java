package com.github.Silexj.payment_engine.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.Silexj.payment_engine.dto.TransferDTO;
import com.github.Silexj.payment_engine.model.TransactionStatus;
import com.github.Silexj.payment_engine.service.TransferService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TransferController.class)
public class TransferControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TransferService transferService;

    @Test
    @DisplayName("Should return 200 OK when transfer request is valid")
    void shouldPerformTransferSuccessfully() throws Exception {
        UUID externalId = UUID.randomUUID();
        TransferDTO.PerformRequest request = new TransferDTO.PerformRequest(
                externalId, 1L, 2L, new BigDecimal("100.00")
        );

        TransferDTO.Response mockResponse = new TransferDTO.Response(
                UUID.randomUUID(), externalId, 1L, 2L, new BigDecimal("100.00"),
                "RUB", TransactionStatus.SUCCESS, LocalDateTime.now(), null
        );
        Mockito.when(transferService.performTransfer(any())).thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.amount").value(100.00));
        verify(transferService).performTransfer(any());
    }

    @Test
    @DisplayName("Should return 400 Bad Request when validation fails (negative amount)")
    void shouldFailValidation() throws Exception {
        // Given (Невалидный запрос: отрицательная сумма и нет externalId)
        TransferDTO.PerformRequest invalidRequest = new TransferDTO.PerformRequest(
                null, 1L, 2L, new BigDecimal("-50.00")
        );

        // When & Then
        mockMvc.perform(post("/api/v1/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest()) // Ждем 400
                .andExpect(jsonPath("$.title").value("Validation Error"));

        verifyNoInteractions(transferService);
    }

    @Test
    @DisplayName("Should return 400 Bad Request when business exception occurs (insufficient funds)")
    void shouldHandleBusinessException() throws Exception {
        // Given
        TransferDTO.PerformRequest request = new TransferDTO.PerformRequest(
                UUID.randomUUID(), 1L, 2L, BigDecimal.TEN
        );

        // Учим мок кидать ошибку (как будто денег нет)
        Mockito.when(transferService.performTransfer(any()))
                .thenThrow(new IllegalArgumentException("Insufficient funds"));

        // When & Then
        mockMvc.perform(post("/api/v1/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest()) // 400
                .andExpect(jsonPath("$.detail").value("Insufficient funds")); // Сообщение ошибки
    }
}
