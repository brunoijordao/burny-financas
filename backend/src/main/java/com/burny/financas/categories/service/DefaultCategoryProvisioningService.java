package com.burny.financas.categories.service;

import com.burny.financas.auth.entity.User;
import com.burny.financas.auth.repository.UserRepository;
import com.burny.financas.categories.entity.Category;
import com.burny.financas.categories.repository.CategoryRepository;
import com.burny.financas.categories.repository.DefaultCategoryRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds a user's default categories from the currently active rows of the administratively managed
 * {@code default_categories} table (edited directly via SQL, never through an API — see
 * design.md). Exposed as its own idempotent operation (safe to call more than once) so it can be
 * used both by the post-registration listener and as a manual failsafe if that listener ever fails
 * silently (see design.md Risks/Trade-offs).
 */
@Service
@RequiredArgsConstructor
public class DefaultCategoryProvisioningService {

    private final CategoryRepository categoryRepository;
    private final DefaultCategoryRepository defaultCategoryRepository;
    private final UserRepository userRepository;

    /**
     * {@code REQUIRES_NEW} is required, not just {@code REQUIRED}: this is invoked from
     * {@code CategoryDefaultsListener}, an {@code @TransactionalEventListener(AFTER_COMMIT)}
     * callback. Spring fires {@code afterCommit} callbacks before it unbinds the just-committed
     * transaction's resources from the thread, so a plain {@code REQUIRED} call here would silently
     * attach to that already-completing transaction instead of opening a genuinely new one — the
     * inserts below would then never actually persist.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void provisionDefaults(Long userId) {
        if (categoryRepository.existsByUserIdAndDefaultCategoryTrue(userId)) {
            return;
        }

        User userReference = userRepository.getReferenceById(userId);
        List<Category> categories = defaultCategoryRepository.findAllByActiveTrueOrderBySortOrderAscIdAsc().stream()
                .map(def -> Category.builder()
                        .user(userReference)
                        .name(def.getName())
                        .icon(def.getIcon())
                        .color(def.getColor())
                        .defaultCategory(true)
                        .active(true)
                        .build())
                .toList();

        categoryRepository.saveAll(categories);
    }
}
