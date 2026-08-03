package com.burny.financas.agent.service.tool;

import com.burny.financas.accounts.dto.AccountResponse;
import com.burny.financas.agent.service.GeminiApiTypes;
import com.burny.financas.categories.dto.CategoryResponse;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Fixed, backend-owned set of tools exposed to Gemini (design.md Decision 3) — not model-
 * configurable. Every {@code functionDeclaration}'s parameter schema describes only business
 * fields; none of them include a user identifier, so there is no field a hallucinating model could
 * fill with a foreign user's id. {@code accountId}/{@code categoryId} are declared as string enums
 * populated from the caller's own accounts/categories (fetched the same request), steering the
 * model toward ids that actually exist for this user (still re-validated on dispatch — see
 * {@link AgentToolDispatcher}).
 */
public final class AgentToolCatalog {

    public static final String GET_SPENDING_BY_CATEGORY = "getSpendingByCategory";
    public static final String GET_BUDGET_STATUS = "getBudgetStatus";
    public static final String GET_GOAL_STATUS = "getGoalStatus";
    public static final String GET_PROJECTED_CASH_FLOW = "getProjectedCashFlow";
    public static final String GET_INVESTMENT_SUMMARY = "getInvestmentSummary";
    public static final String PROPOSE_TRANSACTION = "proposeTransaction";

    private AgentToolCatalog() {
    }

    public static List<GeminiApiTypes.FunctionDeclaration> declarations(
            List<AccountResponse> accounts, List<CategoryResponse> categories) {
        List<GeminiApiTypes.FunctionDeclaration> declarations = new ArrayList<>();
        declarations.add(getSpendingByCategoryDeclaration());
        declarations.add(getBudgetStatusDeclaration());
        declarations.add(getGoalStatusDeclaration());
        declarations.add(getProjectedCashFlowDeclaration());
        declarations.add(getInvestmentSummaryDeclaration());
        declarations.add(proposeTransactionDeclaration(accounts, categories));
        return declarations;
    }

