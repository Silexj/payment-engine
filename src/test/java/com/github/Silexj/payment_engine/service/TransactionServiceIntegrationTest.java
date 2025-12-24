package com.github.Silexj.payment_engine.service;

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
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class TransactionServiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:latest");

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountRepository accountRepository;

    @AfterEach
    void tearDown() {
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
    }

    @Test
    @DisplayName("Should save transaction when called inside active transaction")
    @Transactional
    void shouldRegisterDeposit_WhenTransactionIsActive() {
        Account account = new Account();
        account.setNumber("12345678901234567890");
        account.setCurrency("RUB");
        account.setBalance(BigDecimal.ZERO);
        account = accountRepository.save(account);

        BigDecimal amount = new BigDecimal("500.00");

        transactionService.registerDeposit(account, amount);

        List<Transaction> transactions = transactionRepository.findAll();
        assertEquals(1, transactions.size());

        Transaction tx = transactions.get(0);
        assertNotNull(tx.getId());
        assertNotNull(tx.getExternalId());
        assertEquals(amount, tx.getAmount());
        assertNull(tx.getSender());
        assertEquals(account.getId(), tx.getReceiver().getId());
        assertEquals("SUCCESS", tx.getStatus().name());
    }

    @Test
    @DisplayName("Should throw exception when called WITHOUT active transaction")
    void shouldThrowException_WhenNoTransaction() {
        Account account = new Account();
        account.setNumber("12345678901234567890");
        account.setCurrency("USD");
        account.setBalance(BigDecimal.ZERO);
        account = accountRepository.save(account);

        BigDecimal amount = BigDecimal.TEN;

        Account finalAccount = account;
        assertThrows(IllegalTransactionStateException.class, () -> {
            transactionService.registerDeposit(finalAccount, amount);
        });

        assertEquals(0, transactionRepository.count(), "Transaction should not be saved");
    }
}