# BrainOut — Matriz de migração e de capacidades (F0 / NB-02)

**Conferido no código em 02/10/2026** sobre `main` = `1074f43`. Plano de referência: [redesign neobrutalista](../plans/2026-10-01-redesign-neobrutalista.md), tarefa NB-02 e seções 1.2, 1.3, 2 e 3.3.

Razão de existir: dizer, rota por rota e capacidade por capacidade, **o que existe hoje** e **o que é proposta** — antes de qualquer tela mudar de aparência. Onde este documento e o código divergirem, o código está certo: reconferir antes de implementar.

## 1. Rotas e telas

### 1.1 Rotas registradas hoje

| Rota real | Declarada em | Composable | Estado por tela |
|---|---|---|---|
| `splash` | `BrainOutRoutes` / `AuthRoutes` | `SplashScreen` (`:feature:auth`) | `AuthUiState` |
| `login` | `BrainOutRoutes` / `AuthRoutes` | `LoginScreen` (`:feature:auth`) | `AuthUiState` |
| `register` | `BrainOutRoutes` / `AuthRoutes` | `RegisterScreen` (`:feature:auth`) | `AuthUiState` |
| `home` | `BrainOutRoutes` / `ProjectsRoutes` | `HomeScreen` (`:feature:projects`) | `HomeUiState` |
| `project/{projectId}` | `BrainOutRoutes.ProjectDetailPattern` / `ProjectsRoutes.projectDetailRoute(id)` | `ProjectDetailScreen` (`:feature:projects`) | `ProjectDetailUiState` |
| `tasks` | `TasksRoutes.TASKS` (`:feature:tasks`) | `TasksScreen` | `TasksUiState` |
| `dashboard` | `TasksRoutes.DASHBOARD` (`:feature:tasks`) | `DashboardScreen` | `DashboardUiState` |
| `settings` | `BrainOutRoutes` / `SettingsRoutes` | `SettingsScreen` (`:feature:settings`) | **sem UiState** (tela stateless, sem ViewModel) |

O `BrainOutNavHost` (`:app`) registra seis composables (`splash`, `login`, `register`, `home`, `project/{projectId}`, `settings`); `tasks` e `dashboard` entram por navegação disparada a partir das abas da `HomeScreen` via `TasksRoutes`.

**A barra inferior não é do `:app`.** Ela vive em `:feature:projects/ui/home/`: `HomeBottomBar.kt` renderiza `HomeTab.values()` e `HomeScreen.kt` guarda a aba atual em `rememberSaveable`. **Não existe** `app/navigation/BottomBar.kt` — a auditoria da seção 14 do plano removeu essa referência.

### 1.2 Callbacks e donos de insets (observado)

| Tela | Callbacks que recebe | Insets |
|---|---|---|
| `LoginScreen` / `RegisterScreen` | ações de auth pelo `AuthViewModel` | `Scaffold` da própria tela |
| `HomeScreen` | `onOpenTasks`, `onOpenDashboard`, `onOpenSettings` (definidos no `NavHost`) | `Scaffold` da própria tela |
| `ProjectDetailScreen` | abrir/voltar, menu de ações | `Scaffold` da própria tela |
| `TasksScreen` | `onRetry`, `onDismissError` — **nada mais** | sem `Scaffold` |
| `DashboardScreen` | dados do próprio VM | sem `Scaffold` |
| `SettingsScreen` | `onOptionClicked(SettingsActionType)` — o `NavHost` decide | `Scaffold` da própria tela |

`enableEdgeToEdge()` é chamado **uma vez**, em `MainActivity`. Não há tratamento explícito de `WindowInsets`/`imePadding` em nenhuma feature: hoje quem absorve insets é o `Scaffold` de cada tela. **Consequência para o redesign:** quando o shell adaptativo (NB-17) passar a envolver as rotas, é preciso eleger **um único dono** de insets, senão o padding é aplicado duas vezes — risco já registrado no plano.

### 1.3 Test tags

Objetos `object XxxTestTags` colocalizados: `LoginTestTags`, `RegisterTestTags`, `HomeTestTags`, `ProjectDetailTestTags`, `DashboardTestTags`, `TasksTestTags`.

