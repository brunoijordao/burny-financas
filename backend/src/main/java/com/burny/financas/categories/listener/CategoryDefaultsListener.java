package com.burny.financas.categories.listener;

import com.burny.financas.auth.event.UserRegisteredEvent;
import com.burny.financas.categories.service.DefaultCategoryProvisioningService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Provisions default categories once the registration transaction has actually committed, so a
 * rolled-back registration never leaves orphaned categories behind. A failure here is logged and
 * swallowed rather than propagated: it must never affect the registration response the user already
 * received (see design.md Risks/Trade-offs).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CategoryDefaultsListener {

    private final DefaultCategoryProvisioningService provisioningService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserRegistered(UserRegisteredEvent event) {
        try {
            provisioningService.provisionDefaults(event.userId());
        } catch (RuntimeException ex) {
            log.error("Failed to provision default categories for user {}", event.userId(), ex);
        }
    }
}
