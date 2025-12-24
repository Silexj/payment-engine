package com.github.Silexj.payment_engine.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.Silexj.payment_engine.dto.AccountDto;
import com.github.Silexj.payment_engine.service.AccountService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountController.class)
public class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountService accountService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldReturn201_WhenCreatingAccount() throws Exception {
        var request = new AccountDto.CreateRequest("RUB");
        var response = new AccountDto.Response(1L, "123", BigDecimal.ZERO, "RUB");

        Mockito.when(accountService.createAccount(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/accounts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.currency").value("RUB"));
    }

    @Test
    void shouldReturn400_WhenCurrencyIsBland() throws Exception {
        var request = new AccountDto.CreateRequest("");

        mockMvc.perform(post("/api/v1/accounts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Error"));

    }

}
