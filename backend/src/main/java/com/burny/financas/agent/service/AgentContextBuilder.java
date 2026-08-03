package com.burny.financas.agent.service;

import com.burny.financas.accounts.dto.AccountResponse;
import com.burny.financas.accounts.service.AccountService;
import com.burny.financas.categories.dto.CategoryResponse;
import com.burny.financas.categories.service.CategoryService;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Builds the per-request system instruction from the caller's real, current financial data
 * (design.md Decision 7 / project.md: balance, accounts, and categories are injected into the
 * system prompt on every call, never cached or hardcoded). Anything time-ranged or expensive to
 * compute (spending, budgets, goals, cash flow, investments) is a tool call instead — not
 * pre-loaded here.
 */
@Component
@RequiredArgsConstructor
class AgentContextBuilder {

    private final AccountService accountService;
    private final CategoryService categoryService;

    AgentContext build(Long userId) {
        List<AccountResponse> accounts = accountService.list(userId);
        List<CategoryResponse> categories = categoryService.list(userId);
        var balance = accountService.getConsolidatedBalance(userId).consolidatedBalance();

        StringBuilder prompt = new StringBuilder();
        prompt.append("Voce e o assistente financeiro do sistema Burny Financas. Responda SEMPRE em portugues do Brasil, ")
                .append("de forma clara e objetiva.\n\n")
                .append("Data de hoje: ").append(LocalDate.now()).append("\n")
                .append("Saldo consolidado atual do usuario: R$ ").append(balance).append("\n\n");

        prompt.append("Contas do usuario:\n");
        if (accounts.isEmpty()) {
            prompt.append("- (nenhuma conta cadastrada)\n");
        } else {
            for (AccountResponse account : accounts) {
                prompt.append("- id=").append(account.id())
                        .append(", nome=").append(account.name())
                        .append(", tipo=").append(account.type())
                        .append(account.balance() != null ? ", saldo=" + account.balance() : "")
                        .append(account.creditLimit() != null ? ", limite=" + account.creditLimit() : "")
                        .append('\n');
            }
        }

        prompt.append("\nCategorias do usuario:\n");
        if (categories.isEmpty()) {
            prompt.append("- (nenhuma categoria cadastrada)\n");
        } else {
            for (CategoryResponse category : categories) {
                prompt.append("- id=").append(category.id()).append(", nome=").append(category.name()).append('\n');
                for (CategoryResponse sub : category.subcategories()) {
                    prompt.append("  - id=").append(sub.id()).append(", nome=").append(sub.name()).append('\n');
                }
            }
        }

        prompt.append("""

                Instrucoes de comportamento:
                - Identifique se o usuario esta fazendo uma CONSULTA (pergunta sobre os dados dele) ou pedindo para \
                REGISTRAR uma transacao, e chame a funcao apropriada.
                - Para consultas sobre gastos, orcamento, metas, fluxo de caixa projetado ou investimentos, sempre \
                use as funcoes disponiveis para obter dados reais - nunca invente ou estime valores.
                - Para registrar uma transacao, chame proposeTransaction somente quando conta, tipo, valor e \
                descricao estiverem claros; caso falte alguma informacao, pergunte ao usuario antes de chamar a funcao.
                - proposeTransaction NUNCA cria a transacao de fato - ela apenas monta um rascunho que sera exibido \
                para o usuario confirmar explicitamente na interface. Deixe claro na sua resposta que voce esta \
                aguardando a confirmacao do usuario.
                - Nunca mencione ids de conta ou categoria na sua resposta em texto; use os nomes.
                """);

        return new AgentContext(prompt.toString(), accounts, categories);
    }
}
