# burny-financas

Sistema de gestão financeira pessoal multiusuário, construído com um workflow spec-driven (OpenSpec) para planejar, implementar e validar cada funcionalidade de forma isolada e documentada.

🔗 **Aplicação ao vivo:** [finance.burnycompany.com](https://finance.burnycompany.com)

> Projeto pessoal em desenvolvimento ativo — este README é atualizado a cada nova funcionalidade entregue.

---

## Deploy e infraestrutura

O sistema roda em produção real, não só localmente:

- **Backend e frontend containerizados** (Docker), orquestrados via `docker-compose`
- **VPS Oracle Cloud** (VM.Standard.A1.Flex, Ampere ARM, Ubuntu 22.04), com Nginx como proxy reverso no host
- **HTTPS** via Let's Encrypt/Certbot, renovação automática
- **Banco de dados**: Oracle Autonomous Database, o mesmo usado durante todo o desenvolvimento — sem diferença de comportamento entre ambiente local e produção
- **CI/CD via GitHub Actions**: todo push na branch `main` roda a suíte de testes; se passar, o pipeline conecta na VPS via SSH e reconstrói os containers automaticamente. Código com testes quebrados nunca chega a fazer deploy.

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
- Integração com Gemini (Google AI Studio) para interpretação de extratos e para o agente conversacional
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
- Docker + Docker Compose
- Nginx (proxy reverso no host) + Let's Encrypt
- GitHub Actions (CI/CD)

---

## Metodologia: desenvolvimento spec-driven com OpenSpec

Cada funcionalidade nasce como uma **change** isolada: uma proposta (`proposal.md`), um design técnico com as decisões e trade-offs (`design.md`), critérios de aceite em formato Given/When/Then (`spec.md`) e uma lista de tarefas (`tasks.md`). Só depois de implementada, testada contra o banco de produção e validada manualmente, a change é arquivada e suas capacidades passam a integrar a especificação permanente do sistema.

Esse processo intencionalmente prioriza **validação contra o ambiente real** (Oracle Autonomous Database, não apenas H2 em memória) antes de qualquer change ser considerada concluída.

Para o desenho visual das telas, o projeto usa uma skill de design dedicada que força decisões de paleta, tipografia e layout intencionais para cada tela nova, evitando o "look" genérico de admin template.

---

## Funcionalidades entregues

### ✅ Autenticação e segurança
Login com JWT, refresh token com detecção de reuso e revogação em cascata, e rate limiting contra força bruta.

### ✅ Contas e carteiras
Múltiplas contas (corrente, cartão, corretora) com transferências atomicamente consistentes e controle de fatura de cartão de crédito.

### ✅ Categorias e regras de auto-categorização
Categorias e subcategorias com auto-categorização de transações por palavra-chave, e categorias padrão configuráveis sem redeploy.

### ✅ Transações financeiras
Lançamento de receitas e despesas com atualização de saldo sempre atômica, recorrências automáticas e anexos de comprovantes.

### ✅ Importação de extrato via PDF + IA
Upload de extrato bancário com extração de texto e categorização automática via Gemini, sempre com revisão humana antes de qualquer lançamento real.

### ✅ Navegação em sidebar retrátil
Layout compartilhado entre as páginas, com sidebar recolhível e versão responsiva para mobile.

### ✅ Dashboard
Visão consolidada de saldo, gastos por categoria e projeções, com gráficos interativos.

### ✅ Orçamentos e metas de economia
Orçamento mensal por categoria com alertas de estouro, e metas de economia com progresso e projeção de ritmo.

### ✅ Planejamento financeiro
Contas a pagar/receber, calendário de vencimentos e fluxo de caixa projetado.

### ✅ Investimentos
Carteira de ativos com cálculo de custo médio ponderado, rentabilidade e comparação com benchmarks (CDI, IBOVESPA, IPCA).

### ✅ Relatórios
Extrato, gastos por categoria e evolução patrimonial, exportáveis em PDF e Excel.

### ✅ Agente de IA conversacional
Chat que responde perguntas financeiras e lança transações por conversa, com isolamento de dados garantido pela arquitetura — não apenas por instrução de prompt.

### ✅ Configurações
Preferências de moeda e formato de data aplicadas em todo o sistema, e tela consolidada de gerenciamento das regras de auto-categorização.

---

## Decisões técnicas de destaque

- **Refresh token com detecção de reuso e revogação em cascata**, em vez de apenas expiração simples.
- **Locks pessimistas com ordenação determinística por ID** em toda operação que movimenta saldo entre duas contas, prevenindo deadlock sob concorrência.
- **Reverse-then-reapply** como padrão único para edição e exclusão de transações — garante que o saldo nunca diverge.
- **Processamento assíncrono com polling** para a chamada de IA na importação de PDF, evitando segurar threads de requisição HTTP por dezenas de segundos.
- **Confirmação item a item obrigatória** na importação de extrato — a IA nunca lança uma transação diretamente.
- **Um único serviço de efeito em saldo (`TransactionBalanceService`) reaproveitado por três fluxos diferentes**: transação manual, importação via PDF/IA e baixa de contas a pagar/receber.
- **Status derivado em vez de persistido**: "atrasado" é calculado em tempo de leitura, sem job agendado adicional.
- **Isolamento de dados do agente de IA garantido por design**: o identificador do usuário nunca é um campo que a IA pode preencher — sempre vem do token de autenticação da requisição.
- **Deploy containerizado com o backend nunca exposto diretamente à internet**: só acessível pela rede interna do Docker, atrás do Nginx.
- **Pipeline de CI/CD que nunca faz deploy de código com testes quebrados**: o job de deploy depende explicitamente do sucesso dos testes.