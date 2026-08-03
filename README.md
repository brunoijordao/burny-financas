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
- Apache PDFBox (extração de texto de PDF e geração de relatórios em PDF)
- Apache POI (geração de relatórios em Excel, streaming via SXSSFWorkbook)
- Integração com Gemini/Gemma (Google AI Studio) para interpretação de extratos e para o agente conversacional (function calling)
- Lombok + MapStruct
- OpenAPI / Swagger

**Frontend**
- React 19 + TypeScript + Vite
- shadcn/ui + Tailwind CSS
- Zustand (estado global e de UI)
- React Hook Form + Zod
- Axios (com interceptor de refresh automático)
- Recharts (gráficos do dashboard, fluxo de caixa e carteira de investimentos)
- React Dropzone (upload de arquivos)

**Banco de dados**
- Oracle Autonomous Database (OCI), conexão TLS sem wallet

**Infraestrutura**
- VPS Oracle Cloud (VM.Standard.A1.Flex, Ampere ARM, Ubuntu 22.04)

---

## Metodologia: desenvolvimento spec-driven com OpenSpec

Cada funcionalidade nasce como uma **change** isolada: uma proposta (`proposal.md`), um design técnico com as decisões e trade-offs (`design.md`), critérios de aceite em formato Given/When/Then (`spec.md`) e uma lista de tarefas (`tasks.md`). Só depois de implementada, testada contra o banco de produção e validada manualmente, a change é arquivada e suas capacidades passam a integrar a especificação permanente do sistema.

Esse processo intencionalmente prioriza **validação contra o ambiente real** (Oracle Autonomous Database, não apenas H2 em memória) antes de qualquer change ser considerada concluída — incluindo testes manuais de fluxos críticos como expiração de token, transferências atômicas, integração com IA externa, cálculo de custo médio ponderado, exportação de arquivos reais e isolamento entre usuários.

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

### ✅ Planejamento financeiro
- Contas a pagar e a receber futuras, com data de vencimento, valor, categoria e conta associada
- Status derivado em tempo de leitura (pendente, pago/recebido, atrasado) — sem depender de job agendado para marcar um lançamento como vencido
- Baixa (settle) de um lançamento gera uma transação real via o mesmo `TransactionBalanceService` já usado em contas e na importação de PDF, sem duplicar lógica de efeito em saldo
- Desfazer baixa (undo) reverte o efeito no saldo e desvincula a transação, sem deixar resíduo
- Calendário financeiro mensal com os vencimentos distribuídos por dia
- Alertas visuais para vencimentos próximos e lançamentos atrasados
- Fluxo de caixa projetado somando saldo atual aos lançamentos pendentes (diferente da projeção simples do dashboard, que considera só o que já foi lançado)

### ✅ Investimentos
- Cadastro de ativos (ações, FIIs, CDB, Tesouro Direto, criptomoedas), com vínculo opcional e somente-leitura a uma conta existente (ex: corretora)
- Registro de aportes (compra) e resgates (venda), com **custo médio ponderado** calculado a partir do histórico completo de operações — validado manualmente com múltiplas compras em preços diferentes
- Resgate parcial reduz apenas a quantidade, preservando o preço médio das unidades remanescentes
- Valorações datadas (ledger de valor de mercado informado manualmente), permitindo reconstituir a evolução do patrimônio ao longo do tempo sem depender de integração externa de cotação
- Rentabilidade por ativo e por carteira, com comparação manual a benchmarks (CDI, IBOVESPA, IPCA)
- Distribuição percentual da carteira por tipo de ativo e gráfico de evolução do patrimônio (Recharts)
- Módulo desacoplado do saldo de contas bancárias: aportes/resgates nunca alteram o saldo de nenhuma `Account` — validado explicitamente mantendo o saldo da conta vinculada em R$ 0,00 durante todo o teste

### ✅ Relatórios
- Três tipos de relatório: extrato detalhado por período, gastos por categoria e evolução patrimonial
- Módulo de composição pura, sem entidade própria — reaproveita `TransactionRepository`, `AccountService` e `InvestmentPortfolioService` já existentes, sem duplicar cálculo
- Preview em tela (JSON) separado da exportação de arquivo, permitindo conferir os dados antes de baixar
- Exportação em PDF via Apache PDFBox (reaproveitando a dependência já existente do projeto, evitando a licença AGPL de bibliotecas alternativas)
- Exportação em Excel via Apache POI com escrita em streaming (`SXSSFWorkbook`), evitando estourar memória em relatórios grandes
- Conteúdo dos arquivos exportados validado byte a byte contra os dados reais do preview
- Isolamento total por usuário em todos os tipos de relatório