Exceções observadas (tags literais, sem objeto): `SettingsScreen` (`settings_option_<tipo>`), `DeadlineField` (`deadline_clear`, `deadline_picker_confirm`) e `NewTaskDialog` (`project_detail_new_task_priority_field`). Não é defeito, mas o redesign deve padronizar ou manter — e **preservar as chaves existentes**, que os testes usam.

### 1.4 Rotas novas previstas (não existem)

| Rota/peça | Onde entra | Tarefa |
|---|---|---|
| `agenda` (destino de topo) | `:app` + `:feature:tasks` | NB-17, NB-23 |
| `task_editor?taskId=&projectId=` | `:feature:tasks` (proposta) | NB-20 |
| biblioteca de tags | dentro da área de Projetos | NB-19 |
| participantes | detalhe do projeto | NB-26 |
| kanban | detalhe do projeto (modo de visualização, não rota) | NB-22 |
| criação rápida (bottom sheet) | compartilhada por Tarefas/Agenda/detalhe | NB-24 |

## 2. Estados por rota

Classificação: **legada** (identidade M3 roxa), **em migração**, **migrada**, **validada** (migrada + matriz de QA). Hoje **todas as rotas estão `legada`**.

| Rota | loading | erro de leitura | erro de ação | vazio | filtro sem resultado | restrição de papel | Situação |
|---|---|---|---|---|---|---|---|
| `splash` | — | — | — | — | — | — | legada |
| `login`/`register` | `isLoading` | — | `errorMessage` + erro por campo | — | — | papel Owner/Member na criação | legada |
| `home` | `isLoading` | `readError` + retry | `actionError` | estado vazio | «nenhum projeto» | membro sem criação | legada |
| `project/{id}` | sim | sim + retry | sim | lista vazia | — | exclusão/gates por papel | legada |
| `tasks` | sim | sim + retry | via `onDismissError` | vazio orientando a criar projeto | — | — | legada |
| `dashboard` | sim | sim | — | zeros válidos | — | — | legada |
| `settings` | — | — | — | — | — | `SignOut` é a única ação real | legada |

Contrato de apresentação a preservar (ESPECIFICACAO §2): erro de leitura **substitui** o conteúdo; erro de ação **preserva** lista e rascunho; estado vazio legítimo é diferente de falha e de filtro sem resultado; offline + fila vazia não é erro de banco.

## 3. Capacidades

### 3.1 Existe hoje (confirmado no código)

| Capacidade | Onde |
|---|---|
| CRUD de projeto e de tarefa, com transação Room + fila offline | `:core:data` (`enqueueInTx`) |
| Matriz de status `TODO→DOING`, `DOING→TODO/DONE`, `DONE→DOING` (`DONE→TODO` proibido) | `core.domain.model.TaskStatus` |
| Cascata conclusão/reabertura do projeto | `ProjectDao.cascadeCompleteTask/cascadeReopenTask` |
| Limite de 50 tarefas ativas por projeto | `UseCaseConstants.MAX_ACTIVE_TASKS_PER_PROJECT` |
| Prioridade bloqueada em tarefa DONE | `UpdateTaskUseCase` (`TaskPriorityChangeForbiddenException`) |
| Reconciliação de lembrete em toda mutação (create/update/status) | `CreateTaskUseCase`, `UpdateTaskUseCase`, `ChangeTaskStatusUseCase` |
| Drenagem de fila em lote de 50; 4xx descarta, 5xx/IO faz retry | `SyncWorker` |
| Preferências de listagem por usuário (busca, tag, ordenação) | `ListingPreferencesRepository` (DataStore) |
| Papéis Owner/Member + `CanPerformActionUseCase` | `:core:domain` (**sem uso de produção** — ver §4) |

### 3.2 Capacidade nova (proposta)

