package com.github.Silexj.payment_engine.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.Silexj.payment_engine.dto.TransferCompletedEvent;
import com.github.Silexj.payment_engine.dto.TransferDTO;
import com.github.Silexj.payment_engine.model.Account;
import com.github.Silexj.payment_engine.model.OutboxEvent;
import com.github.Silexj.payment_engine.model.Transaction;
import com.github.Silexj.payment_engine.model.TransactionStatus;
import com.github.Silexj.payment_engine.repository.AccountRepository;
import com.github.Silexj.payment_engine.repository.OutboxEventRepository;
import com.github.Silexj.payment_engine.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransferService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    /**
     * Выполняет перевод средств между двумя счетами.
     * Гарантирует атомарность и защиту от Deadlock через сортировку ID
     */
    @Transactional
    public TransferDTO.Response performTransfer(TransferDTO.PerformRequest request) {
        log.info("Initiating transfer: externalId={}, amount={}", request.externalId(), request.amount());

        var existingTx = transactionRepository.findByExternalId(request.externalId());
        if (existingTx.isPresent()) {
            log.warn("Duplicate request detected. Returning existing transaction: {}", existingTx.get().getId());
            return mapToResponse(existingTx.get());
        }

        if (request.fromAccountId().equals(request.toAccountId())) {
            throw new IllegalArgumentException("Self-transfer is not allowed");
        }

        Long firstLockId = Math.min(request.fromAccountId(), request.toAccountId());
        Long secondLockId = Math.max(request.fromAccountId(), request.toAccountId());

        Account firstLock = accountRepository.findByIdWithLock(firstLockId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + firstLockId));
        Account secondLock = accountRepository.findByIdWithLock(secondLockId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + secondLockId));

        Account sender = firstLock.getId().equals(request.fromAccountId()) ? firstLock : secondLock;
        Account receiver = firstLock.getId().equals(request.toAccountId()) ? firstLock : secondLock;

        validateTransfer(sender, receiver, request);

        sender.setBalance(sender.getBalance().subtract(request.amount()));
        receiver.setBalance(receiver.getBalance().add(request.amount()));

        Transaction transaction = saveTransaction(sender, receiver, request);

        saveOutboxEvent(transaction);

        log.info("Transfer completed successfully: txId={}", transaction.getId());

        return mapToResponse(transaction);
    }


    /**
     * Проверяет возможность выполнения перевода (валюта, баланс)
     */
    private void validateTransfer(Account sender, Account receiver, TransferDTO.PerformRequest request) {
        if (!sender.getCurrency().equals(receiver.getCurrency())) {
            log.error("Currency mismatch: sender={}, receiver={}", sender.getCurrency(), receiver.getCurrency());
            throw new IllegalArgumentException("Cross-currency transfers are not supported");
        }

        if (sender.getBalance().compareTo(request.amount()) < 0) {
            log.warn("Insufficient funds: accountId={}, balance={}, required={}",
                    sender.getId(), sender.getBalance(), request.amount());
            throw new IllegalArgumentException("Insufficient funds");
        }
    }


    /**
     * Сохраняет запись о транзакции в базу данных.
     * Генерирует внутренний ID транзакции.
     */
    private Transaction saveTransaction(Account sender, Account receiver, TransferDTO.PerformRequest request) {
        Transaction transaction = Transaction.builder()
                .id(UUID.randomUUID())
                .externalId(request.externalId())
                .sender(sender)
                .receiver(receiver)
                .amount(request.amount())
                .currency(sender.getCurrency())
                .timestamp(LocalDateTime.now())
                .status(TransactionStatus.SUCCESS)
                .build();

        return transactionRepository.save(transaction);
    }

    private TransferDTO.Response mapToResponse(Transaction tx) {
        return new TransferDTO.Response(
                tx.getId(),
                tx.getExternalId(),
                tx.getSender().getId(),
                tx.getReceiver().getId(),
                tx.getAmount(),
                tx.getCurrency(),
                tx.getStatus(),
                tx.getTimestamp(),
                tx.getErrorMessage()
        );
    }

    @SneakyThrows
    private void saveOutboxEvent(Transaction transaction) {
        var event = new TransferCompletedEvent(
                transaction.getId(),
                transaction.getSender().getId(),
                transaction.getReceiver().getId(),
                transaction.getAmount(),
                transaction.getCurrency()
        );

        String jsonPayload = objectMapper.writeValueAsString(event);

        OutboxEvent outboxEvent = OutboxEvent.builder()
                .id(UUID.randomUUID())
                .aggregateType("TRANSACTION")
                .aggregateId(transaction.getId().toString())
                .type("TRANSFER_COMPLETED")
                .payload(jsonPayload)
                .status("PENDING")
                .build();

        outboxEventRepository.save(outboxEvent);
    }

}
