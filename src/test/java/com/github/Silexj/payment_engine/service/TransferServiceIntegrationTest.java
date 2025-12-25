package com.github.Silexj.payment_engine.service;

import com.github.Silexj.payment_engine.dto.AccountDto;
import com.github.Silexj.payment_engine.dto.TransferDTO;
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
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
public class TransferServiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:latest");

    @Autowired
    private TransferService transferService;
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
    @DisplayName("Success: Should transfer money and update balances")
    void shouldTransferMoneySuccessfully() {
        Long senderId = createAccount("RUB", "1000.00");
        Long receiverId = createAccount("RUB", "0.00");

        BigDecimal amount = new BigDecimal("500.00");

        TransferDTO.PerformRequest request = new TransferDTO.PerformRequest(
                UUID.randomUUID(), senderId, receiverId, amount
        );

        TransferDTO.Response response = transferService.performTransfer(request);

        assertEquals("SUCCESS", response.status().name());
        assertNotNull(response.transactionId());

        assertBalance(senderId, "500.00");
        assertBalance(receiverId, "500.00");

        assertEquals(2, transactionRepository.count(), "Should be 2 transactions (1 Deposit + 1 Transfer)");

        Transaction lastTx = transactionRepository.findById(response.transactionId()).orElseThrow();
        assertEquals(amount.setScale(2), lastTx.getAmount().setScale(2));
        assertEquals(senderId, lastTx.getSender().getId());
    }

    @Test
    @DisplayName("Fail: Should throw exception if insufficient funds")
    void shouldFailIfInsufficientFunds() {
        Long senderId = createAccount("RUB", "10.00");
        Long receiverId = createAccount("RUB", "0.00");
        BigDecimal amount = new BigDecimal("100.00");

        TransferDTO.PerformRequest request = new TransferDTO.PerformRequest(
                UUID.randomUUID(), senderId, receiverId, amount
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                transferService.performTransfer(request)
        );
        assertEquals("Insufficient funds", ex.getMessage());

        assertBalance(senderId, "10.00");
        assertBalance(receiverId, "0.00");


        assertEquals(1, transactionRepository.count(), "Transaction count should not change (rollback)");
    }

    @Test
    @DisplayName("Concurrency: Should survive Deadlock when 2 threads transfer cross-wise")
    void shouldExecuteConcurrentTransfersWithoutDeadlock() throws InterruptedException {
        Long account1 = createAccount("EUR", "1000.00");
        Long account2 = createAccount("EUR", "1000.00");

        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        executor.submit(() -> {
            try {
                transferService.performTransfer(new TransferDTO.PerformRequest(
                        UUID.randomUUID(), account1, account2, new BigDecimal("100.00")
                ));
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        });

        executor.submit(() -> {
            try {
                transferService.performTransfer(new TransferDTO.PerformRequest(
                        UUID.randomUUID(), account2, account1, new BigDecimal("100.00")
                ));
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        });

        boolean finished = latch.await(10, TimeUnit.SECONDS);
        assertTrue(finished, "Deadlock detected, Threads didn't finish");

        assertBalance(account1, "1000.00");
        assertBalance(account2, "1000.00");


        assertEquals(4, transactionRepository.count(), "Should have 4 transactions total");
    }

    @Test
    @DisplayName("Idempotency: Should not deduct money twice for same externalId")
    void shouldBeIdempotent() {
        Long senderId = createAccount("USD", "1000.00");
        Long receiverId = createAccount("USD", "0.00");
        BigDecimal amount = new BigDecimal("100.00");
        UUID idempotencyKey = UUID.randomUUID();

        TransferDTO.PerformRequest request = new TransferDTO.PerformRequest(
                idempotencyKey, senderId, receiverId, amount
        );

        TransferDTO.Response response1 = transferService.performTransfer(request);

        TransferDTO.Response response2 = transferService.performTransfer(request);

        assertEquals(response1.transactionId(), response2.transactionId());
        assertEquals("SUCCESS", response1.status().name());
        assertEquals("SUCCESS", response2.status().name());

        assertBalance(senderId, "900.00");
        assertBalance(receiverId, "100.00");

        assertEquals(2, transactionRepository.count(), "Should be 2 transactions (1 Deposit + 1 Transfer)");
    }

    private Long createAccount(String currency, String initialBalance) {
        var createReq = new AccountDto.CreateRequest(currency);
        var response = accountService.createAccount(createReq);

        BigDecimal balance = new BigDecimal(initialBalance);
        if (balance.compareTo(BigDecimal.ZERO) > 0) {
            accountService.topUpBalance(new AccountDto.TopUpRequest(response.id(), balance));
        }

        return response.id();
    }

    private void assertBalance(Long accountId, String expectedBalance) {
        Account account = accountRepository.findById(accountId).orElseThrow();
        assertTrue(new BigDecimal(expectedBalance).compareTo(account.getBalance()) == 0,
                "Balance mismatch. Expected: " + expectedBalance + ", Actual: " + account.getBalance());
    }
}
