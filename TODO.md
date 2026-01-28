# Pipeline de Classificação de Tickets com IA

## Entrada
- Subject + Resumo sanitizado do body (200–300 chars; sem PII).
- Classificar **TIPO** (REQ/INC/OS), **DOMÍNIO** e **SERVIÇOS** (score).

## Seleção
- Se `confidence_score ≥ threshold_confidence` → aplicar `serviço_final` e derivar fila/SLA.
- Se `confidence_score < threshold_confidence` → mover para `fallback_queue` (classificação manual).

## Restrições e Segurança
- Remetente mascarado (ex.: `c****@empresa.com`).
- Sem anexos para a IA.

## Logs, Validação e Erros
- `correlation_id` por processamento.
- Registrar decisão (`applied | partial | manual | not_applied`) e tempos.

## Testes Mínimos
- Confiança alta (≥ limiar) → aplicado.
- Confiança baixa (< limiar) → manual (`fallback_queue`).
- Erro recuperável → retry/backoff curto e log apropriado.

---

## Fase 1 – Critérios de Aceite
- SysConfig disponível na navegação; `Maint::Config::Rebuild` sem erros.
- Tabela de log existente e gravando registros.
- Pipeline aplica/encaminha conforme limiar e registra status/tempos.
- `/code-review` executado sem apontamentos críticos pendentes.

---

## Fase 2 – OpenAI Client e RAG Engine (Concluída)
**Objetivo:** Implementar clientes HTTPS com robustez.

### Entregas Mínimas
- `OpenAIClient.pm` com HTTPS, autenticação segura e retry/backoff/circuit breaker.
- `RAGEngine.pm` (Qdrant) para busca/index; HTTPS no Cloud ou TLS no ingress (self-host).
- Comandos `Maint::AI::ReindexRAG` e `Maint::AI::ProcessTicket`.

### Critérios de Aceite
- Requisições HTTPS funcionais; tratamento de erros adequado.
- KPIs básicos de chamadas e reindex.

---

## Fase 3 – ClassificationProcessor (Concluída)
**Objetivo:** Orquestração com heartbeat.

### Entregas Mínimas
- `ClassificationProcessor.pm` com checkpoints `TaskLockUpdate()`.
- Compensação: reverter Queue se Type falhar.
- Logs completos em `ai_ticket_classification_log`.

### Entregas Realizadas
- Pipeline de 8 estágios: Context → Sanitize → RAG → AI → Validate → Apply → Log → Index.
- Heartbeat via `TaskLockUpdate()` configurável.
- Compensação ativa para falhas de Type.
- SysConfig: `EnableCompensation`, `HeartbeatEnabled`, `IndexAfterApply`, `MaxBatchSize`.
- Testes unitários em `scripts/test/Ticket/AI/ClassificationProcessor.t`.

---

## Fase 4 – Event Handler (Concluída)
**Objetivo:** Disparo automático de classificação.

### Entregas Mínimas
- `AITicketClassifier.pm` herdando de `AsynchronousExecutor`.
- Registro `Ticket::EventModulePost###...` (Transaction=1 quando aplicável).
- `Maint::Config::Rebuild` e restart do Daemon.

### Entregas Realizadas
- `AITicketClassifier.pm` em `Custom/Kernel/System/Ticket/Event/`.
- Processamento assíncrono via `AsyncCall()`.
- Loop protection (padrão Znuny).
- Registro `Ticket::EventModulePost###100-AITicketClassifier` com `Transaction=1`.
- Configurações: `ExcludedQueues` (array), `SkipIfTypeSet` (checkbox).
- Testes unitários em `scripts/test/Ticket/Event/AITicketClassifier.t`.
- Code review com correções aplicadas.

**Nota:** Executar `Maint::Config::Rebuild` e restart do Daemon em produção/homologação.

---

## Fase 5 – Testes (Concluída)
**Objetivo:** Validação funcional, integração e performance com mocks.

