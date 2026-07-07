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
- Apache PDFBox (extração de texto de PDF) — próxima etapa
- Lombok + MapStruct
- OpenAPI / Swagger

**Frontend**
- React 19 + TypeScript + Vite
- shadcn/ui + Tailwind CSS
- Zustand (estado global)
- React Hook Form + Zod
- Axios (com interceptor de refresh automático)
- Recharts, TanStack Table, React Dropzone (nas próximas etapas)
**Banco de dados**
- Oracle Autonomous Database (OCI), conexão TLS sem wallet
**Infraestrutura**
- VPS Oracle Cloud (VM.Standard.A1.Flex, Ampere ARM, Ubuntu 22.04)
**IA (planejado)**
- Gemma via Google AI Studio — leitura de extratos e agente conversacional
---
 
## Metodologia: desenvolvimento spec-driven com OpenSpec
 
Cada funcionalidade nasce como uma **change** isolada: uma proposta (`proposal.md`), um design técnico com as decisões e trade-offs (`design.md`), critérios de aceite em formato Given/When/Then (`spec.md`) e uma lista de tarefas (`tasks.md`). Só depois de implementada, testada contra o banco de produção e validada manualmente, a change é arquivada e suas capacidades passam a integrar a especificação permanente do sistema.
 

---
 
## Funcionalidades entregues
 
### ✅ `setup-auth` — Autenticação e segurança
- Cadastro com senha em BCrypt
- Access token JWT (15 min) + refresh token opaco rotativo
- **Detecção de reuso de refresh token**: se um token já utilizado for reapresentado, todo o histórico de sessões daquele usuário é revogado em cascata (proteção contra roubo de token)
- Histórico de login (data, IP, sucesso/falha)
- Proteção de rotas com deny-by-default
- Rate limiting: 5 tentativas/min de login por IP, 100 requisições/min por usuário autenticado
- Frontend: sessão em memória (nunca em `localStorage`), interceptor com refresh automático em requisições concorrentes
### ✅ `setup-accounts` — Contas e carteiras
- Contas com nome, ícone e cor personalizados; tipos: corrente, poupança, carteira, corretora, cartão de crédito
- Transferências entre contas **atomicamente consistentes**, com lock pessimista e ordenação por ID para evitar deadlock
- Regra de negócio: cartão de crédito nunca fica com fatura negativa (excesso de pagamento é "travado" em zero)
- Isolamento total por usuário (tentativa de acesso a conta de outro usuário retorna 404)
- Soft delete
### ✅ `add-categories` — Categorias e regras de auto-categorização
- Categorias e subcategorias (até 2 níveis) com ícone e cor
- 5 categorias padrão provisionadas automaticamente no cadastro do usuário
- Regras de palavra-chave por categoria, com resolução por *longest-substring-match* (a keyword mais específica vence)
- Soft delete com desativação em cascata para subcategorias
### ✅ `default-category-management` — Categorias padrão configuráveis
- Migração das categorias padrão de uma lista fixa no código para uma tabela dedicada no banco (`default_categories`), permitindo ajuste sem redeploy
### ✅ `add-transactions` — Transações financeiras
- Lançamento de receitas e despesas vinculado a conta e categoria
- Atualização de saldo/fatura **atômica**, com reversão automática ao editar (troca de valor ou conta) ou excluir
- Auto-categorização por descrição via o mesmo motor de regras de palavras-chave
- Recorrências (mensal, semanal, etc.) com job diário de geração automática das próximas ocorrências
- Anexos de comprovantes (upload, download, exclusão)
- Listagem paginada com filtros por conta, categoria, período e tipo
---

## Decisões técnicas de destaque
 
- **Refresh token com detecção de reuso e revogação em cascata**, em vez de apenas expiração simples.
- **Locks pessimistas com ordenação determinística por ID** em toda operação que movimenta saldo entre duas contas, prevenindo deadlock sob concorrência.
- **Reverse-then-reapply** como padrão único para edição e exclusão de transações — garante que o saldo nunca diverge, independente de quantas vezes um lançamento é editado.
- **Auto-categorização por longest-substring-match**, evitando falsos positivos entre palavras-chave parecidas (ex: "UBER" vs "UBER EATS").
