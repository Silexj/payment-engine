package com.github.Silexj.payment_engine.service;

import com.github.Silexj.payment_engine.model.Account;
import com.github.Silexj.payment_engine.model.Transaction;
import com.github.Silexj.payment_engine.model.TransactionStatus;
import com.github.Silexj.payment_engine.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;

    @Transactional(propagation = Propagation.MANDATORY)
    public void registerDeposit(Account receiver, BigDecimal amount) {


        Transaction transaction = Transaction.builder()
                .id(UUID.randomUUID())
                .externalId(UUID.randomUUID())
                .sender(null) // Deposit -> sender is null
                .receiver(receiver)
                .amount(amount)
                .currency(receiver.getCurrency())
                .timestamp(LocalDateTime.now())
                .status(TransactionStatus.SUCCESS)
                .build();

        transactionRepository.save(transaction);

        log.info("Deposit transaction registered: txId={}, accountId={}, amount={}", transaction.getId(), receiver.getId(),amount);
    }
}
