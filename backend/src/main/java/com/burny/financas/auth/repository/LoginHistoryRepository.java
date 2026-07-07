package com.burny.financas.auth.repository;

import com.burny.financas.auth.entity.LoginHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoginHistoryRepository extends JpaRepository<LoginHistory, Long> {

    List<LoginHistory> findByUserIdOrderByCreatedAtDesc(Long userId);
}
