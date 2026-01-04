package com.github.Silexj.payment_engine.repository;

import com.github.Silexj.payment_engine.model.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    /**
     * Получает пакет событий со статусом 'PENDING' для обработки.
     * Использует конструкцию FOR UPDATE SKIP LOCKED.
     * Это позволяет нескольким экземплярам приложения (или потокам) одновременно читать
     * таблицу, не блокируя друг друга и не обрабатывая одни и те же события дважды.
     *
     * Гарантирует порядок обработки FIFO (First-In-First-Out) по времени создания.
     */
    @Query(value = """
        SELECT * FROM outbox_events 
        WHERE status = 'PENDING' 
        ORDER BY created_at ASC 
        LIMIT :limit 
        FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    List<OutboxEvent> findBatchToProcess(@Param("limit") int limit);
}
