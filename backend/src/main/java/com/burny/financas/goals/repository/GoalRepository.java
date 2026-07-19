package com.burny.financas.goals.repository;

import com.burny.financas.goals.entity.Goal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GoalRepository extends JpaRepository<Goal, Long> {

    Optional<Goal> findByIdAndUserId(Long id, Long userId);

    @Query("""
            SELECT g FROM Goal g
            WHERE g.user.id = :userId
              AND g.active = :active
            ORDER BY g.completed ASC, g.deadline ASC
            """)
    List<Goal> findByUserIdAndActive(@Param("userId") Long userId, @Param("active") boolean active);
}
