package com.github.Silexj.payment_engine.service;

import com.github.Silexj.payment_engine.dto.AccountDto;
import com.github.Silexj.payment_engine.model.OutboxEvent;
import com.github.Silexj.payment_engine.repository.AccountRepository;
import com.github.Silexj.payment_engine.repository.OutboxEventRepository;
import com.github.Silexj.payment_engine.repository.TransactionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
public class OutboxIntegratedTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:latest");


    @Autowired
    private AccountService accountService;

    @Autowired
    private OutboxEventRepository outboxRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @AfterEach
    void cleanUp() {
        outboxRepository.deleteAll();
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
    }

    @Test
    @DisplayName("Should save ACCOUNT_CREATED event to Outbox table")
    void shouldSaveAccountCreatedEvent() {
        AccountDto.CreateRequest request = new AccountDto.CreateRequest("RUB");

        AccountDto.Response response = accountService.createAccount(request);

        List<OutboxEvent> events = outboxRepository.findAll();

        assertFalse(events.isEmpty(), "Outbox table should not be empty");

        Optional<OutboxEvent> eventOpt = events.stream()
                .filter(e -> "ACCOUNT_CREATED".equals(e.getType()))
                .findFirst();

        assertTrue(eventOpt.isPresent(), "Event of type ACCOUNT_CREATED not found");

        OutboxEvent event = eventOpt.get();

        assertEquals(response.id().toString(), event.getAggregateId(), "Aggregate ID mismatch");
        assertEquals("PENDING", event.getStatus(), "Initial status should be PENDING");

        assertTrue(event.getPayload().contains("RUB"), "Payload should contain currency");
        assertTrue(event.getPayload().contains(response.number()), "Payload should contain account number");
    }

    @Test
    @DisplayName("Should save BALANCE_DEPOSITED event to Outbox table")
    void shouldSaveDepositEvent() {
        AccountDto.CreateRequest createReq = new AccountDto.CreateRequest("USD");
        AccountDto.Response createRes = accountService.createAccount(createReq);
        Long accountId = createRes.id();
        BigDecimal depositAmount = new BigDecimal("100.00");

        accountService.topUpBalance(new AccountDto.TopUpRequest(accountId, depositAmount));

        List<OutboxEvent> events = outboxRepository.findAll();

        Optional<OutboxEvent> depositEventOpt = events.stream()
                .filter(e -> "BALANCE_DEPOSITED".equals(e.getType()))
                .findFirst();

        assertTrue(depositEventOpt.isPresent(), "Event of type BALANCE_DEPOSITED not found");

        OutboxEvent depositEvent = depositEventOpt.get();

        assertEquals(accountId.toString(), depositEvent.getAggregateId());

        assertTrue(depositEvent.getPayload().contains("100.00"), "Payload should contain amount");
        assertTrue(depositEvent.getPayload().contains("USD"), "Payload should contain currency");
    }
}
