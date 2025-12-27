package com.github.Silexj.payment_engine.service.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.Silexj.payment_engine.dto.event.AccountCreatedEvent;
import com.github.Silexj.payment_engine.dto.event.BalanceDepositedEvent;
import com.github.Silexj.payment_engine.dto.event.TransferCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationListener {

    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${app.kafka.topic-name}", groupId = "notification-group")
    public void handlePaymentEvent(
            @Payload String payload,
            @Header("EVENT_TYPE") String eventType // Читаем заголовок!
    ) {
        try {
            log.info("Received event type: {}", eventType);

            switch (eventType) {
                case "TRANSFER_COMPLETED" -> {
                    var event = objectMapper.readValue(payload, TransferCompletedEvent.class);
                    sendNotification(event);
                }
                case "ACCOUNT_CREATED" -> {
                    var event = objectMapper.readValue(payload, AccountCreatedEvent.class);
                    log.info("[NOTIFICATION] Welcome! Account created: {}", event.number());
                }
                case "BALANCE_DEPOSITED" -> {
                    var event = objectMapper.readValue(payload, BalanceDepositedEvent.class);
                    log.info("[NOTIFICATION] Balance topped up: +{} {}", event.amount(), event.currency());
                }
                default -> log.warn("Unknown event type: {}", eventType);
            }

        } catch (Exception e) {
            log.error("Failed to process message", e);
        }
    }

    private void sendNotification(TransferCompletedEvent event) {
        log.info("[NOTIFICATION] Sending email to Account {}: 'You received {} {} from Account {}'",
                event.receiverAccountId(),
                event.amount(),
                event.currency(),
                event.senderAccountId());

        log.info("[NOTIFICATION] Sending push to Account {}: 'Transfer sent successfully'",
                event.senderAccountId());
    }
}