### ✅ Agente de IA conversacional
- Chat em linguagem natural (`/assistant`) com acesso a dados financeiros reais do usuário, via function calling nativo do Gemini (`gemini-2.5-flash`)
- Consultas: saldo, gastos por categoria, status de orçamento, progresso de metas, fluxo de caixa projetado, resumo de investimentos — cada uma mapeada 1:1 a um service já existente, sem duplicar lógica de negócio nem acessar o banco diretamente
- Inserção de transações por conversa, sempre no modelo **propor → confirmar**: o agente nunca cria uma transação diretamente — ele retorna um rascunho, que só vira transação real após confirmação explícita do usuário em um endpoint separado, revalidado do zero como se fosse preenchido manualmente
- **Isolamento por usuário garantido estruturalmente, não por instrução de prompt**: nenhum schema de tool exposto ao modelo contém um campo de identificação de usuário — o `userId` real sempre vem do token de autenticação da requisição HTTP, nunca de nada que o modelo gere, tornando impossível o agente acessar dados de outro usuário mesmo em caso de alucinação do modelo
- Loop de function calling limitado a 3 chamadas por turno, com resposta de fallback caso o modelo não conclua
- Contexto financeiro (saldo, contas, categorias) injetado no system prompt a cada mensagem; consultas mais custosas (gasto por período, projeções) ficam como tools sob demanda, não pré-carregadas
- Histórico de conversa limitado tanto no frontend quanto no backend, evitando custo crescente por turno
- Configuração de modelo e cliente HTTP totalmente independente da integração de IA já usada na importação de PDF
- Rate limiting: 30 mensagens/hora por usuário

---

## Decisões técnicas de destaque

- **Refresh token com detecção de reuso e revogação em cascata**, em vez de apenas expiração simples.
- **Locks pessimistas com ordenação determinística por ID** em toda operação que movimenta saldo entre duas contas, prevenindo deadlock sob concorrência.
- **Reverse-then-reapply** como padrão único para edição e exclusão de transações — garante que o saldo nunca diverge, independente de quantas vezes um lançamento é editado.
- **Auto-categorização por longest-substring-match**, evitando falsos positivos entre palavras-chave parecidas (ex: "UBER" vs "UBER EATS").
- **Processamento assíncrono com polling** para a chamada de IA na importação de PDF, evitando segurar threads de requisição HTTP por dezenas de segundos.
- **Confirmação item a item obrigatória** na importação de extrato — a IA nunca lança uma transação diretamente, sempre passa por revisão humana antes de afetar o saldo real.
- **Reaproveitamento consistente de lógica de agregação**: o cálculo de gasto por categoria usado nos orçamentos, no dashboard e nos relatórios é a mesma query, evitando caminhos de cálculo divergentes para o mesmo dado.
- **Um único serviço de efeito em saldo (`TransactionBalanceService`) reaproveitado por três fluxos diferentes**: lançamento manual de transação, confirmação de item importado via PDF/IA e baixa de contas a pagar/receber — nenhum desses caminhos duplica a regra de negócio de débito/crédito.
- **Status derivado em vez de persistido**: "atrasado" é calculado comparando data de vencimento com a data atual no momento da consulta, evitando mais um job agendado no sistema e eliminando qualquer risco de status desatualizado.
- **Posição de investimento sempre recalculada a partir das operações**, nunca armazenada como campo isolado — evita divergência entre a posição exibida e o histórico real de compras/vendas.
- **Módulo de investimentos deliberadamente desacoplado do saldo de contas bancárias** nesta fase do projeto, evitando integração automática prematura entre dois domínios que ainda podem evoluir de forma independente.
- **Escolha de biblioteca de PDF orientada por licença**: reaproveitar o PDFBox (Apache-2.0) já presente no projeto em vez de adicionar uma nova dependência com licença AGPL, evitando obrigações de código aberto indesejadas em um eventual uso comercial.
- **Isolamento de dados do agente de IA garantido por design, não por confiança no modelo**: o identificador do usuário nunca é um campo que a IA pode preencher — é sempre extraído do contexto de autenticação da requisição, tornando estrutural (e não apenas uma instrução de prompt) a garantia de que o agente nunca acessa dados de outro usuário.
- **Escritas via IA seguem sempre o padrão propor → confirmar em endpoints separados**: o modelo nunca tem acesso a uma tool que persiste dados diretamente; a confirmação é uma requisição autenticada comum, revalidada com as mesmas regras de uma transação criada manualmente.