package com.burny.financas.accounts.repository;

import com.burny.financas.accounts.entity.AccountTransfer;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountTransferRepository extends JpaRepository<AccountTransfer, Long> {

    List<AccountTransfer> findAllByUserIdOrderByCreatedAtDesc(Long userId);
}
