# Redesign neobrutalista do BrainOut — plano de implementação

> **Para o agente de IA:** REQUIRED SUB-SKILL: use `executing-plans` para executar este plano tarefa por tarefa. Esta entrega é o planejamento; a implementação começa por NB-01.

**Goal:** transformar o BrainOut na identidade “Bloco de ação” — impacto visual marcante — e ampliar o produto com tarefas globais acionáveis, kanban, agenda de prazos, biblioteca de tags, criação rápida e membership local, mantendo offline-first, acessibilidade e desempenho medido.

**Architecture:** tokens e componentes em `:core:ui` envolvendo Material 3 com tema neobrutalista aditivo; migração de rotas progressiva com ativação global no fim. Capacidades novas vivem em `:core:domain`/`:core:data` (membership, semântica de prazo, CRUD de tags, regras de DONE) e são expostas por ViewModels imutáveis + componentes data/callback. Integração remota inalterada: capacidades novas são locais e avisam “só neste dispositivo” quando necessário.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, Navigation Compose, Hilt, Room (com `MIGRATION_4_5`), WorkManager; Foundation/Animation para desenho e motion; JUnit 4, Truth, Turbine, Robolectric e Compose UI Test (Robolectric-hosted nos fluxos críticos). Fontes e vetores locais. Sem biblioteca visual nova obrigatória.

---

## 1. Base, objetivo e decisões confirmadas

- **Data:** 01/10/2026. **Status:** planejado; nenhuma tarefa de implementação concluída por este documento. Esta versão incorpora as decisões Q1–Q30 confirmadas pelo usuário (“confirmado”) e substitui as recomendações da versão anterior.
- **Base inspecionada:** `4dcfa0a57fddd9d987b352914396bac1fd02c59c`. **Auditada linha a linha em 02/10/2026** contra `5b483ba` (ver seção 14): as referências de arquivo/linha foram conferidas uma a uma e as divergências estão listadas lá.
- **Especificação de referência:** [DESIGN.md](../../DESIGN.md), [componentes e fluxos](../design/ESPECIFICACAO.md), [pesquisa de bibliotecas](../design/PESQUISA.md) e [tokens](../design/tokens.json).
- **Rastreabilidade acadêmica:** [roadmap existente](../ROADMAP.md). Este plano é uma iteração nova de redesign/ampliação; não reabre entregas marcadas como concluídas (inclusive E4.8 e `v1.0.0-rc`).
- **Execução:** um desenvolvedor; entregas intermediárias ao usuário **não** são previstas (decisão Q11: entrega final única). PRs pequenos existem para revisão/CI, não para builds de demonstração. Estimativas são de esforço, não datas (Q3: sem prazo fixo).

### 1.1 Decisões confirmadas (Q1–Q30)