| Capacidade | Onde entra | Contrato |
|---|---|---|
| Membership local (`project_members`, `MIGRATION_4_5`) | `:core:data` | plano §2.1 |
| Semântica de prazo: dia inteiro 23:59 local × hora exata | `:core:domain` + `:core:data` | plano §2.2 |
| DONE editável (título/prazo/responsável; sem status/prioridade) | `:core:domain` | plano §2.3 |
| Biblioteca de tags (criar/renomear/recolorir/excluir + contagem de uso) | `:core:data` + UI | plano §2.4 |
| Editor compartilhado de tarefa | `:feature:tasks` | plano §2.5 |
| Tarefas globais acionáveis (busca, filtros, ordenação persistidos) | `:feature:tasks` | plano §2.6 |
| Kanban por projeto (DnD + comandos acessíveis) | `:feature:projects` | plano §2.7 |
| Agenda mês/semana/dia | `:feature:tasks` | plano §2.8 |
| Shell adaptativo de 5 destinos (4 hoje) | `:app` | plano §3.2 |
| Perfil, tema Sistema/Claro/Escuro persistido, preferência de lembretes | `:feature:settings` | plano NB-27 |

### 3.3 Capacidade que **não existe** (para não ser desenhada como pronta)

Perfil, escolha manual de tema (System/Light/Dark), preferência de lembretes, telas `ProfileScreen`/`ThemeScreen`/`NotificationsScreen` e `SettingsViewModel`. O módulo `:feature:settings` tem **apenas** `SettingsScreen` (lista neutra), `SettingsStructure` (dados das linhas) e `SettingsRoutes`; o `NavHost` trata `Profile`, `Notifications` e `Theme` como placeholder vazio. `Theme.kt` aplica `isSystemInDarkTheme()` e `dynamicColor = false`, sem persistência de escolha.

### 3.4 Local × sync (verdade dos dados)

| Dado | Local | Viaja no sync | Aviso na UI (Q24) |
|---|---|---|---|
| Projeto/tarefa criar, editar, excluir, mudar status | sim | sim | — |
| Tag criar/excluir | sim | sim | — |
| Prazo (data/hora) | sim | **não** | «só neste dispositivo» |
| Distinção TODO/DOING | sim | **não** (contrato usa booleano `done`) | «só neste dispositivo» |
| Responsável (`assigneeId`) | sim | **não** | «só neste dispositivo» |
| Tag renomear/recolorir | sim | **não** (UPDATE é descartado pelo dispatcher) | «só neste dispositivo» |
| Desassociar tag ↔ projeto | sim | **não** (só associações são enfileiradas) | «só neste dispositivo» |
| Participantes (membership) | sim | **não** (sem endpoint) | «só neste dispositivo» |

## 4. Lacunas conhecidas (conferidas uma a uma)

| Lacuna | Resultado | Evidência |
|---|---|---|
| `CanPerformActionUseCase` sem uso de produção | **confirmada** | só definição e teste (`core/domain/.../CanPerformActionUseCase.kt`) |
| Member sem visibilidade | **confirmada** | `HomeListingPipeline` chama `projectRepository.observeSearch(ownerId = capturedOwnerId, …)`; sem join em membership |
| Detalhe de projeto sem gate de papel | **confirmada** | nenhuma ocorrência de `UserRole`/`isOwner` em `ProjectDetailViewModel.kt` |
| `UpdateTaskUseCase` rejeita qualquer update de DONE | **confirmada** | rejeita quando o registro vigente está DONE, com `TaskPriorityChangeForbiddenException` |
| `selected_tag_id` órfão ao excluir tag | **confirmada** | `ListingPreferencesRepositoryImpl` não limpa a seleção; o filtro continua sendo aplicado na consulta |
| «Lembrete obsoleto em `ChangeTaskStatusUseCase`» | **refutada** | o use case já chama `reconcileReminder`; a auditoria de 02/10/2026 removeu a lacuna do plano |

Lacunas novas encontradas durante o baseline (fora do escopo do redesign, registradas no plano §1.3): 4 testes de `HomeViewModelTest` falhando em `:feature:projects`; nenhum workflow da CI executa teste por feature.

## 5. Ordem de migração e rollback

A ordem do plano vale: F1 fundação visual (aditiva, sem ativar tema global) → F2 componentes + pilotos → F4 shell e Projetos → F5 tarefas (editor, global, kanban, agenda, criação rápida) → F6 auth/settings/arte → F7 dashboard/acabamento → F8 ativação global. Enquanto NB-32 não roda, cada rota migrada mantém o tema novo apenas no próprio subtree, e reverter é desfazer o wrapper daquela rota — sem migração de dados envolvida.
