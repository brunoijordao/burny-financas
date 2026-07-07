package com.burny.financas.transactions.repository;

import com.burny.financas.transactions.entity.TransactionRecurrence;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRecurrenceRepository extends JpaRepository<TransactionRecurrence, Long> {

    Optional<TransactionRecurrence> findByIdAndUserId(Long id, Long userId);

    List<TransactionRecurrence> findAllByActiveTrueAndNextOccurrenceDateLessThanEqual(LocalDate date);
}
