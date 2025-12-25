package com.github.Silexj.payment_engine.service;

import com.github.Silexj.payment_engine.dto.AccountDto;
import com.github.Silexj.payment_engine.model.Account;
import com.github.Silexj.payment_engine.model.Transaction;
import com.github.Silexj.payment_engine.repository.AccountRepository;
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

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
public class AccountServiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:latest");

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @AfterEach
    void cleanUp() {
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
    }

    @Test
    @DisplayName("Should create account with unique number and zero balance")
    void shouldCreateAccount() {
        var request = new AccountDto.CreateRequest("RUB");

        var response = accountService.createAccount(request);

        assertNotNull(response.id(), "Account ID should not be null");

        assertEquals("RUB", response.currency(), "Currency should match");

        assertTrue(BigDecimal.ZERO.compareTo(response.balance()) == 0, "Balance should be zero");

        assertNotNull(response.number());
        assertEquals(20, response.number().length(), "Account number length should be 20");

        assertTrue(accountRepository.findById(response.id()).isPresent());
    }

    @Test
    @DisplayName("Should top up balance and create audit transaction")
    void shouldTopUpBalanceAndCreateTransaction() {
        AccountDto.CreateRequest createReq = new AccountDto.CreateRequest("USD");
        AccountDto.Response accountRes = accountService.createAccount(createReq);
        Long accountId = accountRes.id();

        BigDecimal amount = new BigDecimal("100.50");
        AccountDto.TopUpRequest topUpReq = new AccountDto.TopUpRequest(accountId, amount);

        AccountDto.Response topUpRes = accountService.topUpBalance(topUpReq);

        assertTrue(amount.compareTo(topUpRes.balance()) == 0, "Response balance should match top-up amount");

        Account updatedAccount = accountRepository.findById(accountId).orElseThrow();
        assertTrue(amount.compareTo(updatedAccount.getBalance()) == 0, "DB balance should match");

        List<Transaction> transactions = transactionRepository.findAll();

        assertEquals(1, transactions.size(), "Should contain exactly 1 transaction");

        Transaction tx = transactions.get(0);

        assertEquals(accountId, tx.getReceiver().getId(), "Receiver ID mismatch");
        assertNull(tx.getSender(), "Sender should be NULL for TopUp");
        assertTrue(amount.compareTo(tx.getAmount()) == 0, "Transaction amount mismatch");
        assertEquals("USD", tx.getCurrency());
        assertEquals("SUCCESS", tx.getStatus().name());
    }

    @Test
    @DisplayName("Should throw exception when account not found")
    void shouldThrowWhenAccountNotFound() {
        AccountDto.TopUpRequest req = new AccountDto.TopUpRequest(999999L, BigDecimal.TEN);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            accountService.topUpBalance(req);
        });

        assertEquals("Account not found", exception.getMessage());
    }
}
