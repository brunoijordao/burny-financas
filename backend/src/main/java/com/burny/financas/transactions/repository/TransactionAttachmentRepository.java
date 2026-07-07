package com.burny.financas.transactions.repository;

import com.burny.financas.transactions.entity.TransactionAttachment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionAttachmentRepository extends JpaRepository<TransactionAttachment, Long> {

    List<TransactionAttachment> findAllByTransactionId(Long transactionId);

    Optional<TransactionAttachment> findByIdAndTransactionId(Long id, Long transactionId);
}
