package com.burny.financas.agent.service.tool;

import static org.assertj.core.api.Assertions.assertThat;

import com.burny.financas.accounts.dto.AccountResponse;
import com.burny.financas.accounts.entity.AccountType;
import com.burny.financas.agent.service.GeminiApiTypes;
import com.burny.financas.categories.dto.CategoryResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Guards design.md Decision 3 structurally: no tool's parameter schema may ever expose a
 * user-identifying field, since dispatch never reads {@code userId} from model output.
 */
class AgentToolCatalogTest {

    private static final List<String> USER_IDENTIFYING_PARAM_NAMES = List.of("userId", "user_id", "userid", "ownerId");

    private List<AccountResponse> sampleAccounts() {
        return List.of(new AccountResponse(
                1L, "Conta Corrente", "wallet", "#000", AccountType.CHECKING, true,
                new BigDecimal("100.00"), null, null, null, null));
    }

    private List<CategoryResponse> sampleCategories() {
        return List.of(new CategoryResponse(
                10L, "Alimentacao", "food", "#111", null, false, true, List.of(), null, null));
    }

    @Test
    void noFunctionDeclarationExposesAUserIdentifyingParameter() {
        List<GeminiApiTypes.FunctionDeclaration> declarations =
                AgentToolCatalog.declarations(sampleAccounts(), sampleCategories());

        for (GeminiApiTypes.FunctionDeclaration declaration : declarations) {
            @SuppressWarnings("unchecked")
            Map<String, Object> properties = (Map<String, Object>) declaration.parameters().get("properties");
            for (String paramName : properties.keySet()) {
                assertThat(USER_IDENTIFYING_PARAM_NAMES).as("tool %s parameter %s", declaration.name(), paramName)
                        .doesNotContain(paramName);
            }
        }
    }

    @Test
    void declaresAllSixTools() {
        List<GeminiApiTypes.FunctionDeclaration> declarations =
                AgentToolCatalog.declarations(sampleAccounts(), sampleCategories());

        assertThat(declarations).extracting(GeminiApiTypes.FunctionDeclaration::name).containsExactlyInAnyOrder(
                AgentToolCatalog.GET_SPENDING_BY_CATEGORY,
                AgentToolCatalog.GET_BUDGET_STATUS,
                AgentToolCatalog.GET_GOAL_STATUS,
                AgentToolCatalog.GET_PROJECTED_CASH_FLOW,
                AgentToolCatalog.GET_INVESTMENT_SUMMARY,
                AgentToolCatalog.PROPOSE_TRANSACTION);
    }

    @Test
    void proposeTransactionAccountIdEnumReflectsTheCallersOwnAccounts() {
        List<GeminiApiTypes.FunctionDeclaration> declarations =
                AgentToolCatalog.declarations(sampleAccounts(), sampleCategories());

        GeminiApiTypes.FunctionDeclaration proposeTransaction = declarations.stream()
                .filter(d -> d.name().equals(AgentToolCatalog.PROPOSE_TRANSACTION))
                .findFirst().orElseThrow();

        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) proposeTransaction.parameters().get("properties");
        @SuppressWarnings("unchecked")
        Map<String, Object> accountIdSchema = (Map<String, Object>) properties.get("accountId");
        @SuppressWarnings("unchecked")
        List<String> enumValues = (List<String>) accountIdSchema.get("enum");

        assertThat(enumValues).containsExactly("1");
    }
}
