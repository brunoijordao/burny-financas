package com.burny.financas.settings.service;

import com.burny.financas.auth.repository.UserRepository;
import com.burny.financas.settings.dto.UpdateUserPreferencesRequest;
import com.burny.financas.settings.dto.UserPreferencesResponse;
import com.burny.financas.settings.entity.CurrencyCode;
import com.burny.financas.settings.entity.DateFormatCode;
import com.burny.financas.settings.entity.UserPreferences;
import com.burny.financas.settings.mapper.UserPreferencesMapper;
import com.burny.financas.settings.repository.UserPreferencesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * No row for a user means "use the defaults" (see design.md Decision 1) — {@link #get} never
 * creates a row, only {@link #update} does, and only on the user's first write.
 */
@Service
@RequiredArgsConstructor
public class UserPreferencesService {

    public static final CurrencyCode DEFAULT_CURRENCY = CurrencyCode.BRL;
    public static final DateFormatCode DEFAULT_DATE_FORMAT = DateFormatCode.DD_MM_YYYY;

    private final UserPreferencesRepository userPreferencesRepository;
    private final UserRepository userRepository;
    private final UserPreferencesMapper userPreferencesMapper;

    @Transactional(readOnly = true)
    public UserPreferencesResponse get(Long userId) {
        return userPreferencesRepository.findByUserId(userId)
                .map(userPreferencesMapper::toResponse)
                .orElseGet(() -> new UserPreferencesResponse(DEFAULT_CURRENCY, DEFAULT_DATE_FORMAT));
    }

    @Transactional
    public UserPreferencesResponse update(Long userId, UpdateUserPreferencesRequest request) {
        UserPreferences preferences = userPreferencesRepository.findByUserId(userId)
                .orElseGet(() -> UserPreferences.builder()
                        .user(userRepository.getReferenceById(userId))
                        .build());

        preferences.setCurrency(request.currency());
        preferences.setDateFormat(request.dateFormat());

        return userPreferencesMapper.toResponse(userPreferencesRepository.save(preferences));
    }
}
