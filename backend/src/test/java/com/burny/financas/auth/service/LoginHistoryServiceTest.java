package com.burny.financas.auth.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.burny.financas.auth.entity.LoginHistory;
import com.burny.financas.auth.repository.LoginHistoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LoginHistoryServiceTest {

    @Mock
    private LoginHistoryRepository loginHistoryRepository;

    @Test
    void recordSwallowsRepositoryFailuresWithoutPropagating() {
        LoginHistoryService service = new LoginHistoryService(loginHistoryRepository);
        when(loginHistoryRepository.save(any(LoginHistory.class)))
                .thenThrow(new RuntimeException("simulated transient DB error"));

        // Recording must never throw, so that a login-history write failure can never block
        // or fail the surrounding login flow (per the login-history spec).
        assertThatCode(() -> service.record(null, "someone@example.com", "127.0.0.1", true))
                .doesNotThrowAnyException();

        verify(loginHistoryRepository).save(any(LoginHistory.class));
    }

    @Test
    void recordPersistsEntryOnSuccessPath() {
        LoginHistoryService service = new LoginHistoryService(loginHistoryRepository);
        when(loginHistoryRepository.save(any(LoginHistory.class))).thenAnswer(inv -> inv.getArgument(0));

        service.record(null, "someone@example.com", "10.0.0.1", false);

        verify(loginHistoryRepository).save(any(LoginHistory.class));
    }
}