### Entregas Mínimas
- Unitários (≥ 80% cobertura), integração (50–100 tickets), performance (100 simultâneos).
- Mocks para OpenAI/Qdrant.

### Entregas Realizadas
- `MockProvider.pm` com simulação de erros, latência e tracking.
- Testes unitários: 85+ casos.
- Integração: 60 tickets (INC, REQ, OS, baixa confiança) + batch + RAG.
- Performance: 100 tickets sequenciais (P95, throughput, benchmarks).
- Error handling: circuit breaker, validação, retry, graceful degradation.
- Code review com correções aplicadas.

### Arquivos de Teste
- `scripts/test/Ticket/AI/MockProvider.t` (15 casos)
- `scripts/test/Ticket/AI/Integration.t` (60+ tickets)
- `scripts/test/Ticket/AI/Performance.t` (100 tickets + benchmarks)
- `scripts/test/Ticket/AI/ErrorHandling.t` (14 casos)

**Nota:** Usar `RestoreDatabase` e modo Mock. Execução completa via Docker:
`bin/znuny.Console.pl Dev::UnitTest::Run --directory Ticket/AI`.

---

## Fase 6 – Análise de Sentimentos (Concluída)
- Algoritmo lexicon-based no body.
- Aumento de severidade em caso de urgência/criticidade.
- Implementado: `SentimentAnalyzer.pm`, integração no pipeline, logs e testes.

---

## Fase 7 – Seleção do Motor de IA (Concluída)
- UI para seleção (provider/model/chave).
- “Testar Conexão”.
- Segurança da chave.
- Apenas admin pode modificar a chave.

---

## Fase 8 – Compatibilidade com Prompt Embutido (Concluída)
- Classificação sem RAG por padrão.
- Prompt com contexto embutido (`docs/plans/Prompt.md`) descrevendo todos os serviços.

### Entregas Realizadas
- `PromptBuilder.pm` com catálogo CAESB (50+ serviços).
- `Ticket::AI::ClassificationMode`: `EmbeddedPrompt` (padrão), `RAG`, `Hybrid`.
- Integração automática com `OpenAIClient`.
- Parser atualizado (tipo/servico_id/servico_nome).
- Testes unitários em `scripts/test/Ticket/AI/PromptBuilder.t` (~85% cobertura).

**Nota:** `EmbeddedPrompt` é padrão e não requer Qdrant.

---

## Fase 9 – Dashboard (Visualização) (Concluída)
**Objetivo:** Métricas, KPIs e análises no Admin UI.

### Entregas Mínimas
- Menu Dashboard em Admin > Core::Ticket::AI.
- Gráfico “Quantidade por Serviço” com filtros.
- KPIs: total, % aplicado/manual, tempo médio; Top erros.
- Respostas rápidas sem PII.

### Entregas Realizadas
- `AdminAIDashboard.pm` com agregações da `ai_ticket_classification_log`.
- `AdminAIDashboard.tt` com filtros, KPIs, top erros e D3.js.
- `AdminAIDashboard.xml` para navegação.
- Filtros: período, serviço, tipo, status, confiança mínima.
- KPIs calculados e gráfico horizontal (top 20).
- Navegação: Admin > Ticket (ícone `fa-chart-bar`).
- Requer `Maint::Config::Rebuild`.

---

## Fase 10 – Complemento de Dados (Pendente)
- Obter dados do usuário via remetente do e-mail.
- Buscar logon na VIEW de usuários.
- Capturar: lotação, contato e matrícula.

---

## Fase 11 – Observabilidade e Enterprise (Pendente)
- Métricas Prometheus; alertas; SLOs.

---

## Regras Operacionais (Resumo)
- Apenas uma fase por execução.
- Pré-commit obrigatório: listar arquivos, justificar mudanças, mostrar diffs e aguardar aprovação.
- Atualização de status: marcar fase como Concluída somente após aprovação e em commit separado
  (ex.: `chore(todo): Fase X concluída`).
