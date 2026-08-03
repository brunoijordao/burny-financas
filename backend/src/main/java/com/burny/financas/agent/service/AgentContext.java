package com.burny.financas.agent.service;

import com.burny.financas.accounts.dto.AccountResponse;
import com.burny.financas.categories.dto.CategoryResponse;
import java.util.List;

/** Per-request financial context: the rendered system instruction plus the raw lists used to build tool enums. */
record AgentContext(String systemInstructionText, List<AccountResponse> accounts, List<CategoryResponse> categories) {
}