    private static GeminiApiTypes.FunctionDeclaration getSpendingByCategoryDeclaration() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("startDate", stringProp("Data inicial do periodo, formato yyyy-MM-dd. Se omitido, usa o inicio do mes atual."));
        properties.put("endDate", stringProp("Data final do periodo, formato yyyy-MM-dd. Se omitido, usa o fim do mes atual."));
        properties.put("categoryName", stringProp("Nome (ou parte do nome) de uma categoria para filtrar o resultado. Opcional."));
        return new GeminiApiTypes.FunctionDeclaration(
                GET_SPENDING_BY_CATEGORY,
                "Retorna quanto o usuario gastou por categoria em um periodo (padrao: mes atual).",
                objectSchema(properties, List.of()));
    }

    private static GeminiApiTypes.FunctionDeclaration getBudgetStatusDeclaration() {
        return new GeminiApiTypes.FunctionDeclaration(
                GET_BUDGET_STATUS,
                "Retorna os orcamentos por categoria do mes atual, com limite e valor ja gasto.",
                objectSchema(Map.of(), List.of()));
    }

    private static GeminiApiTypes.FunctionDeclaration getGoalStatusDeclaration() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("goalName", stringProp("Nome (ou parte do nome) da meta para filtrar. Opcional; se omitido, retorna todas as metas."));
        return new GeminiApiTypes.FunctionDeclaration(
                GET_GOAL_STATUS,
                "Retorna o progresso das metas de economia do usuario (valor atual, percentual, se esta no prazo).",
                objectSchema(properties, List.of()));
    }

    private static GeminiApiTypes.FunctionDeclaration getProjectedCashFlowDeclaration() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("months", integerProp("Numero de meses a projetar a partir de hoje. Padrao: 3."));
        return new GeminiApiTypes.FunctionDeclaration(
                GET_PROJECTED_CASH_FLOW,
                "Retorna o fluxo de caixa projetado do usuario com base em contas a pagar/receber pendentes.",
                objectSchema(properties, List.of()));
    }

    private static GeminiApiTypes.FunctionDeclaration getInvestmentSummaryDeclaration() {
        return new GeminiApiTypes.FunctionDeclaration(
                GET_INVESTMENT_SUMMARY,
                "Retorna o resumo da carteira de investimentos do usuario: total investido, valor atual e rentabilidade.",
                objectSchema(Map.of(), List.of()));
    }

    private static GeminiApiTypes.FunctionDeclaration proposeTransactionDeclaration(
            List<AccountResponse> accounts, List<CategoryResponse> categories) {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("accountId", stringEnumProp(
                "Id da conta onde a transacao ocorreu. " + describeAccounts(accounts),
                accountIds(accounts)));
        properties.put("type", stringEnumProp(
                "Tipo da transacao: INCOME (entrada/receita) ou EXPENSE (saida/despesa).",
                List.of("INCOME", "EXPENSE")));
        properties.put("amount", numberProp("Valor da transacao, sempre positivo."));
        properties.put("description", stringProp("Descricao curta da transacao."));
        List<String> categoryIds = categoryIds(categories);
        if (!categoryIds.isEmpty()) {
            properties.put("categoryId", stringEnumProp(
                    "Id da categoria da transacao, se identificavel. " + describeCategories(categories),
                    categoryIds));
        }
        properties.put("date", stringProp("Data da transacao, formato yyyy-MM-dd. Se omitido, usa a data de hoje."));

        return new GeminiApiTypes.FunctionDeclaration(
                PROPOSE_TRANSACTION,
                "Monta um RASCUNHO de transacao para o usuario revisar e confirmar explicitamente. "
                        + "NUNCA cria a transacao de fato - apenas retorna os dados para confirmacao. "
                        + "So chame esta funcao quando conta, tipo, valor e descricao estiverem claros; "
                        + "caso contrario, pergunte ao usuario o que falta.",
                objectSchema(properties, List.of("accountId", "type", "amount", "description")));
    }

    private static String describeAccounts(List<AccountResponse> accounts) {
        return "Contas disponiveis: " + accounts.stream()
                .map(a -> a.id() + "=" + a.name())
                .collect(Collectors.joining(", "));
    }

    private static String describeCategories(List<CategoryResponse> categories) {
        return "Categorias disponiveis: " + flattenCategories(categories).stream()
                .map(c -> c.id() + "=" + c.name())
                .collect(Collectors.joining(", "));
    }

    private static List<String> accountIds(List<AccountResponse> accounts) {
        return accounts.stream().map(a -> String.valueOf(a.id())).toList();
    }

    private static List<String> categoryIds(List<CategoryResponse> categories) {
        return flattenCategories(categories).stream().map(c -> String.valueOf(c.id())).toList();
    }

    private static List<CategoryResponse> flattenCategories(List<CategoryResponse> categories) {
        List<CategoryResponse> flat = new ArrayList<>();
        for (CategoryResponse category : categories) {
            flat.add(category);
            if (category.subcategories() != null) {
                flat.addAll(category.subcategories());
            }
        }
        return flat;
    }

    private static Map<String, Object> objectSchema(Map<String, Object> properties, List<String> required) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "OBJECT");
        schema.put("properties", properties);
        if (required != null && !required.isEmpty()) {
            schema.put("required", required);
        }
        return schema;
    }

    private static Map<String, Object> stringProp(String description) {
        return Map.of("type", "STRING", "description", description);
    }

    private static Map<String, Object> integerProp(String description) {
        return Map.of("type", "INTEGER", "description", description);
    }

    private static Map<String, Object> numberProp(String description) {
        return Map.of("type", "NUMBER", "description", description);
    }

    private static Map<String, Object> stringEnumProp(String description, List<String> enumValues) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "STRING");
        schema.put("description", description);
        schema.put("enum", enumValues);
        return schema;
    }
}
