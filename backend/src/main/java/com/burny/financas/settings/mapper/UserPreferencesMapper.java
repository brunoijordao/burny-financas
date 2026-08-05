package com.burny.financas.settings.mapper;

import com.burny.financas.settings.dto.UserPreferencesResponse;
import com.burny.financas.settings.entity.UserPreferences;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserPreferencesMapper {

    default UserPreferencesResponse toResponse(UserPreferences preferences) {
        return new UserPreferencesResponse(preferences.getCurrency(), preferences.getDateFormat());
    }
}
