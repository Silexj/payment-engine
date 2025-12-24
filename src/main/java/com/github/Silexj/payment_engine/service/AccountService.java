package com.github.Silexj.payment_engine.service;

import com.github.Silexj.payment_engine.dto.AccountDto;
import com.github.Silexj.payment_engine.model.Account;
import com.github.Silexj.payment_engine.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private final AccountRepository accountRepository;
    private final TransactionService transactionService;
    private final SecureRandom secureRandom = new SecureRandom();

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

    private AccountDto.Response tryCreateAccount(AccountDto.CreateRequest request) {
        String accountNumber = generateRandomNumberString();

        Account account = new Account();
        account.setCurrency(request.currency());
        account.setBalance(BigDecimal.ZERO);
        account.setNumber(accountNumber);

        account = accountRepository.saveAndFlush(account);

        log.info("Account created: id={}, number={}", account.getId(), account.getNumber());
        return mapToResponse(account);
    }

    @Transactional
    public AccountDto.Response topUpBalance(AccountDto.TopUpRequest request) {
        log.info("Processing top-up: {}", request);

        Account account = accountRepository.findByIdWithLock(request.accountId())
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        BigDecimal amount = request.amount();
        account.setBalance(account.getBalance().add(amount));

        accountRepository.save(account);

        transactionService.registerDeposit(account, amount);

        log.info("Balance topped up successfully: accountId={}", account.getId());
        return mapToResponse(account);
    }

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
