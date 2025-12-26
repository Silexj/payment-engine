package com.github.Silexj.payment_engine.service.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.Silexj.payment_engine.dto.TransferCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationListener {

    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${app.kafka.topic-name}", groupId = "notification-group")
    public void handlePaymentEvent(String messagePayload) {
        try {
            TransferCompletedEvent event = objectMapper.readValue(messagePayload, TransferCompletedEvent.class);
            sendNotification(event);
        } catch (Exception e) {
            log.error("Failed to process message from Kafka", e);
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