| ID | Decisão confirmada |
|---|---|
| Q1 | Impacto visual é o critério de sucesso principal: transformação visual marcante |
| Q2 | Novas funcionalidades podem (e devem) entrar; não é apenas restyle |
| Q3 | Sem prazo fixo; “quanto levar” é o esforço necessário |
| Q4 | Identidade “Bloco de ação”, paleta A, Archivo + Public Sans aprovadas como estão; refinar só a execução |
| Q5 | Celular e tablet adaptativo são ambos obrigatórios |
| Q6 | Plano técnico completo: contratos de componentes/estados/eventos, fixtures e roteiros detalhados de validação |
| Q7 | Escopo funcional A+B+C+D+E: tarefas globais acionáveis + busca/filtros/ordenação; kanban por projeto; agenda de prazos; CRUD completo de tags com criação independente; criação rápida de tarefa |
| Q8 | Integração remota **inalterada**: deadline, distinção TODO/DOING, responsável, rename/recore de tag e desassociação são locais; backend continua stub |
| Q9 | Ilustrações geométricas (blocos/cartas/setas), sem mascote |
| Q10 | Arte geométrica interativa (explorável + reativa a ações reais) |
| Q11 | Entrega final única; sem builds intermediários para o usuário |
| Q12 | Membership local: Owner vincula contas Member locais a projetos (email = id de login único); permissões aplicadas no domínio/use case, não só escondendo UI |
| Q13 | Cinco destinos de topo: Projetos, Tarefas, Agenda, Dashboard, Configurações (bottom bar no telefone, rail no tablet); kanban dentro do detalhe do projeto; biblioteca de tags na área de Projetos |
| Q14 | Editor/detalhe de tarefa compartilhado, alcançado pela lista global, kanban, agenda e criação rápida (com seletor de projeto) |
| Q15 | Kanban com drag-and-drop **e** comandos acessíveis equivalentes |
| Q16 | Agenda com visões mês, semana e dia |
| Q17 | Prazo: data obrigatória + hora opcional; sem hora = dia inteiro |
| Q18 | Biblioteca de tags independente (tags podem existir sem associação; exibir contagem de uso por tag) |
| Q19 | Arte: reativa a ações reais **e** pequena composição explorável (splash); não altera dados do usuário |
| Q20 | Matriz de papéis estrita aplicada no domínio: Owner administra projeto, participantes e biblioteca de tags; Member cria/edita/move tarefas, mas não exclui projeto/tarefa nem gerencia tags/participantes |
| Q21 | Tarefas DONE: editáveis para título/prazo/responsável; **não** para status/prioridade enquanto DONE (prioridade de DONE segue bloqueada) |
| Q22 | Ordenação nas colunas do kanban: prioridade decrescente (enum `TaskPriority`: CRITICAL → URGENT → HIGH → MEDIUM → LOW) e depois mais antigas primeiro |
| Q23 | Prazo/lembretes: dia inteiro = fim do dia local 23:59; com hora = instante exato; lembrete 24h antes (dia inteiro) ou 1h antes (com hora), recalculado em **toda** edição, inclusive mudança de status; fuso = fuso atual do dispositivo; deadlines existentes à meia-noite são reclassificados como dia inteiro (meia-noite → fim do dia) na primeira carga |
| Q24 | Avisos contextuais discretos “só neste dispositivo” nos editores de tarefa/tag (dados locais); banners de sync existentes permanecem |
| Q25 | Filtros da lista global (projeto, status, prioridade, período de prazo) + ordenação (prazo, prioridade, criação, título) persistidos por usuário, como as preferências da Home |
| Q26 | Gate de aceite: matriz CI/emulador + **2 dispositivos físicos** (preferir um 60 Hz e um 120 Hz) checando press/sombras/IME/fonte 2.0/dark/tablet; sessões de usabilidade **não** bloqueiam (evidência M4 pode ficar parcial sem usuários) |
| Q27 | Desempenho: procedimento manual reproduzível (`am start -W` + `dumpsys gfxinfo`/`framestats`) documentado em `docs/`, mesmo build antes/depois (release sem assinatura local ou artefato CI); Macrobenchmark só se regressão for encontrada |
| Q28 | Arte em splash/estados vazios/conclusão apenas, 1–2 elementos interativos, assets ≤ 1 MB comprimidos, fallback estático quando animações desativadas |
| Q29 | Gate de testes: domain/data sempre testados (Kover 60% em core domain/data), ViewModels com Turbine, Compose UI Test apenas nos fluxos críticos (kanban, editor, agenda); sem golden/snapshot |
| Q30 | Publicação inicial: PR só de docs (DESIGN.md + docs/design/ + docs/plans/) a partir de `origin/main`, sem tocar os 4 commits preexistentes do `main` local; a publicação deles fica com o usuário. **Concluída em 02/10/2026** (PR #80): `main` local e `origin/main` estão idênticos em `5b483ba`, então a ressalva sobre os 4 commits deixou de se aplicar |

| Q31 | **Correção de dependência (02/10/2026):** NB-03 (pilotos) sai de F0 e passa para F2, imediatamente após NB-11, porque o piloto usa a galeria de componentes de NB-11 — como estava escrito, F0 não fechava antes de F2. F0 passa a ser só NB-01 e NB-02. Consequência em cadeia: NB-04 deixa de depender de NB-03 e passa a depender de NB-02 (a fonte dos tokens é `DESIGN.md` + `tokens.json`, auditados em F0), senão F1 → NB-11 → NB-03 → F1 fecharia um ciclo |

### 1.2 Incluído na entrega principal

1. Identidade visual completa (paleta A, claro/escuro, Archivo/Public Sans, ícones locais, sombras rígidas, motion com duration scale).
2. Shell adaptativo com cinco destinos (bottom bar < 600 dp, rail ≥ 600 dp) e todos os estados existentes.
3. Capacidades novas confirmadas: tarefas globais acionáveis com busca/filtros/ordenação persistidos, kanban por projeto (DnD + comandos), agenda mês/semana/dia, biblioteca de tags (criar/renomear/recolorir/excluir/associação), criação rápida, editor compartilhado, membership local com permissões de domínio.
4. Semântica nova de prazo (dia inteiro 23:59 / hora opcional) com recálculo de lembretes e reclassificação de dados legados.
5. Regras de DONE editável (Q21) e avisos “só neste dispositivo” (Q24).
6. Arte geométrica interativa em splash/vazios/conclusão com fallback estático.
7. Correção do fechamento prematuro do diálogo de criação de projeto.
8. QA com matriz CI/emulador + 2 dispositivos físicos, desempenho comparado e documentação/evidências.

### 1.3 Fora desta entrega (backlog)

Paletas B/C selecionáveis; autenticação/remota e sync ampliado (deadline/TODO-DOING/tags/participants no servidor); captura sem projeto (“inbox”); recorrência de tarefas; fotos/avatares remotos; novos gráficos (Vico); painéis lista/detalhe simultâneos ≥ 840 dp; sync de preferências de visualização; import/refresh de dados remotos; Macrobenchmark (exceto se regressão); sessões de usabilidade formais.

## 2. Contratos funcionais e de dados

Capacidades novas exigem mudanças em `:core:domain`/`:core:data`. Cada uma vira tarefa funcional com testes próprios (F3). O contrato de rede **não** muda (Q8).

### 2.1 Membership local (Q12, Q20)

- **Entidade nova:** `ProjectMemberEntity(projectId, userId, role)` → tabela `project_members`, PK `(project_id, user_id)`, FK para `projects(id)` com cascade; `role` reutiliza `UserRole`. `MIGRATION_4_5`.
- **Repositório:** `ProjectMembershipRepository { observeMembers(projectId), addMember(projectId, userEmail), removeMember(projectId, userId), observeAccessibleProjectIds(userId) }`.
- **Use cases novos:** `AddProjectMemberUseCase`, `RemoveProjectMemberUseCase`. `CanPerformActionUseCase` passa a ser chamado por **todos** os use cases de mutação recebendo o usuário ativo (`Create/Update/DeleteProject`, `Create/Update/DeleteTask`, `ChangeTaskStatusUseCase`, operações de tag e membership).
- **Erros de domínio novos:** `PermissionDeniedException`, `UserNotFoundException`, `AlreadyMemberException` em `core.domain.error`.
- **Matriz efetiva:** Owner cria/edita/exclui projeto, gerencia participantes e biblioteca de tags, CRUD completo de tarefas; Member cria/edita/move tarefas do projeto, não exclui projeto/tarefa e não gerencia tags/participantes. Status é edição (Member pode mover); exclusão é só Owner.
- **Visibilidade:** Home e lista global passam a incluir projetos com `owner_id = eu` **ou** membership ativo (`HomeListingPipeline`, `TaskDao` com join em `project_members`). Hoje Member não enxerga nada — isso é corrigido aqui.
- **Aviso local (Q24):** tela de participantes informa que o vínculo é “só neste dispositivo”.

### 2.2 Semântica de prazo e lembretes (Q17, Q23)

- **Modelo:** `Task.dueDate: Instant?` permanece; nova coluna `deadline_all_day INTEGER NOT NULL DEFAULT 0` em `tasks` (`MIGRATION_4_5`) e campo `dueDateAllDay: Boolean` no domínio.
- **Gravação:** dia inteiro → `dueDate` = 23:59:00 local do dia escolhido (fuso do dispositivo no momento da gravação), flag `1`; com hora → instante exato, flag `0`.
- **Exibição:** dia inteiro mostra a data sem hora (“hoje/amanhã/23 mai”); com hora mostra data + hora. Fuso sempre o atual do dispositivo; trocar de fuso pode mudar o dia exibido (comportamento documentado, sem fuso congelado).
- **Lembretes:** gatilho = `dueDate − 24h` (dia inteiro) ou `dueDate − 1h` (com hora), somente se futuro. Reconciliação executada em **toda** mutação de tarefa: `CreateTaskUseCase`, `UpdateTaskUseCase`, `ChangeTaskStatusUseCase` e exclusão cancela. **Auditoria 02/10/2026:** `ChangeTaskStatusUseCase` **já** chama `reconcileReminder` (l. 68 e 85) — não há gap atual de lembrete obsoleto a corrigir; o que falta é a revalidação do gatilho no `DeadlineWorker` e a semântica nova de dia inteiro.
- **`DeadlineWorker`** passa a revalidar antes de publicar: tarefa inexistente/DONE/sem prazo/pref desligada/permissão negada/**gatilho não corresponde mais ao prazo atual** (auto-correção de agendamento obsoleto).
- **Reclassificação legada (Q23):** normalização idempotente executada na primeira carga após upgrade: `dueDate` à meia-noite local → dia inteiro (23:59 + flag). Testes cobrem detecção de meia-noite no fuso do dispositivo.
- **Aviso local (Q24):** editor de tarefa exibe “prazo visível só neste dispositivo”.

### 2.3 Regras de DONE (Q21)

- `UpdateTaskUseCase`: para tarefa persistida DONE, aceita alterações de `title`, `dueDate`/`dueDateAllDay` e `assigneeId`; **rejeita** mudança de `priority` e mudança direta de `status` (status só via `ChangeTaskStatusUseCase`, matriz `TaskStatus` preservada: DONE → DOING como reabertura; DONE → TODO segue inválido).
- `Task.changePriority` em DONE permanece bloqueado no domínio (prioridade de DONE segue bloqueada — regra existente mantida).
- UI: no editor compartilhado, controles de status/prioridade ficam desabilitados quando a tarefa é DONE; ações de reabrir/concluir continuam nos menus válidos.

### 2.4 Biblioteca de tags (Q18, Q20)

- **API nova em `TagRepository`:** `create(name, color): Tag` (persiste tag independente do proprietário ativo), `update(id, name?, color?)` (rename/recore), `observeTagsWithUsage(): Flow<List<TagWithUsage>>` (`Tag` + `projectCount`).
- **Unicidade:** `(owner_id, name)` case-sensitive (comportamento atual); colisão vira `DuplicateTagNameException` de domínio (mapeando a constraint SQLite).
- **Exclusão:** continua global para a identidade (remove associações); **corrige** `selected_tag_id` órfão nas preferências de listagem ao excluir (bug atual: Home pode ficar vazia — `HomeListingPipeline.kt:174-178`).
- **Associação a projeto existente:** `ProjectRepository.update` com `replaceProjectTags` (capacidade existente). Owner edita associações; Member não.
- **Papéis:** biblioteca mostra tags do usuário ativo; Owner cria/edita/exclui; Member apenas usa tags já existentes. Contrato a validar em NB-12/NB-15.
- **Sync (Q8):** CREATE/DELETE viajam (comportamento atual). **Rename/recore/desassociação são locais** (dispatcher descarta TAG UPDATE e só adiciona associações) → aviso “só neste dispositivo”. Não enfileitar operação sabidamente descartada.

### 2.5 Editor compartilhado de tarefa (Q14, Q21)

- **Rota:** `task_editor?taskId={taskId}&projectId={projectId}` + helper `taskEditorRoute(taskId = null, projectId = null)`. `taskId = null` ⇒ criação; `projectId` pré-selecionado quando vindo do contexto do projeto, seletor quando vindo da criação rápida.
- **Estado:** `TaskEditorUiState(title, priority, status, dueDate, dueTime, dueDateAllDay, assigneeId, isNew, saving, fieldErrors, actionError, localOnlyNotice, projects, canEditStatus, canEditPriority, canDelete)`.
- **Eventos (Channel):** `Saved(taskId)`, `Deleted`, `Failed` (opcional, erro também em `actionError`).
- **Regras:** uma submissão por vez (`saving` bloqueia novo submit e fechamento); falha preserva rascunho; erro de ação não apaga conteúdo; confirmação explícita para exclusão; validação de título (1–200) e prazo (comportamento atual de feriado/check preservado).
- **Atalho de criação rápida (Q7E):** `QuickCreateTaskSheet` (bottom sheet) com título, seletor de projeto, prioridade padrão MEDIUM, data+hora opcionais, responsável opcional; aciona `CreateTaskUseCase`; sucesso mostra feedback e atualiza listas.

### 2.6 Tarefas globais acionáveis (Q7A, Q25)

- **Estado estendido:** `TasksUiState(tasks, search, filters(projectId, status, priority, duePeriod), sort, loading, readError, actionError)`.
- **Filtros:** projeto (incl. “todos”), status, prioridade, período de prazo (todas, atrasadas, hoje, próximos 7 dias, sem prazo). **Ordenação:** prazo (sem prazo por último), prioridade decrescente, criação decrescente, título. Busca por título (case-insensitive, filtro em memória sobre a Flow observada).
- **Persistência:** chaves novas em `ListingPreferencesRepository` por usuário, preservadas no logout (mesmo mecanismo da Home).
- **Callbacks por linha:** abrir projeto, abrir editor, mudar status (menu de transições válidas), excluir (somente Owner, com confirmação).
- **Ordenação da lista:** `observeAllForOwner` passa a considerar projetos acessíveis (owner + membership).

### 2.7 Kanban por projeto (Q7B, Q15, Q22)

- **Localização:** dentro do detalhe do projeto; controle segmentado `Lista | Kanban` persistido por usuário (chave de preferência de listagem).
- **Colunas fixas:** TODO, DOING, DONE; cartões ordenados por `priorityCode` decrescente (CRITICAL → LOW) e depois `createdAt` crescente.
- **Drag-and-drop:** implementação própria com `pointerInput` entre colunas; drop válido chama `ChangeTaskStatusUseCase`; transição inválida mostra mensagem e devolve o cartão. Risco e fallback na seção 11. **Auditoria 02/10/2026:** a premissa «Compose 1.7.5 não traz DnD de lazy list» **não é decidível pelo POM** (que só publica versões); conferir a API resolvida no início de NB-22 e manter os comandos acessíveis como caminho garantido.
- **Comandos acessíveis (obrigatórios, Q15):** menu por cartão “Mover para …” listando apenas transições válidas da matriz `TaskStatus`; operável por teclado/TalkBack/Switch Access.
- **DONE:** sem mudança de prioridade no cartão; comando de reabrir = DONE → DOING. Conclusão de TODO via use case encadeia TODO → DOING → DONE (comportamento existente preservado).
- **Chaves estáveis** por `task.id`; sem animação de lista inteira.

### 2.8 Agenda (Q7C, Q16, Q17)

- **Rota nova:** `agenda` (top-level). Visões **Mês | Semana | Dia** (controle segmentado; padrão Mês).
- **Agrupamento:** tarefas com prazo agrupadas pela data local de `dueDate` (dia inteiro 23:59 cai no próprio dia); seção “Atrasadas” sempre visível; dia atual destacado.
- **Dados:** nova consulta `TaskDao.observeDueBetween(start, end)` sobre projetos acessíveis; janelas limitadas por visão (mês/semana/dia) sem carregar tudo.
- **Interações:** toque no dia abre a lista do dia; toque na tarefa abre o editor compartilhado; botão “criar tarefa neste dia” abre a criação rápida com data pré-preenchida. Dia sem tarefas mostra estado vazio explícito.
- **Sem fuso congelado:** exibição usa fuso atual do dispositivo (contrato 2.2).

### 2.9 Verdade dos dados locais e offline (Q8, Q24)

| Capacidade | Local | Viaja no sync | Aviso na UI |
|---|---|---|---|
| Projeto/tarefa criar/editar/excluir/status | sim | sim (payload atual) | — |
| Tag criar/excluir | sim | sim (CREATE/DELETE) | — |
| Prazo (data/hora) | sim | não | “só neste dispositivo” no editor |
| Distinção TODO/DOING | sim | não (`done` boolean) | “só neste dispositivo” no editor |
| Responsável (`assigneeId`) | sim | não | “só neste dispositivo” no editor |
| Tag rename/recore | sim | não (UPDATE descartado) | “só neste dispositivo” na biblioteca |
| Desassociação tag↔projeto | sim | não (dispatcher só adiciona) | “só neste dispositivo” no editor de projeto |
| Membership/participantes | sim | não (sem endpoint) | “só neste dispositivo” na tela de participantes |

Tabela de conectividade/fila existente preservada (offline+0, offline+N, online+N, online+0 — sem afirmar “tudo sincronizado”; 4xx descarta fila).

## 3. Estratégia de migração do tema e impacto

### 3.1 Limites dos módulos

- `:core:ui`: tokens imutáveis, `CompositionLocal`, desenho, motion, componentes, previews, testes. Sem Room/Hilt/DataStore/domínio.
- `:feature:*`: associam enums de domínio a receitas visuais, textos traduzidos e callbacks; ViewModels seguem `UiState` imutável + `stateIn(WhileSubscribed(5_000))` + `_retryToken`/`flatMapLatest` + `Channel` para eventos.
- `:app`: resolve `ThemeMode`, shell adaptativo, insets, navegação, workers (reconciliação de lembretes).
- `:core:domain`/`:core:data`: mudanças funcionais das seções 2.1–2.4 com testes e Kover.

### 3.2 Ativação progressiva

1. `BrainOutNeoTheme` aditivo (F1); `BrainOutTheme` continua servindo rotas legadas.
2. O modo resolvido por `MainActivity` (System/Light/Dark persistido) alimenta ambos; nada de `isSystemInDarkTheme()` por rota (perderia a escolha manual).
3. Cada rota migrada envolve conteúdo completo (top bar, diálogos, menus). Shell novo com tema próprio; rotas legadas mantêm tema antigo explicitamente.
4. Lista estática temporária de rotas migradas (não persistida, sem flag de usuário).
5. F8/NB-32: ativação global, remoção de wrappers e valores legados.

### 3.3 Contratos funcionais que devem sobreviver

| Área | Contrato a preservar |
|---|---|
| Projetos | Busca, tags, ordenação, ativos/concluídos, navegação por ID; criação com `TagDraft` transacional |
| Tarefas do projeto | Matriz `TaskStatus`, cascata conclusão/reabertura do projeto, limite de 50 tarefas ativas, prioridade bloqueada em DONE |
| Prazo | Check de feriado/fim de semana, janela inclusiva, feedback de indisponibilidade sem bloquear salvamento |
| Dashboard | Cinco grupos de prioridade (`TaskPriority` LOW…CRITICAL), contagens e denominadores de conclusão |
| Autenticação | Login/cadastro locais, papéis Owner/Member, validações e destino inicial |
| Preferências | System/Light/Dark, perfil local, lembretes independentes da permissão Android |
| Sessão | Logout limpa sessão e pilha |
| Offline | Escrita local + pendência na mesma transação; processamento da fila intacto |

## 4. Sequência, esforço e marcos

| Fase | Entrega | Tarefas | Depende de | Esforço |
|---|---|---|---|---|
| F0 | Baseline e contratos | NB-01 a NB-02 | — | 8–12 h |
| F1 | Fundação visual (tokens, fontes, tema, sombra/motion) | NB-04 a NB-07 | F0 | 18–26 h |
| F2 | Componentes, catálogo e pilotos | NB-08 a NB-11 + NB-03 | F1 | 24–34 h |
| F3 | Domínio: membership, prazos, DONE, tags | NB-12 a NB-16 | F1 (testes), podendo paralelizar com F2 | 26–38 h |
| F4 | Shell de 5 destinos + Projetos | NB-17 a NB-19 | F2 + F3 (NB-12) | 19–27 h |
| F5 | Editor, global acionável, kanban, agenda, criação rápida | NB-20 a NB-24 | F3 + F4 | 33–47 h |
| F6 | Auth, participantes, preferências, arte | NB-25 a NB-28 | F4; NB-26 depende de NB-12 | 22–30 h |
| F7 | Dashboard, motion e acabamento | NB-29 a NB-31 | F5 + F6 | 14–20 h |
| F8 | Ativação global, QA físico e entrega | NB-32 a NB-35 | F7 | 18–26 h |
| **Total** | **35 tarefas** | | | **182–260 h** |
| **Com reserva de 25%** | Ajustes, regressões e spikes | | | **228–325 h** |

Esforço total estimado: **182–260 h** (228–325 h com reserva). Sem prazo contratado (Q3); com ~20 h semanais isso corresponderia a 12–16 semanas, indicativo de planejamento pessoal, não compromisso. Reestimar ao final de F0 e F2.

**Marcos:** M0 baseline (F0); M1 design system utilizável (F2); M2 domínio funcional (F3); M3 fluxo completo de tarefas (F5); M4 todas as rotas (F7); M5 entrega com QA físico (F8).

**Caminho crítico:** F0 → F1 → F2 → F4 → F5 → F7 → F8, com F3 liberando F4/F5 e F6 podendo paralelizar após F4. Fontes/licenças, contraste, semântica, modo de tema e sucesso de criação são critérios de entrada, não polimento posterior.

## 5. Mapa de arquivos

Aliases expandem para caminhos completos relativos à raiz; `$UI/theme/NeoColor.kt` = `core/ui/src/main/kotlin/pucgo/joaopedrogmsilva/brainout/core/ui/theme/NeoColor.kt`. Tarefas distinguem **criar** de **alterar**.

| Alias | Caminho |
|---|---|
| `$UI` | `core/ui/src/main/kotlin/pucgo/joaopedrogmsilva/brainout/core/ui` |
| `$UI_TEST` | `core/ui/src/test/kotlin/pucgo/joaopedrogmsilva/brainout/core/ui` |
| `$APP` | `app/src/main/kotlin/pucgo/joaopedrogmsilva/brainout` |
| `$APP_TEST` | `app/src/test/kotlin/pucgo/joaopedrogmsilva/brainout` |
| `$PROJECTS` | `feature/projects/src/main/kotlin/pucgo/joaopedrogmsilva/brainout/feature/projects` |
| `$PROJECTS_TEST` | `feature/projects/src/test/kotlin/pucgo/joaopedrogmsilva/brainout/feature/projects` |
| `$AUTH` | `feature/auth/src/main/kotlin/pucgo/joaopedrogmsilva/brainout/feature/auth` |
| `$AUTH_TEST` | `feature/auth/src/test/kotlin/pucgo/joaopedrogmsilva/brainout/feature/auth` |
| `$AUTH_ANDROID_TEST` | `feature/auth/src/androidTest/kotlin/pucgo/joaopedrogmsilva/brainout/feature/auth` |
| `$TASKS` | `feature/tasks/src/main/kotlin/pucgo/joaopedrogmsilva/brainout/feature/tasks` |
| `$TASKS_TEST` | `feature/tasks/src/test/kotlin/pucgo/joaopedrogmsilva/brainout/feature/tasks` |
| `$SETTINGS` | `feature/settings/src/main/kotlin/pucgo/joaopedrogmsilva/brainout/feature/settings` |
| `$SETTINGS_TEST` | `feature/settings/src/test/kotlin/pucgo/joaopedrogmsilva/brainout/feature/settings` |

Domínio/dados novos ficam em `core/domain/src/main/kotlin/pucgo/joaopedrogmsilva/brainout/core/domain/` e `core/data/src/main/kotlin/pucgo/joaopedrogmsilva/brainout/core/data/` (e testes irmãos). Recursos compartilhados em `core/ui/src/main/res/`; textos por feature em `values/strings.xml` + `values-en/strings.xml` (sempre os dois). Todo `.kt`/`.kts`/`.xml` com cabeçalho de autoria do projeto; fontes/ícones com licença em `core/ui/licenses/`.

**Novos arquivos centrais (criar):** `ProjectMembershipRepository` + impl, `ProjectMemberEntity`, `MIGRATION_4_5`, `AddProjectMemberUseCase`, `RemoveProjectMemberUseCase`, `TagRepository.create/update/observeTagsWithUsage`, `TaskDao.observeDueBetween`, `DeadlineReminderReconciler`, `LegacyDeadlineNormalization`, `TaskEditorScreen/ViewModel`, `QuickCreateTaskSheet`, `KanbanBoard`, `AgendaScreen/ViewModel`, `TasksFilterBar`, `ParticipantsScreen`, `BrainOutNavigationShell`, `$UI/theme/NeoColor.kt`, `NeoTokens.kt`, `NeoTypography.kt`, `BrainOutNeoTheme.kt`, `$UI/modifier/HardShadow.kt`, `$UI/motion/BrainOutMotionPolicy.kt`, componentes `$UI/component/*`, `$UI/preview/BrainOutComponentGallery.kt` (debug), arte geométrica em `core/ui/src/main/res/raw|drawable/` e `docs/design/verify_roadmap.py`.

## 6. Backlog executável (NB-01 a NB-35)

Cada tarefa: verificar dependências → produzir o artefato → executar a validação indicada → registrar evidência → gate da fase. Lógica: teste de comportamento primeiro (ver falhar pelo motivo esperado → implementar → verde). Visual: inspeção + testes semânticos relevantes; sem testes que só repliquem constantes.

**Fixtures compartilhadas (usadas de F2 em diante):** `Owner` + `Member` (contas locais), 3 projetos (ativo do Owner, compartilhado com Member, concluído), tags válidas/inválidas/duplicadas, tarefas em todos os status/prioridades (LOW…CRITICAL), prazos passado/hoje-dia-inteiro/hoje-com-hora/amanhã/semana/sem prazo, título longo (200), 60 tarefas para volume. Definidas em NB-11 como helpers de teste reutilizáveis (sem duplicação por arquivo).

### F0 — Baseline e contratos

#### NB-01 — Baseline técnico/funcional e roteiro de desempenho

**Depende de:** —. **Refs:** R12, R14, E4.7, E5.3.

**Criar:** `docs/design/BASELINE.md`, `docs/design/PERFORMANCE.md`. **Ler:** `AGENTS.md`, `gradle/libs.versions.toml`, `core/ui/build.gradle.kts`, `$APP/MainActivity.kt`, `$APP/navigation/BrainOutNavHost.kt`.

1. Registrar SHA, JDK, SDK, variante, resolução real de dependências e dispositivos disponíveis.
2. Rodar o gate da seção 9 (incluindo os `testDebugUnitTest` por feature — a CI os lista porque `testDevDebugUnitTest` não os alcança); registrar falhas preexistentes.
3. Registrar o defeito do diálogo de criação (fecha antes do sucesso: `HomeScreen.kt` fecha em `onCreateProject`, no dispatch de `viewModel.createProject`, e não no evento de sucesso) como conhecido; a correção é NB-18, antes da migração visual da Home.
4. Definir o roteiro reproduzível de desempenho (Q27): build de medição = `:app:assembleDevRelease` assinado com keystore local **não versionado** (variáveis `BRAINOUT_KEYSTORE_*`, gerado com `keytool` fora do repo) ou artefato CI; capturas de tela não são evidência de desempenho.
5. Comandos do roteiro: `adb shell am force-stop <pkg>` + `am start -W -S -n <pkg>/.MainActivity` (startup frio ×3), cenário fixo Projetos → detalhe → voltar; `adb shell dumpsys gfxinfo <pkg> reset` → cenário → `dumpsys gfxinfo <pkg> framestats`; métricas: tempo total de startup, proporção de frames lentos, p95 por frame (referência 16,7 ms a 60 Hz / 8,3 ms a 120 Hz). Registrar dispositivo, taxa de atualização, 3 execuções e variação. Mesmo build/dispositivo antes (NB-01) e depois (NB-34).

**Aceite:** baseline reproduzível com números reais; roteiro executável por outra pessoa; nenhuma afirmação de desempenho a partir apenas do código.

#### NB-02 — Matriz de migração e de capacidades

**Depende de:** NB-01. **Refs:** R1, R3, R11, E2.8, E4.4.

**Criar:** `docs/design/MATRIZ_MIGRACAO.md`. **Ler:** rotas/telas dos quatro módulos de feature, `$PROJECTS/ui/home/{HomeBottomBar,HomeTab,HomeScreen}.kt` (a barra inferior vive na feature de projetos; **não existe** `$APP/navigation/BottomBar.kt`), `$APP/navigation/BrainOutNavHost.kt`, seção 2 deste plano.

1. Listar as onze rotas atuais + as novas (agenda, editor de tarefa, participantes, biblioteca de tags, kanban): componentes, callbacks, `TestTags`, insets.
2. Registrar estados: loading, erro de leitura, erro de ação, vazio, filtro sem resultado, conteúdo, permissões; marcar rotas `legada/em migração/migrada/validada`.
3. Tabela de capacidades: o que já existe (CRUD de tarefas, cascata, filas, preferências) × capacidade nova (seções 2.1–2.8) × o que é local-only (2.9).
4. Registrar lacunas conhecidas (**conferidas em 02/10/2026**): `CanPerformActionUseCase` sem uso de produção, Member sem visibilidade (`observeSearch(ownerId = …)` é owner-only), detalhe sem gate de papel, `UpdateTaskUseCase` bloqueia qualquer update de DONE, `selected_tag_id` órfão e — lacuna nova — `ProfileScreen`/`ThemeScreen`/`NotificationsScreen`/`SettingsViewModel` **inexistentes** (Perfil, Tema e Notificações são placeholders vazios no `NavHost`). `ChangeTaskStatusUseCase` **saiu** da lista: já reconcilia lembrete.

**Aceite:** 100% das rotas/estados mapeados; nenhuma capacidade nova descrita como existente.

#### NB-03 — Pilotos de direção executáveis

**Depende de:** NB-02. **Refs:** R11, R14, E4.4, E5.3.

> **Q31 — ordem de execução (02/10/2026):** esta tarefa pertence a **F2**, logo depois de NB-11, e não a F0 como na versão anterior. O piloto usa a galeria criada em NB-11, que depende de F1; manter NB-03 em F0 tornava a fase impossível de fechar. A linha `Depende de:` acima continua apontando para NB-02 porque é a dependência de **conteúdo** (matriz de migração) e porque o verificador do plano exige dependência com ID anterior. Ordem real: F0 (NB-01, NB-02) → F1 → F2 (NB-08…NB-11 → NB-03).

**Criar:** `docs/design/PILOTOS.md`, `docs/design/evidencias/pilotos/`. **Protótipo:** rota interna de galeria (ver NB-11) com fixtures — execução real em emulador, não mockup estático.

1. Montar Projetos, detalhe, login, lista global e agenda (mock) em claro/escuro com dados curtos/longos e as fixtures de NB-11.
2. Simular 320/600/840 dp, fonte 2.0, erro, teclado aberto; checar hierarquia, densidade e impacto visual (critério Q1).
3. Revisar com o usuário decisões de execução (sem mudar identidade/paleta/fontes — Q4) e registrar em `PILOTOS.md`.
4. Reestimar **F3–F8** a partir das dificuldades observadas (F1 e F2 já estarão executadas quando esta tarefa rodar — ver Q31).

**Gate F0 / M0:** baseline conhecido e matriz completa. Direção visual validada em tela só no gate de F2, junto com NB-03.

### F1 — Fundação visual

#### NB-04 — Tokens de cor e geometria

**Depende de:** NB-02. **Refs:** R11, E4.4, E4.5.

**Criar:** `$UI/theme/NeoColor.kt`, `$UI/theme/NeoTokens.kt`, `$UI_TEST/theme/NeoContrastRatioTest.kt`, `$UI_TEST/theme/NeoTokenParityTest.kt`. **Ler:** `$UI/theme/Color.kt`, `$UI_TEST/theme/ContrastRatioTest.kt`, `docs/design/tokens.json`.

1. Papéis light/dark em tipos imutáveis + `CompositionLocal`; espaçamentos 0/4/8/12/16/20/24/32/40/48/64 dp; raios 0/4/8/16; bordas 1/2/3; offsets 2/4/6.
2. Mapear todos os papéis Material (containers, outline, inverse, scrim); `primary` = link legível; botão amarelo usa tokens de ação (nunca `primary` amarelo).
3. **Paridade JSON × Kotlin (contrato):** teste unitário que declara os mesmos pares de papéis/valores do JSON e falha se divergir — comparação por código revisável, sem parser em runtime.
4. Testar pares reais: texto ≥ 4.5:1, essenciais ≥ 3:1, paridade entre temas.

**Validação:** `python3 docs/design/verify_tokens.py`; `./gradlew :core:ui:testDebugUnitTest`. **Aceite:** sem púrpura residual; paletas B/C não habilitadas.

#### NB-05 — Fontes, ícones e arte base

**Depende de:** NB-04. **Refs:** R11, E4.4, E4.6.

**Criar:** `$UI/theme/NeoTypography.kt`, `core/ui/src/main/res/font/`, `core/ui/src/main/res/drawable/`, `core/ui/licenses/`, `docs/design/ASSETS.md`.

1. Fontes estáticas Archivo/Public Sans (SIL OFL 1.1) com pesos usados e licenças; sem fonte variável como único suporte a API 24/25; primeira abertura 100% offline.
2. Quinze estilos tipográficos do contrato; metadata 12 sp, mas erros/ações/prazos ≥ 14 sp; fonte 1.0/1.3/2.0 sem clamp.
3. Vetores Material Symbols Outlined locais (mesma família, só os usados; mirroring onde necessário).
4. Assets de arte geométrica ≤ 1 MB comprimidos (Q28), com poster estático para fallback.

**Validação:** build + primeira abertura offline; API 24/25 quando disponível; pt-BR/EN. **Aceite:** licenças rastreáveis, nenhuma fonte remota.

#### NB-06 — Tema aditivo sem ativação global

**Depende de:** NB-04, NB-05. **Refs:** R11, E4.5.

**Criar:** `$UI/theme/BrainOutNeoTheme.kt`. **Alterar:** `$APP/MainActivity.kt`, `$APP/navigation/BrainOutNavHost.kt`, `$UI_TEST/theme/ThemeSelectionTest.kt`.

1. Wrapper Material com tokens/tipografia/shapes novos; `BrainOutTheme` segue para rotas legadas.
2. Propagar o `ThemeMode` resolvido pelo app aos subtrees novos (sem recalcular por `isSystemInDarkTheme()`).
3. Lista estática temporária de rotas migradas (inicialmente vazia).
4. Testar Light/Dark/System, inclusive sistema escuro com escolha manual clara e o inverso.

**Aceite:** preferência persistida com precedência correta; nenhuma tela redesenhada antecipadamente.

#### NB-07 — Sombra rígida e política de motion

**Depende de:** NB-06. **Refs:** R11, E4.4.

**Criar:** `$UI/modifier/HardShadow.kt`, `$UI/motion/BrainOutMotionPolicy.kt`, `$UI/motion/BrainOutMotionTokens.kt`, `$UI_TEST/motion/BrainOutMotionPolicyTest.kt`. **Alterar:** `gradle/libs.versions.toml`, `core/ui/build.gradle.kts` só se dependência direta for necessária.

1. Sombra deslocada sem blur, desenhada à mão porque `dropShadow` só aparece em Compose mais novo que a BOM atual (`2024.10.01` → UI `1.7.5`). **Auditoria 02/10/2026:** o POM confirma a versão, mas **não** decide a presença da API — conferir a API resolvida ao iniciar NB-07 e só então manter ou trocar a abordagem.
2. Face se move no press (80 ms, +2 dp) e volta (120 ms); hitbox/layout estáveis; cobrir clipping e listas.
3. Durações: estado 160 ms, navegação 220 ms (≤ 16 dp), modal/conclusão 240 ms (≤ 24 dp), ilustração ≤ 600 ms, um ciclo.
4. `BrainOutMotionPolicy` central: duration scale 0 desativa motion customizado e seleciona poster estático (Q28); haptics opcional só em confirmação real.

**Validação:** preview claro/escuro, toque real, teste normal/zero. **Gate F1:** fundação usável por qualquer feature.

### F2 — Componentes e catálogo

#### NB-08 — Ações, controles e indicadores semânticos

**Depende de:** NB-07. **Refs:** R11, E4.4.

**Criar:** `$UI/component/BrainOutButton.kt`, `BrainOutFab.kt`, `BrainOutFilterChip.kt`, `BrainOutBadge.kt`, `BrainOutSelectionControls.kt`, `$UI_TEST/component/BrainOutActionsTest.kt`. **Alterar:** `core/ui/build.gradle.kts` (Compose UI Test JUnit4 + test manifest, aliases do catálogo).

1. Botões primário/secundário/terciário/destrutivo + loading/disabled, sem duplo acionamento.
2. FAB e chips com target ≥ 48×48 dp, botão ≥ 52 dp, FAB 56 dp; segmentado (Lista|Kanban, Mês|Semana|Dia) como controle com `selected`.
3. Badges com texto/ícone redundantes; badge informativo não anuncia botão.
4. Radio/switch com selected/checked, foco 3 dp + offset 2 dp, TalkBack/teclado.

**Validação:** `./gradlew :core:ui:testDebugUnitTest`. **Aceite:** sombra não reduz hitbox; semântica correta.

#### NB-09 — Campos, data/hora, feedback e aviso local

**Depende de:** NB-08. **Refs:** R7, R11, E2.8, E3.5, E4.4.

**Criar:** `$UI/component/BrainOutTextField.kt`, `BrainOutSearchField.kt`, `BrainOutDateField.kt`, `BrainOutTimeField.kt`, `BrainOutFeedback.kt`, `BrainOutLocalOnlyNotice.kt`; `$UI_TEST/component/BrainOutFieldsTest.kt`.

1. Label persistente, supporting text, erro, disabled, senha, limpeza, IME; campo ≥ 56 dp.
2. **Campo de prazo (Q17):** data obrigatória + hora opcional + alternador “dia inteiro”; estilizar DatePicker/TimePicker nativos sem degradar teclado/leitura.
3. Erro inline/snackbar sem “desfazer” fictício; erro preserva valor digitado.
4. `BrainOutLocalOnlyNotice`: faixa contextual discreta “só neste dispositivo” (Q24), com papel informativo (não botão).

**Aceite:** nenhuma mensagem essencial só por placeholder/cor; data e hora navegáveis por teclado.

#### NB-10 — Superfícies, overlays e estados de conteúdo

**Depende de:** NB-09. **Refs:** R5, R10, R11, E2.8, E3.4.

**Criar:** `$UI/component/BrainOutSurface.kt`, `BrainOutAppBar.kt`, `BrainOutDialog.kt`, `BrainOutStatePanel.kt`, `BrainOutSyncBanner.kt`; `$UI_TEST/component/BrainOutStatesTest.kt`.

1. Superfície/card, top bar, menu, diálogo/sheet com scrim escuro opaco; sem transparência no texto.
2. Painéis de loading, vazio e erro com CTA real quando existente; skeleton estático acessível.
3. Erro de leitura substitui conteúdo; erro de ação preserva lista e rascunho.
4. `BrainOutSyncBanner` recebe estado de apresentação (tabela 2.9); sem consultar worker/repositório.

**Aceite:** um anúncio por mensagem; back fecha overlay; mensagens de sync não extrapolam o estado recebido.

#### NB-11 — Catálogo, contrato e fixtures

**Depende de:** NB-08, NB-09, NB-10. **Refs:** R11, R12, E4.4, E4.7.

**Criar:** `$UI/preview/BrainOutComponentGallery.kt` (registrada só em `debug` sourceSet / `BuildConfig.DEBUG`), `docs/design/CATALOGO.md`, helpers de fixture compartilhados (seção 6).

1. Previews/galeria com variantes, temas, fonte 2.0 e textos longos usando fixtures.
2. Documentar API, semântica, sombra, estados e receitas de status/prioridade/tag.
3. Rodar o piloto Compose (NB-03) com estes blocos; corrigir API compartilhada antes de copiar para features.
4. Galeria é ferramenta de desenvolvimento (não entra em build de release; remoção avaliada em NB-32).

**Gate F2 / M1:** componentes utilizáveis, contratos acessíveis verificados, `:core:ui` sem domínio/dados e direção validada em tela por NB-03.

### F3 — Domínio e dados funcionais

#### NB-12 — Membership e permissões de domínio

**Depende de:** NB-02. **Refs:** R2, R4, E1.7, E2.1.

**Criar:** `ProjectMemberEntity`, `ProjectMembershipRepository` + impl, `AddProjectMemberUseCase`, `RemoveProjectMemberUseCase`, `PermissionDeniedException`, `UserNotFoundException`, `AlreadyMemberException`, `MIGRATION_4_5` (+ testes de migração). **Alterar:** `BrainOutDatabase` (v5), `DataModule`, `HomeListingPipeline.kt:146-175`, `TaskDao` (join de projetos acessíveis), use cases de mutação (gate `CanPerformActionUseCase` com usuário ativo), `HomeActions.kt:34-43`, `ProjectDetailViewModel.kt:105-123` (gate de acesso por papel).

1. Teste primeiro: Member sem membership não vê projeto alheio; Owner adiciona Member por email; Member vê e edita tarefas; Member não deleta projeto/tarefa nem gerencia tags/participantes; Owner sim.
2. Implementar tabela/repositório/use cases e aplicar gate em todas as mutações (domínio, não só UI).
3. Atualizar consultas de visibilidade (Home, lista global, agenda).
4. Cobrir erros: email inexistente, membro duplicado, permissão negada em cada mutação.

**Validação:** `./gradlew :core:domain:test :core:data:testDevDebugUnitTest` + Kover. **Aceite:** matriz Q20 aplicada no domínio; migração 4→5 sem perda.

#### NB-13 — Semântica de prazo, lembretes e reclassificação

**Depende de:** NB-02. **Refs:** R7, R8, E3.5, E3.6.

**Criar:** `DeadlineReminderReconciler`, `LegacyDeadlineNormalization`, coluna `deadline_all_day` (em `MIGRATION_4_5` ou migração complementar documentada). **Alterar:** `Task`/`TaskEntity` (`dueDateAllDay`), `CreateTaskUseCase.kt:65-74`, `UpdateTaskUseCase.kt:50-69`, `ChangeTaskStatusUseCase.kt:83-97`, `WorkManagerDeadlineScheduler.kt:41-59`, `DeadlineWorker.kt:54-78`, `InstantConverter`/`DeadlineField.kt:33-48`.

1. Teste primeiro: dia inteiro grava 23:59 local; com hora grava instante exato; lembrete 24 h/1 h antes; recálculo em qualquer edição **inclusive mudança de status** (o `ChangeTaskStatusUseCase` **já** reconcilia hoje — o teste protege o comportamento existente e cobre a semântica nova de dia inteiro).
2. Normalização idempotente de meia-noite local → dia inteiro na primeira carga; testes de fuso (inclui mudança de fuso).
3. `DeadlineWorker` revalida gatilho × prazo atual antes de publicar.
4. UI de prazo (em NB-09/NB-20) consome o novo contrato; semântica de feriado preservada.

**Validação:** `:core:domain:test`, `:core:data:testDevDebugUnitTest`, `:app:testDevDebugUnitTest` (worker). **Aceite:** zero lembrete obsoleto nos cenários testados.

#### NB-14 — Regras de DONE editável

**Depende de:** NB-13. **Refs:** R4, E2.2, E2.4.

**Alterar:** `UpdateTaskUseCase.kt:43-55`, `Task.kt` (guardas de prioridade), testes de domínio.

1. Teste primeiro: DONE aceita título/prazo/responsável; rejeita prioridade e status direto; DONE → DOING continua via `ChangeTaskStatusUseCase`.
2. Implementar exceções de domínio claras (`InvalidTaskStateException` ou equivalente existente).
3. Atualizar `ProjectDetailViewModel`/editor para expor `canEditStatus/canEditPriority/canDelete`.

**Aceite:** Q21 implementado no domínio e exposto na UI; cascata e matriz de status intactas.

#### NB-15 — CRUD persistente de tags e contagem de uso

**Depende de:** NB-12 (papéis). **Refs:** R3, R4, E2.1, E2.6.

**Criar:** `DuplicateTagNameException`, `TagWithUsage`. **Alterar:** `TagRepository.kt` (hoje 22 linhas, só leitura: `observeForOwner`/`observeByProjectIds`; `create`/`update`/`observeTagsWithUsage` ainda não existem), `TagRepositoryImpl.kt`, `TagDao.kt:34-67`, `ListingPreferencesRepositoryImpl.kt` (limpar `selected_tag_id` órfão), `HomeListingPipeline.kt:174-178`.

1. Teste primeiro: criar tag independente; rename/recore persistem localmente; duplicata vira erro de domínio; exclusão limpa seleção órfã; `projectCount` correto.
2. Implementar `create/update/observeTagsWithUsage` com regra de papel (Owner administra — Q20).
3. Associação/desassociação via `replaceProjectTags` existente; **não** enfileirar TAG UPDATE (descartado pelo dispatcher); aviso local na UI (NB-19/NB-26).
4. Preservar unicidade `(owner_id, name)` e validações 1–30 / `#RRGGBB`.

**Validação:** `:core:domain:test`, `:core:data:testDevDebugUnitTest`. **Aceite:** Q18 completo localmente; nenhuma promessa de sync de rename/recore.

#### NB-16 — Consultas de agenda e filtros globais

**Depende de:** NB-12. **Refs:** R4, R9, E2.2, E2.6.

**Alterar:** `TaskDao` (`observeDueBetween`, consulta global incluindo membership), `TaskRepository` (expor consultas), testes de DAO.

1. Teste primeiro: janela [start, end] retorna prazos corretos (dia inteiro 23:59 no dia certo; com hora no instante); projetos de membership inclusos; ordenação estável.
2. Implementar consultas com índices adequados se necessário (análise de `EXPLAIN` simples registrada).
3. Consulta global filtrada/ordenada pode continuar em memória sobre a Flow; documentar limites (volume das fixtures).

**Aceite:** consultas suportam NB-21/NB-23 sem varredura indevida.

### F4 — Shell de cinco destinos e Projetos

#### NB-17 — Shell adaptativo com cinco destinos

**Depende de:** NB-11, NB-12. **Refs:** R1, R11, E4.4.

**Criar:** `$APP/navigation/BrainOutNavigationShell.kt`, `$APP_TEST/navigation/BrainOutNavigationShellTest.kt`. **Alterar:** `$PROJECTS/ui/home/HomeBottomBar.kt` e `HomeTab.kt` (a barra inferior está na feature de projetos; `$APP/navigation/BottomBar.kt:65-115` e `$APP_TEST/navigation/BottomBarTest.kt` **não existem** — o teste é novo), `BrainOutNavHost.kt`, `BrainOutRoutes` (rota `agenda`).

1. Cinco destinos (Projetos, Tarefas, Agenda, Dashboard, Configurações); tema novo no shell.
2. < 600 dp bottom bar; ≥ 600 dp rail; margens 16/24/32; conteúdo ≤ 1200 dp; formulários ≤ 480 dp. **Contrato ≥ 840 dp:** mesmo grid de 2 colunas com margem 32 (painéis lista/detalhe ficam no backlog). Largura útil = janela − rail − margens − offset de sombra.
3. Preservar `saveState/restoreState`, `singleTop`; detalhe e subrotas sem barra indevida; um único proprietário de insets (corrigir duplo padding de Scaffolds).
4. Testes 599/600 e 839/840 dp, rotação, altura < 480 dp, IME, back (incluindo preditivo quando disponível).

**Validação:** `./gradlew :app:testDevDebugUnitTest` + inspeção em emuladores 320/600/840/1024. **Aceite:** cinco abas restauram estado; sem padding duplicado.

#### NB-18 — Correção do ciclo assíncrono de criação de projeto

**Depende de:** NB-11. **Refs:** R3, R5, E2.1, E2.8.

**Alterar:** `$PROJECTS/ui/home/HomeActions.kt`, `HomeViewModel.kt`, `HomeUiModels.kt`, `HomeScreen.kt` (o índice `102-109` aponta para o FAB; o fechamento indevido está no bloco do `CreateProjectDialog`), `HomeCreateProjectDialog.kt`; `$PROJECTS_TEST/ui/home/HomeViewModelTest.kt`. **Criar:** `$PROJECTS_TEST/ui/home/HomeCreateProjectDialogTest.kt`.

1. Teste primeiro (vermelho): hoje `HomeScreen` fecha o diálogo no dispatch, mesmo em falha.
2. Estado explícito de criação + evento de sucesso local (Channel); impedir segunda submissão ativa; back durante submissão não cancela escrita iniciada silenciosamente (confirmar descarte).
3. Falha mantém diálogo + rascunho; separar erro de criação do erro de leitura; ausência de sessão sem loading preso.
4. Preservar transação, cancelamento de coroutine e permissões existentes.

**Aceite:** TDD vermelho→verde documentado; PR funcional separado (rollback fácil).

#### NB-19 — Migrar Projetos e entrada da biblioteca de tags

**Depende de:** NB-17, NB-18. **Refs:** R3, R5, R9, R10, R11, E2.1, E2.6, E3.4.

**Alterar:** `$PROJECTS/ui/home/HomeScreen.kt`, `HomeTopBar.kt`, `HomeSearchBar.kt`, `HomeFilterRow.kt`, `HomeProjectCard.kt`, `HomeProjectsContent.kt:57-160`, `HomeFab.kt`, `HomeStates.kt`, `HomeEmptyStates.kt`, `HomeOfflineBanner.kt`, `HomeCreateProjectDialog.kt`. **Criar:** `$PROJECTS_TEST/ui/home/HomeScreenTest.kt`, `$PROJECTS_TEST/ui/home/HomeEmptyStateTest.kt` (`HomeEmptyStateTest.kt` **não existe** hoje — a suíte de `:feature:projects` tem só três arquivos), `$PROJECTS/ui/tags/TagLibraryScreen.kt` + `TagLibraryViewModel` (entrada na área de Projetos), testes correspondentes.

1. Componentes novos mantendo `TestTags` e callbacks; tags com swatch + label neutra (hex inválido → fallback sem alterar valor persistido).
2. Grid: 1 coluna < 600 dp; 2 colunas ≥ 600 dp quando cada card ≥ 260 dp; keys estáveis; sombra sem corte.
3. **Biblioteca de tags (Q18):** listar com contagem de uso, criar, renomear, recolorir, excluir (confirmação), com `BrainOutLocalOnlyNotice` em rename/recore/desassociação; Member vê somente leitura (Q20).
4. Estados loading/erro/vazio/filtro vazio/sync conforme tabela 2.9; diálogos Owner/Member preservados.

**Gate F4:** Projetos + shell operacionais offline; captura comparável ao baseline com impacto visual visível.

### F5 — Tarefas: editor, global, kanban, agenda e criação rápida

#### NB-20 — Editor compartilhado de tarefa

**Depende de:** NB-13, NB-14, NB-19. **Refs:** R3, R4, R7, E2.2, E2.4, E3.5.

**Criar:** `$PROJECTS/ui/taskeditor/TaskEditorScreen.kt`, `TaskEditorViewModel.kt`, `TaskEditorTestTags.kt` (ou rota equivalente em feature de tarefas — decidir em NB-20 e documentar; proposta: `:feature:tasks`), `$TASKS_TEST/.../TaskEditorViewModelTest.kt`, `$TASKS_TEST/.../TaskEditorScreenTest.kt` (Compose UI Test crítico).

1. Contrato da seção 2.5: `TaskEditorUiState` + eventos `Saved/Deleted`; uma submissão por vez; falha preserva rascunho.
2. Campos: título (1–200), prioridade (desabilitada em DONE), status (somente leitura no editor; mudanças pelos menus/kanban), prazo data+hora+dia inteiro (NB-09), responsável, aviso local (Q24).
3. Validações/feriado existentes preservados; exclusão só Owner com confirmação (Q20).
4. Testes Compose UI: validação, salvamento, regras de DONE (Q21), preservação de rascunho em erro.

**Aceite:** editor único usado por lista global/kanban/agenda/criação rápida.

#### NB-21 — Tarefas globais acionáveis com filtros persistidos

**Depende de:** NB-20, NB-16. **Refs:** R3, R4, R9, E2.2, E2.6.

**Alterar:** `$TASKS/ui/TasksScreen.kt:65-269`, `TasksViewModel.kt` (bloco de `uiState`), `ListingPreferencesRepository` (chaves novas); `$TASKS_TEST/ui/TasksViewModelTest.kt`. **Criar:** `$TASKS/ui/TasksFilterBar.kt`, `$TASKS_TEST/ui/TasksScreenTest.kt`.

1. Linhas acionáveis: abrir projeto, abrir editor, menu de status (transições válidas), excluir (Owner, confirmação); prazo/badges com label redundante.
2. Busca por título + filtros (projeto, status, prioridade, período) + ordenação (prazo, prioridade, criação, título) — contrato 2.6; persistência por usuário preservada no logout.
3. Estados: loading, erro de leitura, vazio real vs. filtro sem resultado (CTAs distintos), erro de ação preserva lista.
4. Testes Turbine de ViewModel + Compose UI smoke da fila de ações; TalkBack: labels de prioridade/status/prazo.

**Aceite:** Q7A/Q25 completos; nenhum affordance falso.

#### NB-22 — Kanban por projeto

**Depende de:** NB-20, NB-14. **Refs:** R4, E2.2, E2.4, E2.5.

**Criar:** `$PROJECTS/ui/projectdetail/KanbanBoard.kt`, `KanbanColumn.kt`, `KanbanCard.kt`, `$PROJECTS_TEST/ui/projectdetail/KanbanBoardTest.kt` (Compose UI Test crítico). **Alterar:** `ProjectDetailScreen.kt`/`Body` (controle segmentado Lista|Kanban), `ListingPreferencesRepository` (modo de visualização).

1. Colunas TODO/DOING/DONE ordenadas por `priorityCode` desc, `createdAt` asc (Q22); keys por `task.id`.
2. DnD próprio (pointerInput) entre colunas → `ChangeTaskStatusUseCase`; transição inválida devolve cartão com mensagem; sem animação de lista inteira.
3. Menu “Mover para …” com transições válidas (comandos acessíveis obrigatórios — Q15); operável por teclado/TalkBack.
4. DONE: sem prioridade editável; reabrir = DONE → DOING; TODO→DONE encadeado existente preservado; cascata do projeto preservada.

**Validação:** testes de transição via comandos e um cenário de DnD em Robolectric/Compose. **Aceite:** matriz `TaskStatus` nunca violada; DnD e comandos produzem o mesmo resultado.

#### NB-23 — Agenda mês/semana/dia

**Depende de:** NB-16, NB-20. **Refs:** R7, R9, E3.5.

**Criar:** `$TASKS/ui/agenda/AgendaScreen.kt`, `AgendaViewModel.kt`, `AgendaMonth.kt`, `AgendaWeek.kt`, `AgendaDay.kt`, `$TASKS_TEST/ui/agenda/AgendaViewModelTest.kt`, `AgendaScreenTest.kt` (Compose UI Test crítico).

1. Visões Mês|Semana|Dia (segmentado); agrupamento por data local (contrato 2.8); “Atrasadas” sempre visíveis; dia atual destacado.
2. Toque no dia → lista do dia; toque na tarefa → editor; “criar tarefa neste dia” → criação rápida com data preenchida; dia vazio explícito.
3. Janelas limitadas via `observeDueBetween`; sem carregar o histórico todo; keys estáveis.
4. Testes: agrupamento dia inteiro vs. com hora, virada de mês/semana, DST/fuso (trocar fuso do dispositivo em teste de VM), navegação para o editor.

**Aceite:** Q7C/Q16 completos; datas corretas no fuso atual.

#### NB-24 — Criação rápida de tarefa

**Depende de:** NB-20. **Refs:** R3, R4, E2.2.

**Criar:** `$TASKS/ui/quickcreate/QuickCreateTaskSheet.kt`, `QuickCreateTaskViewModel.kt`, testes de ViewModel + smoke Compose.

1. Bottom sheet: título, seletor de projeto (obrigatório; pré-preenchido em contexto), prioridade padrão MEDIUM, data+hora opcionais, responsável opcional.
2. Aciona `CreateTaskUseCase` (limite 50 ativas preservado); sucesso → feedback + listas atualizadas; falha preserva rascunho; uma submissão por vez.
3. Entradas: Tarefas (ação global), Agenda (dia), detalhe do projeto (atalho) — todas usam o mesmo sheet.

**Gate F5 / M3:** lista global, kanban, agenda e criação rápida operando sobre o mesmo editor e as mesmas regras de domínio.

### F6 — Auth, participantes, preferências e arte

#### NB-25 — Splash, login e cadastro + arte explorável

**Depende de:** NB-17. **Refs:** R2, R11, E1.6, E1.7, E4.4.

**Alterar:** `$AUTH/ui/splash/SplashScreen.kt`, `ui/login/LoginScreen.kt`, `ui/register/RegisterScreen.kt`; `$AUTH_ANDROID_TEST/ui/login/LoginScreenTest.kt`. **Criar:** `$AUTH_TEST/ui/AuthScreensTest.kt`, composição de arte geométrica explorável (Q10/Q19).

1. Splash de apresentação + botão Começar (não é system splash); arte explorável (blocos arrastáveis/reagentes ao toque) **sem** escrita de dados; fallback estático com motion desativado.
2. Formulários ≤ 480 dp, fontes locais, erro inline, senha, loading; papéis Owner/Member e validações preservados.
3. Teclado aberto, altura curta, fonte 2.0, primeira abertura offline; arte não compete com CTA.

**Validação:** `./gradlew :feature:auth:testDebugUnitTest` + androidTest existente quando houver dispositivo.

#### NB-26 — Tela de participantes e gates por papel

**Depende de:** NB-12, NB-19. **Refs:** R2, R4, E1.7.

**Criar:** `$PROJECTS/ui/projectdetail/ParticipantsScreen.kt` (ou diálogo), `ParticipantsViewModel.kt`, testes.

1. Owner: listar participantes, adicionar por email (conta local — `UserNotFoundException` amigável), remover (confirmação). Member: somente leitura (Q20).
2. `BrainOutLocalOnlyNotice` “só neste dispositivo” (Q24).
3. Gates visuais coerentes com o domínio: sem botões destrutivos para Member no detalhe (edição/exclusão de projeto, exclusão de tarefa, tags) — a UI espelha o que NB-12 aplica no domínio.
4. Testes: adicionar/remover, email inexistente, membro duplicado, Member sem ação.

#### NB-27 — Configurações, perfil, tema e notificações

**Depende de:** NB-25. **Refs:** R2, R8, R11, E4.5, E4.6, E3.6.

**Alterar:** `$SETTINGS/ui/SettingsScreen.kt`, `SettingsStructure.kt`; `$SETTINGS_TEST/ui/SettingsScreenTest.kt`. **Criar:** `$SETTINGS/ui/ProfileScreen.kt`, `ThemeScreen.kt`, `NotificationsScreen.kt`, `$SETTINGS_TEST/ui/SettingsViewModelTest.kt` — **nenhum desses quatro arquivos existe hoje**: o módulo tem só `SettingsScreen`, `SettingsStructure` e `SettingsRoutes`, e o `NavHost` ignora `Profile`/`Notifications`/`Theme`. Tema persistido (System/Light/Dark) e preferência de lembretes também **não existem** e entram aqui, não como ajuste visual.

1. Lista de preferências + perfil local (nome, avatar por iniciais); sem foto/upload/sync sugeridos.
2. System/Light/Dark com radio semantics e confirmação do modo efetivo; persistência após reinício; sem paletas B/C.
3. Lembretes: preferência × permissão Android distintas; releitura em `ON_RESUME`; API < 33 e 33+; negar permissão não bloqueia o app.
4. Logout limpa pilha; pt-BR/EN completos.

#### NB-28 — Arte em estados vazios e conclusão

**Depende de:** NB-25, NB-20. **Refs:** R11, E4.4. **Refs de arte:** Q9/Q10/Q19/Q28.

**Criar:** ilustrações geométricas reativas para vazio (projetos/tarefas/agenda/tags) e conclusão (tarefa concluída / projeto concluído), com fallback estático.

1. Reativas a ações reais (ex.: blocos se encaixam ao concluir), 1–2 elementos interativos por tela; orçamento total ≤ 1 MB comprimidos.
2. Sem mascote; significado acompanhado de texto; decorativas sem anúncio redundante.
3. Motion ≤ 600 ms, um ciclo; poster estático com `BrainOutMotionPolicy` zero (Q28).

**Gate F6:** auth/preferências/participantes/arte passam regressões; nenhuma promessa de UI divergente da implementação.

### F7 — Dashboard, motion e acabamento

#### NB-29 — Dashboard sem trocar sua matemática

**Depende de:** NB-21. **Refs:** R9, R11, E2.7, E2.8.

**Alterar:** `$TASKS/ui/DashboardScreen.kt`, `DashboardProjectStateCard.kt`, `DashboardPriorityChart.kt`, `DashboardCompletionRateCards.kt`, `DashboardLoading.kt`, `DashboardErrorBanner.kt`, `DashboardEmptyState.kt`; `$TASKS_TEST/ui/DashboardScreenTest.kt`. **Ler:** `DashboardViewModel.kt`.

1. Cinco grupos de prioridade (`TaskPriority` LOW…CRITICAL) no Canvas existente; labels/resumo textual equivalente às barras.
2. Denominadores e ausência vs. zero preservados; sem estatística inventada em vazio/erro.
3. Arranjo compacto/largo; números em pt-BR/EN. Vico não é necessário.

#### NB-30 — Motion integrada e reduced motion

**Depende de:** NB-29, NB-07. **Refs:** R11, E4.4.

**Alterar:** `$APP/navigation/BrainOutNavHost.kt`, componentes de ação/overlay e pontos de confirmação de F4–F6. **Criar:** `$APP_TEST/navigation/ReducedMotionTest.kt`.

1. Transições ≤ 16 dp (navegação) / ≤ 24 dp (modais) nas durações da seção 2.3 do DESIGN; press/release do HardShadow.
2. Sem animação de lista inteira, stagger em cascata ou mudança de layout durante press; DnD com feedback visual estável.
3. Duration scale 0 percorrido em cada motion customizado; haptics só em confirmação real.
4. Cancelamento, retorno, recomposição e background/foreground testados.

#### NB-31 — Acessibilidade, idiomas e layout de todas as rotas

**Depende de:** NB-30. **Refs:** R11, E4.4, E4.6.

**Atualizar:** telas migradas, recursos pt/en, `docs/design/MATRIZ_MIGRACAO.md`.

1. Matriz pairwise da seção 10 incluindo kanban/agenda/editor; limites 599/600 e 839/840; paisagem < 480 dp.
2. Fonte 2.0 sem truncamento de informação; targets ≥ 48 dp; foco de teclado/Switch Access.
3. Percurso TalkBack completo (hierarquia, erros, sem anúncios duplicados, kanban com comandos).
4. pt-BR/EN, pluralização, textos longos, insets.

**Gate F7 / M4:** todas as rotas migradas e revisadas; sem falha crítica aberta.

### F8 — Ativação, QA físico e entrega

#### NB-32 — Ativação global e limpeza

**Depende de:** NB-31. **Refs:** R11, R12, E4.5, E4.7.

**Alterar:** `$UI/theme/Theme.kt`, `Color.kt`, `Shape.kt`, `Type.kt`, `$APP/MainActivity.kt`, `BrainOutNavHost.kt`, testes de tema/contraste.

1. Promover a implementação nova à API canônica `BrainOutTheme`; remover lista de rotas e wrappers.
2. Remover valores legados sem uso (busca de referências); manter `PackageMarker` e exclusões de cobertura.
3. Dynamic color permanece desligado por padrão (identidade determinística); documentar a política.
4. Avaliar remoção da galeria de debug ou mantê-la como ferramenta interna documentada.

**Validação:** gate completo da seção 9. **Aceite:** uma única identidade; sem resíduo de roxo Material nem infraestrutura de migração.

#### NB-33 — QA funcional completo e dispositivos físicos

**Depende de:** NB-32. **Refs:** R5, R10, R11, R14, E3.4, E4.4, E4.7, E5.3.

**Criar:** `docs/design/QA_FINAL.md`, `docs/design/evidencias/final/`.

1. Roteiro funcional da seção 10 com backend stub, incluindo fila offline e 4xx/5xx (sem mensagem falsa de sync).
2. **2 dispositivos físicos (Q26)** — preferir um 60 Hz e um 120 Hz — checando press/sombras/IME/fonte 2.0/dark/tablet; registrar modelos, taxa de atualização e limitações.
3. Matriz pairwise executada e registrada (combinações usadas, não produto cartesiano).
4. Sessões de usabilidade **não** bloqueiam (Q26); se não houver usuários, M4 fica documentado como parcial — sem inventar resultados.
5. Corrigir falhas e repetir só o trecho afetado + gates; não aprovar por screenshots isolados.

**Aceite:** zero bloqueadores funcionais/acessíveis; QA físico com evidência real.

#### NB-34 — Desempenho comparado

**Depende de:** NB-33. **Refs:** R12, R14, E4.7.

**Atualizar:** `docs/design/PERFORMANCE.md` com resultados; `docs/design/BASELINE.md` com comparação.

1. Repetir exatamente o roteiro de NB-01 (mesmo build/dispositivo/cenário, 3 execuções) com o app final.
2. Comparar startup, proporção de frames lentos e p95; registrar variação; alvo numérico firmado em NB-01 após o ruído real do baseline.
3. Se houver regressão: analisar e corrigir; Macrobenchmark só nesse caso (Q27).

**Aceite:** sem regressão perceptível sem análise; números publicáveis.

#### NB-35 — Documentação, evidências e entrega

**Depende de:** NB-34. **Refs:** R12, R13, R14, E5.2, E5.3.

**Atualizar:** `DESIGN.md`, `docs/design/ESPECIFICACAO.md`, `docs/design/PESQUISA.md`, `docs/design/MATRIZ_MIGRACAO.md`, este plano.

1. Sincronizar documentação com o entregue; marcar explicitamente o que ficou no backlog (seção 1.3).
2. Changelog, capturas antes/depois, licenças; artefato final conforme seção 11 (mesmo fluxo de assinatura do baseline).
3. Conferir checklist da seção 12 item a item com evidência; registrar pendências não bloqueantes com próximo passo.

**Gate F8 / M5:** entrega final única (Q11) com evidências reproduzíveis.

## 7. Bibliotecas: regras de entrada

| Opção | Quando considerar | Prova exigida | Decisão inicial |
|---|---|---|---|
| Foundation/Animation + Material 3 | Todos os componentes e microinterações | Compilação no grafo resolvido e semântica preservada | Base obrigatória, sem upgrade geral |
| Lottie Compose | Ilustração pontual que o estático não comunica | Spike, licença, poster, lifecycle, scale zero, tamanho/startup/frames | Opcional após NB-11 |
| Rive | Interação com state machine realmente necessária | Custo nativo, ABIs, lifecycle, acessibilidade | Adiado (arte interativa via Compose em NB-25/28) |
| Compose Unstyled | Um wrapper Material impede controle necessário | Comparação local + foco/semântica | Adiado |
| Calendar | Grid de mês do NB-23 ficar inviável em Foundation | Compatibilidade com Compose 1.7.5 e ganho real | Condicional (spike antes de adotar) |
| Vico | Dashboard evolui além das cinco barras | Requisito novo + resumo acessível | Adiado |
| Coil | Imagens remotas entrarem no escopo | Política offline/cache | Fora deste ciclo |
| Compottie / Shimmer | Necessidade futura específica | Justificativa + custo de upgrade | Fora deste ciclo |

Releases da pesquisa são snapshot de 01/10/2026, **reverificados em 02/10/2026** (8/8 tags e datas conferem; nenhuma release nova entre as duas datas — detalhe na **seção 6** da [pesquisa](../design/PESQUISA.md)). Antes de adotar: checar release/POM atual, versão no catálogo, resolver dependências, spike com relatório adotar/rejeitar. Sem Accompanist deprecated nem kits web. Upgrades de BOM/Compose são PRs próprios, anteriores ao uso de APIs novas.

**Drag-and-drop do kanban:** Compose 1.7.5 não oferece DnD de lazy list pronto; a implementação própria de NB-22 é o caminho principal e os comandos acessíveis são obrigatórios (Q15). Se o DnD próprio se mostrar instável nos testes/reais, o fallback é entregar colunas + comandos de movimentação completos e registrar o DnD como melhoria — nunca sacrificar a matriz de transições ou a acessibilidade.

## 8. Critérios transversais

### Acessibilidade e geometria

- Texto ≥ 4.5:1 nos pares usados; controles/contornos essenciais ≥ 3:1; seleção/erro com texto ou ícone.
- Targets ≥ 48×48 dp; campos 56 dp, botão 52 dp, FAB 56 dp, linha de referência 64 dp sem impedir crescimento.
- Foco: contorno 3 dp, offset 2 dp; sombra não é a única indicação.
- Fonte até 2.0 sem clamp; rolagem em altura curta e overlays; kanban/agenda/editor testados nesses extremos.
- Semântica de botão só onde há ação; gráficos com resumo textual; ilustrações decorativas sem anúncio redundante.
- `TestTags` existentes preservadas; novas em snake_case com prefixo de tela.

### Desempenho

- Keys estáveis; desenho/sombra centralizados; assets locais limitados; sem loop decorativo; arte interativa ≤ 1 MB e com poster estático.
- Medir p95 e proporção de frames lentos (referências 16,7 ms / 8,3 ms); comparar builds equivalentes (nunca debug vs. release); 3 execuções e variação.
- Build de medição assinado localmente com keystore **fora do versionamento** (variáveis `BRAINOUT_*`) ou artefato CI; keystore nunca no PR.

## 9. Comandos de verificação

Da raiz; JDK 21 é o padrão de CI. O JBR local (25) não roda detekt 1.23.7 (`--jvm-target` derivado); validar detekt no JDK 21/CI, sem workarounds de build.

### Documentação e contratos

```bash
python3 docs/design/verify_tokens.py
python3 docs/design/verify_roadmap.py
```

Esperado: contraste/paridade dos documentos de design e consistência deste roadmap (links locais, IDs/dependências de tarefas, estimativas).

### Feedback rápido por módulo alterado

```bash
./gradlew :core:domain:test
./gradlew :core:ui:testDebugUnitTest
./gradlew :core:data:testDevDebugUnitTest
./gradlew :feature:projects:testDebugUnitTest
./gradlew :feature:tasks:testDebugUnitTest
./gradlew :feature:auth:testDebugUnitTest
./gradlew :feature:settings:testDebugUnitTest
./gradlew :app:testDevDebugUnitTest
```

Features não têm flavor próprio (`testDebugUnitTest`); `:app`/`:core:data` usam variantes dev/prod. A CI lista os `testDebugUnitTest` por feature explicitamente porque `testDevDebugUnitTest` não os alcança — o gate completo da seção abaixo os inclui.

### Gates antes de publicar código e ao finalizar

```bash
./gradlew ktlintCheck detekt
./gradlew testDevDebugUnitTest :core:domain:koverVerify :core:data:koverVerify
./gradlew :feature:projects:testDebugUnitTest :feature:tasks:testDebugUnitTest \
  :feature:auth:testDebugUnitTest :feature:settings:testDebugUnitTest
./gradlew :app:lintDevDebug
./gradlew :feature:auth:lintDebug :feature:projects:lintDebug :feature:tasks:lintDebug :feature:settings:lintDebug
./gradlew :app:assembleDevDebug
```

Esperado: checks verdes, Kover ≥ 60% em domain/data, traduções completas, sem novos erros de lint. UI/features sem gate Kover novo. `testDevDebugUnitTest` cobre `:app`/`:core:data`; os comandos explícitos por feature cobrem as features (paridade com a CI).

### Dispositivo/emulador e build de medição

```bash
./gradlew connectedDevDebugAndroidTest
./gradlew :app:assembleDevRelease
```

O primeiro exige dispositivo/emulador e backend adequados. O segundo gera a variante de medição; instalar exige assinatura local (seção 8). Não usar `prod` (URL placeholder) para simular integração.

## 10. QA e evidências

### Matriz pairwise (executável)

Cobrir pares de risco; cada combinação abaixo pelo menos uma vez, registrando o que foi usado.

| Dimensão | Cobertura |
|---|---|
| Largura | 320, 360, 412, 599/600, 839/840 e 1024 dp |
| Altura | Retrato comum e paisagem < 480 dp com teclado |
| Fonte | 1.0, 1.3, 2.0 |
| Tema | Light, Dark, System (escolha manual oposta ao sistema + reinício) |
| Idioma | pt-BR e EN |
| Motion | Normal e duration scale zero |
| Entrada | Toque, TalkBack, teclado externo, Switch Access quando disponível |
| API | 24/25 (fontes/layout) e 33+ (permissão) |
| Dados | Vazio, 60 tarefas, textos longos, todas as prioridades, tags inválidas |
| Rede | Offline, reconexão, 4xx, 5xx, erro de leitura |
| Recursos novos | Kanban (DnD + comandos), agenda (mês/semana/dia), editor (dia inteiro/com hora), membership (Owner/Member) |

### Roteiro funcional principal

1. Primeira abertura offline → splash explorável → cadastro/login local; sessão existente após reinício.
2. Owner cria projeto com tag existente e nova; falha conserva rascunho; clique duplo não duplica; Member sem membership não vê nada; após convite vê e edita tarefas, sem ações destrutivas.
3. Buscar/filtrar/ordenar projetos e **tarefas globais** (filtros persistem após reinício/logout); abrir detalhe sem perder estado.
4. Editor compartilhado: criar tarefa com prazo dia inteiro e com hora; validação/feriado; regras de DONE (Q21); exclusão só Owner.
5. Kanban: mover por DnD e por comando; transições inválidas recusadas; ordenação por prioridade/antiguidade; cascata de conclusão/reabertura.
6. Agenda: mês/semana/dia; tarefa atrasada; virada de mês; criar tarefa a partir do dia; fuso do dispositivo.
7. Biblioteca de tags: criar independente, rename/recore com aviso local, contagem de uso, exclusão com confirmação e limpeza de seleção.
8. Criar tarefa rápida a partir de Tarefas/Agenda/detalhe; offline → reconexão → fila; 4xx/5xx sem “tudo sincronizado”.
9. Dashboard com fixture conhecida; tema persistido; lembretes × permissão; perfil; logout.
10. Repetir pontos de risco com fonte 2.0, TalkBack e motion zero; QA nos 2 dispositivos físicos (Q26).

**Registro por execução:** SHA, variante, dispositivo (modelo/Hz), idioma/tema/fonte, cenário, resultado, capturas. Falhas viram tarefas com reprodução e severidade. Não registrar teste previsto como executado.

## 11. PRs, rollback e publicação

### Recortes de entrega (internos; sem builds intermediários ao usuário — Q11)

| PR | Conteúdo | Tipo / refs |
|---|---|---|
| P0 | Documentação (plano + design) — **PR inicial, só docs (Q30)** | `docs(design)`; R11/R13 |
| P1 | Baseline, matriz, piloto e roteiro de desempenho | `docs(design)`; R12/R14, E4.7/E5.3 |
| P2 | Tokens, fontes, tema aditivo | `feat(ui)`; R11, E4.5 |
| P3 | Sombra, motion e controles | `feat(ui)`; R11, E4.4 |
| P4 | Campos, overlays, estados, catálogo | `feat(ui)`; R11, E2.8/E4.4 |
| P5 | Membership + permissões de domínio (NB-12) | `feat(domain)`; R2/R4, E1.7 |
| P6 | Prazos/lembretes + DONE + tags (NB-13/14/15/16) | `feat(domain)`; R4/R7/R8, E2.4/E3.5 |
| P7 | Correção de criação assíncrona | `fix(projects)`; R3/R5, E2.1/E2.8 |
| P8 | Shell adaptativo + Projetos + biblioteca de tags | `feat(projects)`; R3/R11, E2.1/E2.6 |
| P9 | Editor + global + quick create | `feat(tasks)`; R3/R4, E2.2/E2.6 |
| P10 | Kanban | `feat(tasks)`; R4, E2.2/E2.4/E2.5 |
| P11 | Agenda | `feat(tasks)`; R7, E3.5 |
| P12 | Auth + participantes + preferências + arte | `feat(auth)`/`feat(settings)`; R2/R8/R11, E1.6/E3.6 |
| P13 | Dashboard + motion + acabamento | `feat(dashboard)`; R9/R11, E2.7/E4.4 |
| P14 | Ativação global + QA + evidências | `feat(ui)` + docs; R11/R14, E4.7/E5.3 |

Cada PR compila isoladamente; dividir por componente/feature se a revisão ficar grande, preservando dependências. P5–P6 são funcionais e reverteis sem desacordo visual.

**Fluxo do repositório:** branch `feat|fix|docs|chore/` a partir de main → checks pertinentes → `git status --short` → stage de caminhos explícitos → Conventional Commit com `Refs:` → push → PR pelo template → CI verde → `gh pr merge --merge` → remover branch remota. Nunca publicar com checks vermelhos; nunca incluir mudanças alheias ou segredos.

**Rollback:** antes de NB-32, desativar/reverter a rota migrada mantendo dados e preferências. Depois, reverter o PR de ativação. Sem reset destrutivo; `MIGRATION_4_5` não tem migração reversa (dados novos são aditivos; app antigo ignora colunas novas — registrar em NB-12/NB-13).

**Publicação inicial (Q30) — concluída:** PR só de docs (`DESIGN.md`, `docs/design/`, `docs/plans/`) mergeado em 02/10/2026 como PR #80. `main` local e `origin/main` estão idênticos em `5b483ba`, sem commits pendentes de publicação; a ressalva original sobre «4 commits à frente» não se aplica mais. Artefatos não-docs preexistentes ficam fora dos PRs seguintes: hoje só `.rolebox/` está não rastreado (`specs/`, `.opencode/` e `gradle/gradle-daemon-jvm.properties` **não existem** nesta árvore).

## 12. Riscos e respostas

| Risco | Onde tratar | Resposta verificável |
|---|---|---|
| Escopo ampliado (Q7) extrapola o esforço estimado | NB-03/NB-11 reestimam | Reserva de 25%; sem prazo contratado; backlog da seção 1.3 excluído |
| Tema global cedo regressa todas as telas | NB-06/NB-32 | Tema aditivo + ativação final com gate completo |
| DnD do kanban instável em Compose 1.7.5 | NB-22 | Comandos acessíveis obrigatórios; fallback documentado (seção 7) |
| Semântica de prazo quebra dados legados | NB-13 | Migração + normalização idempotente com testes de fuso/meia-noite |
| Lembretes obsoletos persistem | NB-13 | Reconciliação em toda mutação + revalidação no worker, com testes |
| Membership expõe dados por engano | NB-12 | Gates no domínio + testes de permissão por mutação |
| Capacidades locais parecem sincronizadas | NB-19/20/21/26 + 2.9 | `BrainOutLocalOnlyNotice` + tabela de verdade; sem enfileirar ops descartadas |
| Paleta expressiva dificulta leitura | NB-04/NB-31 | Contraste por pares reais + neutralidade 75–85% + labels redundantes |
| Sombras cortadas/hitbox deslocada | NB-07/08/19/22 | Offset reservado, inspeção em listas e toque nas bordas |
| Rails/grid reduzem área útil | NB-17/NB-31 | Breakpoints, card ≥ 260 dp, um dono de insets |
| Migração de banco falha em dispositivos reais | NB-12/NB-13 | Testes de migração Room + QA nos 2 aparelhos |
| Evidência de QA não corresponde ao artefato | NB-33/34/35 | Registrar SHA, variante, assinatura, aparelho, Hz e cenário |
| Publicação mistura commits alheios | NB-P0/Q30 | PR docs a partir de `origin/main`; publicação concluída no PR #80, sem commits pendentes (ver seção 11) |

## 13. Definição de pronto

- [ ] As 35 tarefas têm artefato/evidência e todos os gates das fases foram atendidos.
- [ ] Cinco destinos, kanban, agenda, biblioteca de tags, editor compartilhado e criação rápida funcionais e coerentes com as seções 2.1–2.9.
- [ ] Matriz de papéis (Q20) aplicada no domínio e refletida na UI; membership local operante.
- [ ] Prazos: dia inteiro 23:59 / hora opcional, lembretes recalculados, dados legados reclassificados (Q23).
- [ ] Tarefas globais acionáveis com filtros/ordenação persistidos (Q7A/Q25).
- [ ] Identidade “Bloco de ação” em todas as rotas, claro/escuro, impacto visual confirmado (Q1/Q4).
- [ ] Acessibilidade: fonte 2.0, TalkBack, foco, targets, pt-BR/EN, motion zero (Q26/Q28).
- [ ] Checks obrigatórios (seção 9) e CI verdes; Kover ≥ 60% em domain/data; Compose UI Tests nos fluxos críticos (kanban, editor, agenda).
- [ ] QA funcional completo + **2 dispositivos físicos** com evidência real (Q26); usabilidade não bloqueia (M4 pode ficar parcial).
- [ ] Desempenho comparado ao baseline com o mesmo roteiro/build (Q27) sem regressão não analisada.
- [ ] Avisos “só neste dispositivo” presentes onde dados não viajam (Q24); nenhuma promessa falsa de sync.
- [ ] Infraestrutura temporária e estilo legado removidos; documentação e evidências refletem o entregue.

**Primeiro recorte recomendado:** P0 (documentação, entregue no PR #81) → NB-01 e NB-02 → F1 (NB-04 a NB-07) em PRs aditivos. Não iniciar pela troca global de `Color.kt` nem por instalar bibliotecas. O primeiro incremento funcional completo é F3+F4 (domínio + Projetos com shell de cinco destinos); o primeiro recorte funcional visível ao usuário só na entrega final (Q11).

**Referência técnica consultada:** [Custom design systems — Android Developers](https://developer.android.com/develop/ui/compose/designsystems/custom) — tokens imutáveis/CompositionLocal e wrappers Material; APIs específicas conferidas contra a versão resolvida (Compose 1.7.5 / Material 3 1.3.1).

## 14. Auditoria doc×código — 02/10/2026

Segunda passagem exigida pelo usuário: os documentos desta proposta descreviam estado atual e estado futuro sem distinguir os dois, e a base inspecionada (`4dcfa0a`) já não era a árvore vigente. Esta seção é o registro da varredura. **Nenhum arquivo `.kt` foi alterado** — quando doc e código divergem, o doc descreve o código como ele é hoje; arquivo futuro fica no texto com marcador `a criar (NB-xx)`.

### 14.1 Método e escopo

- **Base:** `5b483ba` (idêntica a `origin/main`), JDK/SDK sem efeito nesta tarefa — só leitura de código, docs e fontes web.
- **Escopo:** `DESIGN.md`, `docs/design/{ESPECIFICACAO,PESQUISA}.md`, `docs/design/tokens.json` + verificadores, este plano, `docs/ACESSIBILIDADE.md`, `docs/ARQUITETURA.md`, `docs/wireframes/README.md`, `AGENTS.md`, `README.md`.
- **Fora do escopo:** `Documentos/` e `docs/ATAS/` (somente leitura), PDFs, e todo código Kotlin.
- **Referências:** 301 citações de `Arquivo.ext[:linha]` extraídas dos documentos em escopo. 128 apontam para arquivos que o próprio plano manda **criar** — não são divergência. As demais foram conferidas uma a uma: existência do arquivo, faixa de linhas dentro do arquivo, conteúdo da linha citada e atribuição correta de `Criar`/`Alterar`/`Ler`/`Atualizar`.
- **Claims externas:** toda a evidência web de `PESQUISA.md` foi reverificada em 02/10/2026 (releases via `gh api`, POM, licenças SPDX, READMEs, CSS upstream, documentação oficial). Resultado na **seção 6** da [pesquisa](../design/PESQUISA.md).
- **Achados que exigem dispositivo:** nenhum foi declarado verificado. Este documento não substitui os gates das seções 9 e 10.

### 14.2 Divergências encontradas e tratadas

| Documento · linha original | Afirmação | Código real em `5b483ba` | Ação tomada | Decisão afetada |
|---|---|---|---|---|
| Plano NB-02 (`Ler`), NB-17 (`Alterar`) | `$APP/navigation/BottomBar.kt:65-115` e `$APP_TEST/navigation/BottomBarTest.kt` existem e serão alterados | Nenhum dos dois existe; a barra vive em `$PROJECTS/ui/home/HomeBottomBar.kt` + `HomeTab.kt`, com estado em `rememberSaveable` na `HomeScreen` | Referências corrigidas; teste passou a **criar** | NB-17 |
| `docs/ACESSIBILIDADE.md` §2.2 | «Bottom bar com 3 destinos» | `HomeTab` tem **4**: `Projects`, `Tasks`, `Dashboard`, `Settings` | Corrigido para 4 com os nomes | — (doc de acessibilidade) |
| `DESIGN.md` §1 (inventário) e §4 (layout) | Shell «com quatro destinos»; barra inferior com quatro | Idiomas corretos para hoje; o alvo deste redesign é **cinco** (Q13) | Texto passou a declarar 4 hoje **e** 5 como alvo | Q13 |
| `DESIGN.md` §1 (inventário) | `feature/settings/.../ui/{Profile,Theme,Notifications}Screen.kt` — perfil, tema e lembretes reais | O módulo tem só `SettingsScreen`, `SettingsStructure`, `SettingsRoutes`; o `NavHost` trata `Profile`/`Notifications`/`Theme` como placeholder vazio | Inventário reescrito: telas **não existem** | NB-27, F6 |
| `DESIGN.md` §1 (texto após o inventário) | «código confirma quatro abas e `SettingsViewModel`» | Quatro abas confere; `SettingsViewModel` **não existe** em módulo algum | Texto corrigido | NB-27 |
| `DESIGN.md` §1 | «Possui … perfil, tema e lembretes» | Perfil, escolha manual de tema e preferência de lembretes não têm tela nem persistência | Frase reescrita como capacidade proposta | NB-06, NB-27 |
| `ESPECIFICACAO.md` §2 (`:feature:settings`) | «Mudança instantânea de preferência já existe»; «o código atual possui orientação textual» (notificações) | `BrainOutTheme` só deriva `darkTheme` de `isSystemInDarkTheme()`; nenhuma preferência é persistida; não há tela de notificações | Bullets de Tema/Perfil/Notificações marcados como **trabalho novo** | NB-06, NB-27 |
| `ESPECIFICACAO.md` §4 | «override de tema já existente» | Existe só o parâmetro `darkTheme`; modo persistido não existe | Reescrito | NB-06 |
| `ESPECIFICACAO.md` §2 (detalhe) | «Edição de tags no projeto **não é suportada** pelo contrato atual» | `ProjectRepository.update(project, tagIds)` já aceita `tagIds`; o que falta é UI de edição e semântica de desassociação no sync | Reescrito: contrato aceita, UI e sync não cobrem | NB-15, NB-19 |
| `DESIGN.md` §1 (linha de `TasksScreen`) | «chips em `Surface`, sem ação fictícia» | `TasksScreen.kt` usa `AssistChip` com `onClick = { /* chip é decorativo */ }` (l. 305 e 337) — mesma dívida que a linha do `TaskRow` descreve | Linha corrigida: a dívida vale para **as duas** telas | NB-21 |
| Plano §2.2, NB-02 §4, NB-13 §1 | «corrige o gap atual de lembrete obsoleto»; lacuna «lembrete obsoleto em `ChangeTaskStatusUseCase`» | `ChangeTaskStatusUseCase` **já** chama `reconcileReminder` (l. 68 e 85); `UpdateTaskUseCase` também | Gap removido da lista; NB-13 passou a proteger o comportamento e cobrir só o que falta (revalidação no worker + dia inteiro) | NB-13 |
| Plano NB-15 | `TagRepository.kt:7-26` | Arquivo tem **22 linhas** | Faixa removida; texto descreve o conteúdo real | NB-15 |
| Plano NB-01, NB-18 | `HomeScreen.kt:102-109` é onde o diálogo fecha antes do sucesso | `102-109` é o bloco do FAB; o fechamento indevido está no `onCreateProject`, no dispatch de `viewModel.createProject` | Índices removidos; comportamento **confirmado** (o defeito existe) | NB-18 |
| Plano NB-19 | `HomeEmptyStateTest.kt` em `Alterar` | Não existe; `:feature:projects/src/test` tem só 3 arquivos | Passou para `Criar` | NB-19 |
| Plano NB-21 | `TasksViewModel.kt:117-188` | Linha 117 é vazia; o bloco citado (`uiState` + `combine`) é vizinho e plausível | Índice trocado por descrição do bloco | NB-21 |
| Plano §11 (Q30) e §12 | «`main` local 4 commits à frente»; artefatos `specs/`, `.opencode/`, `gradle/gradle-daemon-jvm.properties`; PR docs pendente | PR #80 já mergeado; `main` == `origin/main`; nenhum desses três caminhos existe (só `.rolebox/` está não rastreado) | Publicação registrada como concluída; lista de artefatos corrigida | Q30 |
| `DESIGN.md` §4, `PESQUISA.md` R06/R07 | «Preferir arquivos estáticos oficiais dos pesos necessários» | `google/fonts` publica **só** `Archivo[wdth,wght].ttf` e `PublicSans[wght].ttf` — não há estáticos por peso | Texto passou a exigir instanciação local documentada | NB-05 |
| `PESQUISA.md` R02 | Link do Hype4 «título, autor, introdução acessíveis» | URL respondeu 404 na 1ª tentativa e 200 na 2ª, no mesmo dia | Nota de instabilidade adicionada, referência preservada | — |
| `docs/ARQUITETURA.md` §1 (árvore) | `core/ui` = «tema, tokens, componentes»; `settings` = «preferências, perfil, tema»; `projects` = «…, dashboard» | `:core:ui` só tem `theme/`; `:feature:settings` só tem lista com placeholders; o painel está em `:feature:tasks` | Árvore corrigida | — |
| Plano NB-07, NB-22 | `dropShadow` exige upgrade; Compose 1.7.5 não traz DnD de lazy list | O POM confirma UI `1.7.5`, mas **não** decide presença de API | Premissas marcadas como **reabertas**: conferir a API resolvida ao iniciar NB-07 e NB-22; comandos acessíveis seguem obrigatórios | NB-07, NB-22 (Q15) |
| `docs/ACESSIBILIDADE.md` §1 | Referência quantitativa WCAG **2.1** AA | A proposta usa WCAG **2.2** (limiares idênticos para AA) | Registrado como diferença de referência, sem alteração de texto: os 24 testes seguem válidos | — |

### 14.3 O que a auditoria confirmou (e portanto não mudou)

Registrado para que a varredura seja verificável e para que ninguém "corrija" o que está certo:

| Verificação | Resultado |
|---|---|
| Contraste da paleta A (27 pares declarados no DESIGN) | **27/27** batem com implementação independente da fórmula sRGB; maior desvio 0,0034. O par reprovado `#51368F`/`#B5A1F5` = 4,13:1 confere |
| `tokens.json` × documentos | `verify_tokens.py` verde: 37 primitivos, 39 papéis/tema, 15 tipos, 108 pares; menor razão light 5,90:1, dark 7,68:1 |
| Consistência interna do plano | `verify_roadmap.py` verde: 1978 verificações (links, IDs, dependências, estimativas) |
| Evidência web de `PESQUISA.md` | 8/8 releases (tag **e** data), POM (`1.7.5` / M3 `1.3.1` / Adaptive `1.0.0`), 11 licenças, 5 regras de compatibilidade por README, CSS do ekmas (raio 5px, offset 4px, blur/spread 0), crash do Vico no Android 10, Accompanist removido, data do NN/g, 7 páginas HTTP 200, seção «neobrutalist shadows» na doc de sombras |
| Paleta dos wireframes | 33 hex de `docs/wireframes/styles.css` **idênticos** aos 33 de `Color.kt` — a afirmação «identidade M3 atual» está correta; nenhum `<script>` nos HTML |
| Abas e rotas | `HomeTab` com 4 valores; `BrainOutRoutes` com 6 rotas; `TasksRoutes` cobre lista e painel |
| Matriz de status | Matriz do domínio idêntica à descrita na ESPECIFICACAO (`TODO→DOING`, `DOING→TODO/DONE`, `DONE→DOING`, `DONE→TODO` proibido) |
| Lista global informativa | `TasksScreen` só recebe `onRetry`/`onDismissError` — sem abertura/edição/conclusão, como o doc afirma |
| Lacunas várias do NB-02 | `CanPerformActionUseCase` sem uso de produção; `observeSearch(ownerId = …)` owner-only (Member sem visibilidade); detalhe sem gate de papel; `UpdateTaskUseCase` rejeita qualquer update de DONE; `selected_tag_id` órfão — **todas confirmadas** |
| Números citados do app atual | 5 prioridades (`LOW…CRITICAL`), limite de 50 tarefas ativas, `cascadeCompleteTask`/`cascadeReopenTask`, `enqueueInTx`, `SyncWorker` com lote 50 e 4xx descartado / 5xx `Result.retry`, `deadline-<taskId>` com `REPLACE`, Room v4 com `MIGRATION_1_2…3_4`, raios `4/8/12/16/28`, `dynamicColor = false` por padrão, `ContrastRatioTest` com 24 testes |
| Papéis no cadastro | `RegisterScreen` seleciona Owner/Member por `RadioButton` mapeado para `UserRole` |

