package com.github.Silexj.payment_engine.repository;

import com.github.Silexj.payment_engine.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    boolean existsByExternalId(UUID externalId);
}
