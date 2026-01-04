package com.github.Silexj.payment_engine.service;

import com.github.Silexj.payment_engine.dto.AccountDto;
import com.github.Silexj.payment_engine.dto.event.AccountCreatedEvent;
import com.github.Silexj.payment_engine.dto.event.BalanceDepositedEvent;
import com.github.Silexj.payment_engine.model.Account;
import com.github.Silexj.payment_engine.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private final AccountRepository accountRepository;
    private final TransactionService transactionService;
    private final SecureRandom secureRandom = new SecureRandom();
    private final OutboxWriterService outboxWriter;

    /**
     * Создает новый счет с генерацией уникального номера.
     * Реализует механизм ретраев (до 3 попыток) для обработки редких коллизий.
     */
    @Transactional
    public AccountDto.Response createAccount(AccountDto.CreateRequest request) {
        int attempts = 0;
        while (attempts < 3) {
            try {
                return tryCreateAccount(request);
            } catch (DataIntegrityViolationException e) {
                log.warn("Account number collision detected, retrying...");
                attempts++;
            }
        }
        throw new IllegalStateException("Failed to generate unique account number after 3 attempts");
    }

    /**
     * Сохраняет сущность и пишет событие в Outbox.
     * Использует saveAndFlush для мгновенной проверки уникальности номера БД.
     */
    private AccountDto.Response tryCreateAccount(AccountDto.CreateRequest request) {
        String accountNumber = generateRandomNumberString();

        Account account = new Account();
        account.setCurrency(request.currency());
        account.setBalance(BigDecimal.ZERO);
        account.setNumber(accountNumber);

        account = accountRepository.saveAndFlush(account);

        var event = new AccountCreatedEvent(
                account.getId(),
                account.getNumber(),
                account.getCurrency(),
                LocalDateTime.now()
        );
        outboxWriter.saveEvent(account.getId().toString(), "ACCOUNT_CREATED", event);

        log.info("Account created: id={}, number={}", account.getId(), account.getNumber());
        return mapToResponse(account);
    }

    /**
     * Пополняет баланс счета (Deposit).
     * Использует пессимистичную блокировку (findByIdWithLock) для исключения Race Conditions.
     * Атомарно обновляет баланс, пишет историю операций и создает событие BALANCE_DEPOSITED.
     */
    @Transactional
    public AccountDto.Response topUpBalance(AccountDto.TopUpRequest request) {
        log.info("Processing top-up: {}", request);

        Account account = accountRepository.findByIdWithLock(request.accountId())
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        BigDecimal amount = request.amount();
        account.setBalance(account.getBalance().add(amount));

        accountRepository.save(account);

        transactionService.registerDeposit(account, amount);

        var event = new BalanceDepositedEvent(
                account.getId(),
                request.amount(),
                account.getCurrency(),
                UUID.randomUUID()
        );
        outboxWriter.saveEvent(account.getId().toString(), "BALANCE_DEPOSITED", event);

        log.info("Balance topped up successfully: accountId={}", account.getId());
        return mapToResponse(account);
    }

    /**
     * Возвращает информацию о счете.
     * Использует readOnly транзакцию для оптимизации работы с пулом соединений и БД.
     */
    @Transactional(readOnly = true)
    public AccountDto.Response getAccount(Long id) {
        return accountRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
    }

    private String generateRandomNumberString() {
        StringBuilder sb = new StringBuilder(20);
        for (int i = 0; i < 20; i++) {
            sb.append(secureRandom.nextInt(10));
        }
        return sb.toString();
    }

    private AccountDto.Response mapToResponse(Account account) {
        return new AccountDto.Response(
                account.getId(),
                account.getNumber(),
                account.getBalance(),
                account.getCurrency()
        );
    }
}
