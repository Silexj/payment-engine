package com.github.Silexj.payment_engine.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.Silexj.payment_engine.model.OutboxEvent;
import com.github.Silexj.payment_engine.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxWriterService {

    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;


    /**
     * Сохраняет событие в БД.
     * aggregateId - ID сущности (AccountId или TransactionId). Используется как partition key в Kafka.
     * type - Строковой тип события.
     */
    @Transactional(propagation = Propagation.MANDATORY) // Только внутри существующей транзакции
    @SneakyThrows
    public void saveEvent(String aggregateId, String type, Object eventPayload) {
        String jsonPayload = objectMapper.writeValueAsString(eventPayload);

        OutboxEvent outboxEvent = OutboxEvent.builder()
                .id(UUID.randomUUID())
                .aggregateType("PAYMENT_ENGINE")
                .aggregateId(aggregateId)
                .type(type)
                .payload(jsonPayload)
                .status("PENDING")
                .build();

        outboxRepository.save(outboxEvent);
    }
}