### 14.4 Limites desta auditoria

- **A tabela 14.2 cita de propósito os caminhos e índices originais** (ex.: `TagRepository.kt:7-26`, `app/navigation/BottomBar.kt`) e alguns textos passaram a negar um caminho existente (ex.: «não existe `app/navigation/BottomBar.kt`»). Por isso, uma varredura automática ingênua continuará acusando esses trechos: são registro histórico, não erro pendente. O único sinal confiável é esta seção, não um `grep`.
- Referências `Atualizar` para `BASELINE.md`, `PERFORMANCE.md` e `MATRIZ_MIGRACAO.md` aparecem antes de o arquivo existir por desenho: são criados em NB-01/NB-02 e atualizados em fases posteriores.
- **Defeito de dependência NB-03 × NB-11 — resolvido em 02/10/2026 (Q31):** NB-03 pertencia a F0 mas usava a galeria de NB-11 (F2), o que tornava F0 impossível de fechar antes de F2. Decidido mover NB-03 para F2, logo após NB-11; F0 passa a ser NB-01 + NB-02, e o gate de F0 deixa de exigir direção validada em tela. **Efeito de segunda ordem tratado junto:** NB-04 dependia de NB-03, o que criaria o ciclo F1 → NB-11 → NB-03 → F1; NB-04 passou a depender de NB-02, e NB-03 deixou de reestimar F1/F2 (já executadas). O verificador do plano não detecta esse tipo de ciclo — ele valida ordem de IDs e dependência com ID anterior, não o grafo completo.

- As linhas conferidas **apodrecem no próximo commit**: isto é um retrato datado de `5b483ba`, não uma garantia futura. Reexecutar a varredura ao fim de cada fase (F2, F4, F6) é barato; confiar nesta tabela por meses não é.
- Claims que dependem de execução (aparência renderizada, TalkBack, IME, sombra cortada, DnD real, desempenho, ABI de runtime) continuam **não verificados** e pertencem aos gates de F0/F8.
- Nenhuma decisão Q1–Q30 foi reescrita. As duas premissas marcadas como reabertas (NB-07, NB-22) serão redecididas **na fase que já é dona do assunto**, como combinado.
