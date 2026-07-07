package com.burny.financas.auth.service;

import com.burny.financas.auth.dto.LoginHistoryResponse;
import com.burny.financas.auth.entity.LoginHistory;
import com.burny.financas.auth.entity.User;
import com.burny.financas.auth.repository.LoginHistoryRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoginHistoryService {

    private final LoginHistoryRepository loginHistoryRepository;

    /**
     * Records a login attempt. Runs in its own transaction (REQUIRES_NEW) and swallows any
     * failure so that a transient DB error writing history can never roll back or block the
     * surrounding login/token-issuance flow. Failures are only logged.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(User user, String emailAttempted, String ipAddress, boolean success) {
        try {
            LoginHistory entry = LoginHistory.builder()
                    .user(user)
                    .emailAttempted(emailAttempted)
                    .ipAddress(ipAddress)
                    .success(success)
                    .build();
            loginHistoryRepository.save(entry);
        } catch (Exception ex) {
            log.warn("Failed to record login history for email={}", emailAttempted, ex);
        }
    }

    @Transactional(readOnly = true)
    public List<LoginHistoryResponse> getHistoryForUser(Long userId) {
        return loginHistoryRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(h -> new LoginHistoryResponse(
                        h.getId(), h.getEmailAttempted(), h.getIpAddress(), h.isSuccess(), h.getCreatedAt()))
                .toList();
    }
}
