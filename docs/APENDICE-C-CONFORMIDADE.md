# BrainOut — Apêndice C: Lista de verificação de conformidade técnica

> Instrumento de autoavaliação do documento norteador (Apêndice C),
> de preenchimento obrigatório e anexado à entrega da N2. Para cada
> requisito, indica-se o atendimento e a referência de localização na
> aplicação ou no repositório.
>
> Marco do roadmap: **E5.4** (`docs/ROADMAP.md`).
>
> **Autor:** João Pedro G M Silva — PUC Goiás ADS — matrícula 20251012000740.

| Nº | Requisito | Atendido | Onde é verificável | Marco(s) do roadmap |
|----|-----------|----------|--------------------|---------------------|
| R1 | Mínimo de 6 telas funcionais com navegação estruturada | **Atendido** | `app/src/main/kotlin/pucgo/joaopedrogmsilva/brainout/navigation/BrainOutRoutes.kt` e `BrainOutNavHost.kt` (Splash, Login, Register, Home, Detalhe do projeto, Configurações); telas em `feature/auth/src/main/kotlin/.../ui/{splash,login,register}/`, `feature/projects/.../ui/{home,projectdetail}/`, `feature/settings/.../ui/SettingsScreen.kt` | E1.3, E1.6, E2.1, E2.2 |
| R2 | Autenticação com 2 perfis de permissão distintos | **Atendido** | `core/domain/src/main/kotlin/.../core/domain/model/UserRole.kt` (enum `Owner`/`Member` com `Set<Permission>`), `Permission.kt`, `core/domain/.../usecase/CanPerformActionUseCase.kt`; matriz coberta por `core/domain/src/test/.../PermissionMatrixTest.kt` e `CanPerformActionUseCaseTest.kt`; fluxo em `feature/auth/.../ui/login/LoginScreen.kt` e `ui/register/RegisterScreen.kt` | E1.6, E1.7, E1.8 |
| R3 | Manutenção completa de dados sobre 2 entidades | **Atendido** | `Project` e `Task`: use cases `CreateProjectUseCase`, `UpdateProjectUseCase`, `DeleteProjectUseCase`, `CreateTaskUseCase`, `UpdateTaskUseCase`, `DeleteTaskUseCase`, `ChangeTaskStatusUseCase` em `core/domain/.../usecase/`; DAOs em `core/data/.../local/dao/ProjectDao.kt` e `TaskDao.kt`; UI em `feature/projects/.../ui/` e `feature/tasks/.../ui/TasksScreen.kt` | E2.1, E2.2 |
| R4 | Mínimo de 3 regras de negócio não triviais | **Atendido** | RN01: `core/domain/.../usecase/CreateTaskUseCase.kt` (`MAX_ACTIVE_TASKS_PER_PROJECT`); RN02: `core/domain/.../model/Task.kt` (`changePriority` lança `TaskPriorityChangeForbiddenException`, definida em `core/domain/.../error/BusinessRuleException.kt`); RN03: `core/data/.../repository/TaskRepositoryImpl.kt` (`completeAndCascade`/`reopenAndCascade` em transação `@Transaction`). Testes: `TaskPriorityRulesTest`, `CreateTaskUseCaseTest`, `ProjectCompletionTest` | E2.3, E2.4, E2.5 |
| R5 | Persistência local com tratamento de ausência de conectividade | **Atendido** | Persistência local (E1.5/E1.8) e tratamento de conectividade (E3.3/E3.4) ambos atendidos. Persistência: `core/data/.../local/BrainOutDatabase.kt` (Room, `version = 4`, `exportSchema = true`, cadeia `MIGRATION_1_2`..`MIGRATION_3_4` em `Migrations.kt`), DAOs em `core/data/.../local/dao/`. Tratamento de ausência de conectividade (E3.3/E3.4): fila `pending_ops` (Room, `PendingOpEntity` em `core/data/.../local/entity/`, `PendingOpDao` em `core/data/.../local/dao/`, tabela criada pela migração `MIGRATION_3_4` documentada em `Migrations.kt`); `SyncWorker` (`app/src/main/kotlin/.../sync/SyncWorker.kt`) drena a fila em lotes com política `SyncOutcome` (2xx remove, IOException/5xx `retry()`, 4xx descarta com log); `SyncConnectivityWatcher` (`app/src/main/kotlin/.../sync/SyncConnectivityWatcher.kt`) registra `NetworkCallback` e agenda reconciliação imediata em `onAvailable`; `PendingSyncMonitor` (`core/data/.../sync/PendingSyncMonitor.kt`) e `ConnectivityObserver`/`AndroidConnectivityObserver` (`core/data/.../sync/ConnectivityObserver.kt`) expõem o estado para a UI; banner persistente "Sem conexão" + "X alterações aguardando sincronização" em `feature/projects/.../ui/common/OfflineBanner.kt`. Permissões `INTERNET` e `ACCESS_NETWORK_STATE` adicionadas ao `app/src/main/AndroidManifest.xml` (E3.4) — sem elas a fila `pending_ops` nunca drenaria no dispositivo. Testes: `UserDaoTest`, `ProjectTaskCascadeTest`, `PendingOpDaoTest` (10 casos), `ConnectivityObserverTest` (Robolectric shadow), novos casos E3.4 em `HomeViewModelTest` (17 casos) e `ProjectDetailViewModelTest` (14 casos). | E1.5, E1.8, E3.3, E3.4 |
| R6 | Persistência remota com sincronização de dados | **Atendido** | Cadeia completa de persistência remota + sincronização (E3.1/E3.2/E3.3/E3.4). Cliente HTTP: `BrainOutApi` (`core/data/.../remote/BrainOutApi.kt`, contrato `/v1/ping`, `/v1/projects`, `/v1/tasks`, `/v1/tags` com GET/POST/PUT/DELETE) instanciado por `RemoteDataSource` (`core/data/.../remote/RemoteDataSource.kt`) com `BuildConfig.BASE_URL` injetada por flavor `dev` (`http://10.0.2.2:8000/`) / `prod` (`https://TBD/`) em `core/data/build.gradle.kts` (override opcional via `local.properties` chave `brainout.baseUrl.dev`); backend FastAPI stub em `backend-stub/server.py` (CRUD idempotente via PUT upsert com `id` cliente-supplied, 4xx/5xx mapeados em `SyncOutcome`). Sincronização bidirecional Room ↔ backend: `BrainOutSyncDispatcher` (`core/data/.../sync/BrainOutSyncDispatcher.kt`) roteia cada `PendingOpEntity` para o endpoint correto (CREATE/UPDATE → PUT idempotente com UUID do cliente; DELETE → DELETE 204 mesmo ausente; TAG CREATE → POST com id do servidor); `SyncWorker` (`app/src/main/kotlin/.../sync/SyncWorker.kt`) drena a fila em ordem, preservando ordem global, com backoff exponencial via `WorkManager`; banner `OfflineBanner` em `feature/projects/.../ui/common/OfflineBanner.kt` reflete a contagem de `pending_ops` durante a drenagem. Testes: `BrainOutApiTest` (MockWebServer — lista vazia, lista com 1 projeto, snake_case, POST 201, 404), `BrainOutSyncDispatcherTest` (9 casos, MockWebServer), `SyncWorkerTest` (8 casos, Robolectric + fila real) cobrindo online/offline/4xx/5xx. | E3.1, E3.2, E3.3, E3.4 |
| R7 | Consumo de serviço ou interface de programação externa | **Atendido** | BrasilAPI (feriados nacionais): `core/data/.../remote/HolidayApi.kt` e `HolidayRemoteDataSource.kt` (`BuildConfig.HOLIDAYS_BASE_URL`), repositório `core/data/.../repository/HolidayRepositoryImpl.kt`, modelo `core/domain/.../model/Holiday.kt` e `CheckDeadlineUseCase`; teste com MockWebServer em `core/data/src/test/.../remote/HolidayRepositoryTest.kt` | E3.5 |
| R8 | Uso de recurso nativo do dispositivo | **Atendido** | Notificações locais de prazo: `app/src/main/kotlin/.../notifications/DeadlineWorker.kt`, `DeadlineReceiver.kt`, `CompleteTaskWorker.kt`, `WorkManagerDeadlineScheduler.kt`, `NotificationPermissionStore.kt`; permissão runtime `POST_NOTIFICATIONS` e canal em `app/src/main/AndroidManifest.xml` | E3.6 |
| R9 | Listagens com filtro ou busca e visão consolidada de dados | **Atendido** | Listagens + busca/filtro/ordenação (E2.6) e visão consolidada no Dashboard (E2.7) ambos atendidos. E2.6: `ProjectDao.searchProjects(ownerId, query, tagId, sort)` em `core/data/.../local/dao/ProjectDao.kt` (JOIN `projects` ↔ `project_tags` ↔ `tags` com `WHERE` parametrizado e `ORDER BY` por `SortOrder`); `ListingPreferencesRepository` (interface em `:core:domain`) + `ListingPreferencesRepositoryImpl` em `core/data/.../preferences/` (DataStore Preferences, namespace por `userId`); `HomeViewModel` em `feature/projects/.../ui/home/HomeViewModel.kt` orquestra `combine(listingPrefs, debouncedSearchInput, tags, projectFilter)` com debounce de 300 ms e persiste em `ListingPreferencesRepository`; `HomeScreen` (`feature/projects/.../ui/home/HomeScreen.kt`) com campo de busca (ícone limpar), `LazyRow` de chips de tag (incluindo "Todas"), `DropdownMenu` de ordenação e empty state `HomeNoMatchesState`. E2.7: `DashboardViewModel` (`feature/tasks/.../ui/DashboardViewModel.kt`) combina `ProjectRepository.observeAllForOwner`, `TaskRepository.observeCountByPriority` (novo `GROUP BY priority_code` no `TaskDao`, normalizado para 5 níveis) e `TaskRepository.observeCompletionStats` (janela semanal segunda-feira 00:00 UTC); `DashboardScreen` (`feature/tasks/.../ui/DashboardScreen.kt`) renderiza cartões de estado e gráfico de barras desenhado em Compose Canvas puro (`DashboardPriorityChart.kt`). Testes: `ListingPreferencesRepositoryTest` (10 casos Robolectric — default, persistência, namespacing por user, clear, valor inválido de sort), `HomeViewModelTest` (17 casos — busca + filtro + sort + debounce + persistência entre instâncias), `DashboardViewModelTest` (10 casos — contagens, normalização do histograma, divisão por zero, reatividade, erro/retry/clearError), `DashboardScreenTest` (3 casos Robolectric — valores iniciais, empty state, banner de erro). | E2.6, E2.7 |
| R10 | Tratamento de erros e indicação de estados de interface | **Atendido** | `feature/projects/.../ui/home/HomeViewModel.kt` (`errorMessage`, `retry`), `ProjectDetailViewModel` e `feature/tasks/.../ui/TasksViewModel.kt` — `.catch` nos Flows do Room, empty states e loaders nas screens; validação inline; mensagens localizadas em `app/src/main/res/values[-en]/strings.xml` | E2.8, E1.6 |
| R11 | Usabilidade e acessibilidade conforme diretrizes da plataforma | **Atendido** | Tokens de tema em `core/ui/.../theme/` (`Color.kt`, `Theme.kt`); contraste WCAG AA coberto por `core/ui/src/test/.../ContrastRatioTest.kt` (24 testes); áreas de toque ≥ 48dp em `AssistChip`/`FilterChip` de `:feature:projects` e `:feature:tasks`; `contentDescription` e TalkBack conforme `docs/ACESSIBILIDADE.md` | E4.4, E4.5 |
| R12 | Organização do código em camadas, sem credenciais versionadas | **Atendido** | Camadas: `:core:domain` (modelos + use cases puros), `:core:data` (Room/Retrofit/DataStore + DI em `DataModule.kt`), `:core:ui`, `:feature:*` — raiz `pucgo.joaopedrogmsilva.brainout` (E1.1/E1.2). Credenciais: `local.properties` fora do versionamento (`.gitignore`), keystore via secrets (`docs/CI-CD.md`), cobertura ≥ 60% por Kover em `:core:domain`/`:core:data` (E4.7) | E1.1, E3.2, E4.7 |
| R13 | Repositório Git com histórico distribuído e README | **Atendido** | `README.md` (instruções de build/execução), `docs/CONTRIBUTING.md`, histórico Git contínuo (61+ commits, PRs #53–#62); pipeline com ktlint, detekt e testes em `.github/workflows/ci.yml` | E1.9, E1.10, E5.2 (parcial — README a completar em E5.2) |
| R14 | Pacote instalável gerado e testado em dispositivo físico | **Parcial** | Workflow de release assinado implementado: `.github/workflows/release-apk.yml` (`.aab` via `bundleRelease` com keystore em secrets, disparado por tags `v*`; executado com sucesso no run 35599507321 da tag `v0.3.1-ciclo3`), procedimento em `docs/CI-CD.md` e `docs/DISPOSITIVOS.md`. Teste em ≥ 2 dispositivos físicos pendente do marco E5.3 | E3.7 (atendido); E5.3 (pendente) |

## Notas de verificação

- Cada caminho foi conferido diretamente no código da branch
  `fix/revisao-tecnica-2026-09-24` em **25/09/2026**
  (grep/leitura dos arquivos citados) — nenhuma referência foi
  copiada do roadmap sem checagem. Esta revisão reflete a
  conclusão pós-merge dos marcos **E3.3** (sincronização
  bidirecional Room ↔ backend), **E3.4** (banner offline +
  reconciliação automática em `onAvailable`) e **E2.7** (visão
  consolidada no Dashboard com Canvas bar chart) — todos já em
  `main` via PR #66 (`feat(sync): E3.3 sincronização offline-first
  + E3.4 banner offline e reconciliação`, commit `9dd8696`) e PR
  `feat/dashboard-e27`.
- **R5/R6 (Atendido):** E3.3 e E3.4 entregues — fila `pending_ops`
  (Room v3→v4, migração não destrutiva), `SyncWorker` com política
  `SyncOutcome`, `BrainOutSyncDispatcher` roteando CREATE/UPDATE →
  PUT upsert idempotente com `id` cliente-supplied, banner
  persistente "Sem conexão" + "X alterações aguardando
  sincronização", reconciliação automática via
  `SyncConnectivityWatcher` no callback `onAvailable`. Permissões
  `INTERNET` + `ACCESS_NETWORK_STATE` adicionadas ao manifest em
  E3.4.
- **R9 (Atendido):** E2.7 entregue — `DashboardViewModel` combinando
  três Flows reativos do Room e `DashboardScreen` com gráfico de
  barras desenhado em Compose Canvas puro (`DashboardPriorityChart.kt`),
  acessível pela aba "Painel" da bottom bar da Home.
- **R14 (Parcial):** geração do pacote assinado já funcional
  (`v0.3.1-ciclo3`, run `35599507321` do workflow
  `.github/workflows/release-apk.yml`); a validação em ≥ 2
  dispositivos físicos (E5.3) permanece pendente e está
  mapeada/checklist em `docs/DISPOSITIVOS.md` antes da entrega N2.
- **R13:** `README.md` já atende E1.10; a versão completa de E5.2
  (troubleshooting e links para o relatório técnico) é pré-requisito
  para a entrega N2 e não bloqueia a conformidade.
- Casos de teste funcionais relacionados: `docs/ROTEIRO-TESTES.md`,
  seção 3 (cobertura por requisito); smoke E3.3/E3.4 em modo avião
  documentado em `docs/SMOKE-TEST-CRUD.md` §11.

---

**João Pedro G M Silva - PUC Goiás ADS - 20251012000740**