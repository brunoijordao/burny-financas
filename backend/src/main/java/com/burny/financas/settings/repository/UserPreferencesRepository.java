package com.burny.financas.settings.repository;

import com.burny.financas.settings.entity.UserPreferences;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserPreferencesRepository extends JpaRepository<UserPreferences, Long> {

    Optional<UserPreferences> findByUserId(Long userId);
}
