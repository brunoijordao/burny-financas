# burny-financas

Sistema de gestão financeira pessoal multiusuário, construído com um workflow spec-driven (OpenSpec) para planejar, implementar e validar cada funcionalidade de forma isolada e documentada.

> Projeto pessoal em desenvolvimento ativo — este README é atualizado a cada nova funcionalidade entregue.

---

## Stack

**Backend**
- Java 21 + Spring Boot 3.5
- Spring Security + JWT (access token + refresh token rotativo)
- Spring Data JPA + Hibernate
- Flyway (migrations versionadas)
- Bucket4j (rate limiting)
- Apache PDFBox (extração de texto de PDF)
- Integração com Gemini/Gemma (Google AI Studio) para interpretação de extratos
- Lombok + MapStruct
- OpenAPI / Swagger

**Frontend**
- React 19 + TypeScript + Vite
- shadcn/ui + Tailwind CSS
- Zustand (estado global e de UI)
- React Hook Form + Zod
- Axios (com interceptor de refresh automático)
- Recharts (gráficos do dashboard)
- React Dropzone (upload de arquivos)

**Banco de dados**
- Oracle Autonomous Database (OCI), conexão TLS sem wallet

**Infraestrutura**
- VPS Oracle Cloud (VM.Standard.A1.Flex, Ampere ARM, Ubuntu 22.04)

---

## Metodologia: desenvolvimento spec-driven com OpenSpec

Cada funcionalidade nasce como uma **change** isolada: uma proposta (`proposal.md`), um design técnico com as decisões e trade-offs (`design.md`), critérios de aceite em formato Given/When/Then (`spec.md`) e uma lista de tarefas (`tasks.md`). Só depois de implementada, testada contra o banco de produção e validada manualmente, a change é arquivada e suas capacidades passam a integrar a especificação permanente do sistema.

Esse processo intencionalmente prioriza **validação contra o ambiente real** (Oracle Autonomous Database, não apenas H2 em memória) antes de qualquer change ser considerada concluída — incluindo testes manuais de fluxos críticos como expiração de token, transferências atômicas, integração com IA externa e isolamento entre usuários.

Para o desenho visual das telas, o projeto usa uma skill de design dedicada que força decisões de paleta, tipografia e layout intencionais para cada tela nova, evitando o "look" genérico de admin template.

---

## Funcionalidades entregues

### ✅ Autenticação e segurança
- Cadastro com senha em BCrypt
- Access token JWT (15 min) + refresh token opaco rotativo
- **Detecção de reuso de refresh token**: se um token já utilizado for reapresentado, todo o histórico de sessões daquele usuário é revogado em cascata (proteção contra roubo de token)
- Histórico de login (data, IP, sucesso/falha)
- Proteção de rotas com deny-by-default
- Rate limiting: 5 tentativas/min de login por IP, 100 requisições/min por usuário autenticado
- Frontend: sessão em memória (nunca em `localStorage`), interceptor com refresh automático em requisições concorrentes

### ✅ Contas e carteiras
- Contas com nome, ícone e cor personalizados; tipos: corrente, poupança, carteira, corretora, cartão de crédito
- Transferências entre contas **atomicamente consistentes**, com lock pessimista e ordenação por ID para evitar deadlock
- Regra de negócio: cartão de crédito nunca fica com fatura negativa (excesso de pagamento é "travado" em zero)
- Isolamento total por usuário (tentativa de acesso a conta de outro usuário retorna 404)
- Soft delete

### ✅ Categorias e regras de auto-categorização
- Categorias e subcategorias (até 2 níveis) com ícone e cor
- Categorias padrão configuráveis (tabela dedicada, sem redeploy) provisionadas automaticamente no cadastro do usuário
- Regras de palavra-chave por categoria, com resolução por *longest-substring-match* (a keyword mais específica vence)
- Soft delete com desativação em cascata para subcategorias

### ✅ Transações financeiras
- Lançamento de receitas e despesas vinculado a conta e categoria
- Atualização de saldo/fatura **atômica**, com reversão automática ao editar (troca de valor ou conta) ou excluir
- Auto-categorização por descrição via o mesmo motor de regras de palavras-chave
- Recorrências (mensal, semanal, etc.) com job diário de geração automática das próximas ocorrências
- Anexos de comprovantes (upload, download, exclusão)
- Listagem paginada com filtros por conta, categoria, período e tipo

### ✅ Importação de extrato via PDF + IA
- Upload de extrato PDF do Itaú, associado a uma conta existente
- Extração de texto no backend via Apache PDFBox
- Interpretação e categorização automática via Gemini/Gemma (Google AI Studio), com fallback para o motor de regras por palavra-chave quando a IA não sugere categoria
- Processamento assíncrono (`202 Accepted` + polling), evitando travar a thread da requisição durante a chamada à IA
- Fluxo de revisão item a item: editar, descartar ou confirmar cada transação extraída antes de criá-la de fato — nada é lançado sem confirmação explícita
- Retry sem necessidade de reenviar o arquivo em caso de falha da IA
- Rate limiting: 10 uploads/hora por usuário (retries não consomem a cota)

### ✅ Navegação em sidebar retrátil
- Layout compartilhado entre todas as páginas autenticadas, com sidebar fixa
- Recolhe/expande (modo compacto só com ícones), estado persistido entre navegações
- Destaque visual da página ativa
- Cabeçalho com identidade do usuário e logout
- Versão responsiva: menu "hamburguer" em overlay no mobile

### ✅ Dashboard
- Saldo consolidado de todas as contas, com fatura de cartão de crédito exibida separadamente (nunca somada ao saldo disponível)
- Cards por conta com saldo/fatura individual
- Totais do mês corrente: receitas, despesas, saldo líquido
- Gráfico de gastos por categoria e comparativo dos últimos 6 meses (Recharts)
- Últimas transações lançadas, com atalho para a tela completa
- Projeção simples de saldo considerando receitas/despesas futuras já lançadas no mês

### ✅ Orçamentos e metas de economia
- Orçamento mensal por categoria, com barra de progresso e alerta visual ao ultrapassar o limite
- Renovação mensal automática (job agendado) gerando um orçamento em branco por categoria a cada novo mês
- Metas de economia com valor alvo e prazo, aportes manuais registrados como ledger append-only
- Progresso percentual e projeção de ritmo (ritmo atual de aportes vs. prazo da meta)
- Histórico de metas concluídas

---

## Decisões técnicas de destaque

- **Refresh token com detecção de reuso e revogação em cascata**, em vez de apenas expiração simples.
- **Locks pessimistas com ordenação determinística por ID** em toda operação que movimenta saldo entre duas contas, prevenindo deadlock sob concorrência.
- **Reverse-then-reapply** como padrão único para edição e exclusão de transações — garante que o saldo nunca diverge, independente de quantas vezes um lançamento é editado.
- **Auto-categorização por longest-substring-match**, evitando falsos positivos entre palavras-chave parecidas (ex: "UBER" vs "UBER EATS").
- **Processamento assíncrono com polling** para a chamada de IA na importação de PDF, evitando segurar threads de requisição HTTP por dezenas de segundos.
- **Confirmação item a item obrigatória** na importação de extrato — a IA nunca lança uma transação diretamente, sempre passa por revisão humana antes de afetar o saldo real.
- **Reaproveitamento consistente de lógica de agregação**: o cálculo de gasto por categoria usado nos orçamentos é a mesma query já usada pelo dashboard, evitando dois caminhos de cálculo divergentes para o mesmo dado.