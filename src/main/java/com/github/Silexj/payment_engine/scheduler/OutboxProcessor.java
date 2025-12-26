package com.github.Silexj.payment_engine.scheduler;

import com.github.Silexj.payment_engine.model.OutboxEvent;
import com.github.Silexj.payment_engine.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxProcessor {

    private final OutboxEventRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${app.kafka.topic-name}")
    private String topicName;

    /**
     * Этот метод запускается каждые 500мс (как настроено в application.yml).
     * Он работает в транзакции: если отправка в Кафку упадет, статус в БД откатится.
     */
    @Scheduled(fixedDelayString = "${app.scheduler.outbox-interval}")
    @Transactional
    public void processOutbox() {
        List<OutboxEvent> events = outboxRepository.findBatchToProcess(50);

        if (events.isEmpty()) {
            return;
        }

        log.debug("Found {} outbox events to process", events.size());

        for (OutboxEvent event : events) {
            try {
                String key = event.getAggregateId();
                String payload = event.getPayload();

                kafkaTemplate.send(topicName, key, payload).get();

                event.setStatus("PROCESSED");
                event.setProcessedAt(LocalDateTime.now());

                log.info("Event processed: {}", event.getId());
            } catch (Exception e) {
                log.error("Error processing event: {}", event.getId(), e);
            }
        }
    }
}
