
# /executar-fase

## Propósito
Executar o projeto de IA para Znuny usando um **fluxo estrito por fases**, com governança, revisão obrigatória e execução segura.

Claude deve:
- Ler o arquivo `TODO.md`;
- Identificar a **primeira** fase com status **“Pendente”**;
- Executar **apenas** essa fase;
- **Nunca** executar mais de uma fase por invocação.

---

## Fontes Autoritativas
- Regras do projeto: `@CLAUDE.md`
- Plano de implementação: `@docs/plans/IA.md`
- Validação do plano: `@docs/plans/validacao.md`
- Estado do projeto: `@TODO.md`
- Estrutura dos catalogo do znuny: `@docs/catalogo_znuny_completo.xlsx`
- Código de referência: repositório Znuny atual (**somente leitura**)

---

## Regras Obrigatórias

### Controle de Fase
1. Ler `TODO.md`.
2. Selecionar a fase **mais inicial** com status **“Pendente”**.
3. Executar **somente** essa fase.
4. Se não houver fases pendentes, **interromper imediatamente**.

---

### Customização Znuny
- **Nunca** modificar arquivos sob `Kernel/`.
- **Todo** código personalizado deve ficar sob `Custom/`.
- Tratar `Kernel/` como **referência somente leitura**.
- Seguir estritamente as convenções do Znuny:
    - SysConfig XML em `Custom/Kernel/Config/Files/XML`
    - Console Commands em `Admin::AI::*` e `Maint::AI::*`
    - Testes em `scripts/test`

---

### Regras de Privacidade e Classificação
- Enviar para IA **apenas**:
    - Subject completo;
    - Resumo santizado do body (200–300 caracteres, sem PII).
- **Nunca** enviar anexos.
- Aplicar **limiar de confiança** definido em `TODO.md`:
    - Se `confidence ≥ threshold_confidence` → aplicar Queue/Type;
    - Se `confidence < threshold_confidence` → **encaminhar para fila de classificação manual**.

---

### Análise Eficiente (Tokens)
- Não ingerir o repositório inteiro.
- Descobrir padrões focando em:
    - `Kernel/System/Console/Command/`
    - `Kernel/Config/Files/XML/`
    - `scripts/database/`
    - `scripts/test/`
- Abrir **somente trechos relevantes** e comparar estruturas.
- Evitar copiar/colar código do core.

---

### Implementação da Fase
Para a fase selecionada:
- Criar apenas diretórios e arquivos necessários em `Custom/`.
- Respeitar padrões arquiteturais do Znuny.
- Implementar logs, validação e tratamento de erros conforme o plano.
- Adicionar **testes mínimos**, porém representativos.

---

### SysConfig, Rebuild e Daemon
- Qualquer alteração em **SysConfig XML** ou **Event Modules**:
    - **DEVE** ser seguida de proposta de execução de:
        - `Maint::Config::Rebuild`
        - Restart do Daemon (quando aplicável)
- **Nunca** executar esses comandos automaticamente:
    - Claude deve **propor** os comandos e **aguardar autorização explícita**.

---

### Revisão de Código (Obrigatória)
- Após concluir a implementação da fase (antes de qualquer commit):
    - Executar `/code-review`.
    - Se houver apontamentos relevantes:
        - Corrigir;
        - Executar `/code-review` novamente.

---

### Pré‑Commit (Obrigatório)
Claude deve **parar** e apresentar:
- Lista de arquivos criados/modificados/deletados;
- Motivação de cada mudança;
- Resumo dos diffs (principais alterações por arquivo);
- Declaração explícita de que a fase foi concluída.

Claude deve **aguardar aprovação explícita** antes de:
- Comitar;
- Sugerir push;
- Executar comandos de manutenção.

---

### Atualização do TODO
Somente após aprovação:
- Atualizar `TODO.md` marcando a fase como **“Concluída”**;
- Realizar **commit separado** para essa atualização  
  (ex.: `chore(todo): Fase X concluída`).

---

### Relatório de Conclusão
Ao final da execução:
- Informar a fase executada;
- Resumir os resultados;
- Indicar limitações conhecidas (se houver);
- Apontar a próxima fase pendente (**sem executar**).

---

### Execução em Terminal
- Trabalhar no **workspace atual**.
- O IDE (IntelliJ/VS Code) **não influencia** o fluxo via CLI.
- Antes de rodar qualquer comando de shell:
    - Mostrar o comando;
    - Explicar o impacto;
    - Aguardar autorização.
- Em alterações amplas, preferir **commits pequenos e descritivos**.
