package com.github.Silexj.payment_engine.repository;

import com.github.Silexj.payment_engine.model.Account;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    /**
     * Получает аккаунт по ID с наложением эксклюзивной пессимистичной блокировки.
     *
     * Другие транзакции, пытающиеся прочитать или изменить эту запись, будут ждать.
     * Если блокировку не удается получить в течение 3000 мс,
     * будет выброшено исключение (PessimisticLockException).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({
            @QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000")
    })
    @Query("SELECT a FROM Account a WHERE a.id = :id")
    Optional<Account> findByIdWithLock(@Param("id") Long id);

    /**
     * Поиск по уникальному бизнес-номеру счета.
     */
    Optional<Account> findByNumber(String number);

    /**
     * Проверка существования счета (используется при генерации уникального номера).
     */
    boolean existsByNumber(String number);
}
