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
| R5 | Persistência local com tratamento de ausência de conectividade | **Atendido** | Persistência local (E1.5/E1.8) e tratamento de conectividade (E3.3/E3.4) ambos atendidos. Persistência: `core/data/.../local/BrainOutDatabase.kt` (Room, `version = 4`, `exportSchema = true`, cadeia `MIGRATION_1_2`..`MIGRATION_3_4` em `Migrations.kt`), DAOs em `core/data/.../local/dao/`. Tratamento de ausência de conectividade (E3.3/E3.4): fila `pending_ops` (Room, `PendingOpEntity` em `core/data/.../local/entity/`, `PendingOpDao` em `core/data/.../local/dao/`, tabela criada pela migração `MIGRATION_3_4` documentada em `Migrations.kt`); `SyncWorker` (`app/src/main/kotlin/.../sync/SyncWorker.kt`) drena a fila em lotes com política `SyncOutcome` (2xx remove, IOException/5xx `retry()`, 4xx descarta com log); `SyncConnectivityWatcher` (`app/src/main/kotlin/.../sync/SyncConnectivityWatcher.kt`) registra `NetworkCallback` e agenda reconciliação imediata em `onAvailable`; `PendingSyncMonitor` (`core/data/.../sync/PendingSyncMonitor.kt`) e `ConnectivityObserver`/`AndroidConnectivityObserver` (`core/data/.../sync/ConnectivityObserver.kt`) expõem o estado para a UI; banner persistente "Sem conexão" + "X alterações aguardando sincronização" em `feature/projects/.../ui/common/OfflineBanner.kt`. Permissões `INTERNET` e `ACCESS_NETWORK_STATE` adicionadas ao `app/src/main/AndroidManifest.xml` (E3.4) — sem elas a fila `pending_ops` nunca drenaria no dispositivo. Testes: `UserDaoTest`, `ProjectTaskCascadeTest`, `PendingOpDaoTest` (10 casos), `ConnectivityObserverTest` (Robolectric shadow), novos casos E3.4 em `HomeViewModelTest` (18 casos) e `ProjectDetailViewModelTest` (14 casos). **Ressalva de honestidade:** o tratamento de ausência de conectividade é **de fato unidirecional no offline** — toda escrita local é enfileirada e drenada quando a rede volta, mas não há caminho de *pull*: nenhuma tela, repositório ou worker chama `listProjects()`, `listTasks()` ou `listTags()` em código de produção (o `grep` encontra esses métodos só na própria interface `BrainOutApi` e em `RemoteDataSource`). O app não recebe alterações feitas em outro dispositivo. | E1.5, E1.8, E3.3, E3.4 |
| R6 | Persistência remota com sincronização de dados | **Atendido** | Cadeia completa de persistência remota + sincronização (E3.1/E3.2/E3.3/E3.4). Cliente HTTP: `BrainOutApi` (`core/data/.../remote/BrainOutApi.kt`, contrato `/v1/ping`, `/v1/projects`, `/v1/tasks`, `/v1/tags` com GET/POST/PUT/DELETE) instanciado por `RemoteDataSource` (`core/data/.../remote/RemoteDataSource.kt`) com `BuildConfig.BASE_URL` injetada por flavor `dev` (`http://10.0.2.2:8000/`) / `prod` (`https://TBD/`) em `core/data/build.gradle.kts` (override opcional via `local.properties` chave `brainout.baseUrl.dev`); backend FastAPI stub em `backend-stub/server.py` (CRUD idempotente via PUT upsert com `id` cliente-supplied, 4xx/5xx mapeados em `SyncOutcome`). Sincronização Room → backend (push; ver ressalva): `BrainOutSyncDispatcher` (`core/data/.../sync/BrainOutSyncDispatcher.kt`) roteia cada `PendingOpEntity` para o endpoint correto (CREATE/UPDATE → PUT idempotente com UUID do cliente; DELETE → DELETE 204 mesmo ausente; TAG CREATE → POST com id do cliente); `SyncWorker` (`app/src/main/kotlin/.../sync/SyncWorker.kt`) drena a fila em ordem, preservando ordem global, com backoff exponencial via `WorkManager`; banner `OfflineBanner` em `feature/projects/.../ui/common/OfflineBanner.kt` reflete a contagem de `pending_ops` durante a drenagem. Testes: `BrainOutApiTest` (MockWebServer — lista vazia, lista com 1 projeto, snake_case, POST 201, 404), `BrainOutSyncDispatcherTest` (9 casos, MockWebServer), `SyncWorkerTest` (8 casos, Robolectric + fila real) cobrindo online/offline/4xx/5xx. **Ressalva de honestidade — «bidirecional» é exagero:** a sincronização implementada é **Room → backend (push)**. Os endpoints de leitura existem (`GET /v1/projects`, `/v1/tasks`, `/v1/tags`, `/v1/projects/{id}`, `/v1/tasks/{id}` em `BrainOutApi`), mas **nenhum código de produção os consome** — nenhum repositório, worker ou ViewModel materializa dados remotos no Room. Também não há `updated_at`/`updatedAt` em lugar nenhum: `PendingOpEntity` guarda `id`, `entity_type`, `entity_id`, `op_type`, `payload`, `created_at` (instante de enfileiramento) e `attempts`, e CREATE/UPDATE vão no mesmo PUT-upsert cego, sem carimbo de versão para detectar conflito. O markdown deste apêndice usava «sincronização bidirecional Room ↔ backend»; o código faz só a metade que vai do dispositivo para o servidor. | E3.1, E3.2, E3.3, E3.4 |
| R7 | Consumo de serviço ou interface de programação externa | **Atendido** | BrasilAPI (feriados nacionais): `core/data/.../remote/HolidayApi.kt` e `HolidayRemoteDataSource.kt` (`BuildConfig.HOLIDAYS_BASE_URL`), repositório `core/data/.../repository/HolidayRepositoryImpl.kt`, modelo `core/domain/.../model/Holiday.kt` e `CheckDeadlineUseCase`; teste com MockWebServer em `core/data/src/test/.../remote/HolidayRepositoryTest.kt` | E3.5 |
| R8 | Uso de recurso nativo do dispositivo | **Atendido** | Notificações locais de prazo: `app/src/main/kotlin/.../notifications/DeadlineWorker.kt`, `DeadlineReceiver.kt`, `CompleteTaskWorker.kt`, `WorkManagerDeadlineScheduler.kt`, `NotificationPermissionStore.kt`; permissão runtime `POST_NOTIFICATIONS` e canal em `app/src/main/AndroidManifest.xml` | E3.6 |
| R9 | Listagens com filtro ou busca e visão consolidada de dados | **Atendido** | Listagens + busca/filtro/ordenação (E2.6) e visão consolidada no Dashboard (E2.7) ambos atendidos. E2.6: `ProjectDao.searchProjects(ownerId, query, tagId, sort)` em `core/data/.../local/dao/ProjectDao.kt` (JOIN `projects` ↔ `project_tags` ↔ `tags` com `WHERE` parametrizado e `ORDER BY` por `SortOrder`); `ListingPreferencesRepository` (interface em `:core:domain`) + `ListingPreferencesRepositoryImpl` em `core/data/.../preferences/` (DataStore Preferences, namespace por `userId`); `HomeViewModel` em `feature/projects/.../ui/home/HomeViewModel.kt` orquestra `combine(listingPrefs, debouncedSearchInput, tags, projectFilter)` com debounce de 300 ms e persiste em `ListingPreferencesRepository`; `HomeScreen` (`feature/projects/.../ui/home/HomeScreen.kt`) com campo de busca (ícone limpar), `LazyRow` de chips de tag (incluindo "Todas"), `DropdownMenu` de ordenação e empty state `HomeNoMatchesState`. E2.7: `DashboardViewModel` (`feature/tasks/.../ui/DashboardViewModel.kt`) combina `ProjectRepository.observeAllForOwner`, `TaskRepository.observeCountByPriority` (novo `GROUP BY priority_code` no `TaskDao`, normalizado para 5 níveis) e `TaskRepository.observeCompletionStats` (janela semanal segunda-feira 00:00 UTC); `DashboardScreen` (`feature/tasks/.../ui/DashboardScreen.kt`) renderiza cartões de estado e gráfico de barras desenhado em Compose Canvas puro (`DashboardPriorityChart.kt`). Testes: `ListingPreferencesRepositoryTest` (11 casos Robolectric — default, persistência, namespacing por user, clear, valor inválido de sort), `HomeViewModelTest` (18 casos — busca + filtro + sort + debounce + persistência entre instâncias), `DashboardViewModelTest` (9 casos — contagens, normalização do histograma, divisão por zero, reatividade, erro/retry/clearError), `DashboardScreenTest` (3 casos Robolectric — valores iniciais, empty state, banner de erro). | E2.6, E2.7 |
| R10 | Tratamento de erros e indicação de estados de interface | **Atendido** | `feature/projects/.../ui/home/HomeViewModel.kt` (`errorMessage`, `retry`), `ProjectDetailViewModel` e `feature/tasks/.../ui/TasksViewModel.kt` — `.catch` nos Flows do Room, empty states e loaders nas screens; validação inline; mensagens localizadas em `app/src/main/res/values[-en]/strings.xml` | E2.8, E1.6 |
| R11 | Usabilidade e acessibilidade conforme diretrizes da plataforma | **Atendido** | Tokens de tema em `core/ui/.../theme/` — identidade Neo ativa (`NeoColor.kt`, `NeoTokens.kt`, `NeoTypography.kt`, `NeoShapes`) entregue por `BrainOutTheme`, com contraste WCAG AA coberto por teste automatizado: `NeoContrastRatioTest` (5 casos, paleta Neo ativa) e `ContrastRatioTest` (24 casos, paleta legada ainda assinada) em `core/ui/src/test/.../theme/`, ambos com o cálculo WCAG 2.2 de `DESIGN.md` §6. Áreas de toque ≥ 48dp via `Modifier.heightIn(min = 48.dp)` nos chips interativos de `:feature:projects` e `:feature:tasks`; `contentDescription` nos elementos acionáveis e `null` explícito nos decorativos; ordem de leitura e announced roles em `docs/ACESSIBILIDADE.md`. **Ressalva de honestidade:** a varredura com Accessibility Scanner é procedimento manual descrito em `docs/ACESSIBILIDADE.md` §6 e **não** foi executada nesta revisão — a evidência de R11 é automatizada. | E4.4, E4.5 |
| R12 | Organização do código em camadas, sem credenciais versionadas | **Atendido** | Camadas: `:core:domain` (modelos + use cases puros), `:core:data` (Room/Retrofit/DataStore + DI em `DataModule.kt`), `:core:ui`, `:feature:*` — raiz `pucgo.joaopedrogmsilva.brainout` (E1.1/E1.2). Credenciais: `local.properties` fora do versionamento (`.gitignore`), keystore via secrets (`docs/CI-CD.md`), cobertura ≥ 60% por Kover em `:core:domain`/`:core:data` (E4.7) | E1.1, E3.2, E4.7 |
| R13 | Repositório Git com histórico distribuído e README | **Atendido** | `README.md` (instruções de build/execução, troubleshooting e links para o documento norteador e para o relatório técnico), `docs/CONTRIBUTING.md`; histórico Git contínuo (129 commits, de 07/09/2026 a 05/10/2026, PRs #12 a #139); pipeline com ktlint, detekt, testes unitários e Kover em `.github/workflows/ci.yml`. O marco E5.2 consta como entregue no `docs/ROADMAP.md`. | E1.9, E1.10, E5.2 |
| R14 | Pacote instalável gerado e testado em dispositivo físico | **Parcial** | **Geração do pacote: entregue (E3.7).** Workflow `.github/workflows/release-apk.yml` gera `.aab` assinado por `:app:bundleRelease`, valida os 4 secrets (`BRAINOUT_KEYSTORE_BASE64/PASSWORD/KEY_ALIAS/KEY_PASSWORD`), decodifica o keystore em `$RUNNER_TEMP` e roda `jarsigner -verify` no artefato antes de publicar (`brainout-release-aab-<tag>`, retenção 30 dias). Execução bem-sucedida registrada em `docs/qa/run-log-t_333ab600.md`: tag `v0.3.1-ciclo3`, run `35599507321`, artefato assinado de 4,2 MB (duas execuções anteriores falharam e foram corrigidas — o run log é a evidência do caminho feliz). Procedimento em `docs/CI-CD.md`. **Teste em ≥ 2 dispositivos físicos: não entregue (E5.3).** `docs/DISPOSITIVOS.md` ainda tem campos `⏳`: o primário (Samsung Galaxy S20 FE 5G / API 33) está mapeado, mas o secundário emprestado não tem modelo definido e nenhum teste de instalação foi executado/registrado. Sem essa evidência, o requisito não pode ser marcado como atendido. | E3.7 (atendido); E5.3 (pendente) |

## Notas de verificação

- **Revisão de 08/10/2026** (`316ed41`): caminhos, versões, contagens
  de teste e o estado de R1–R14 foram reconferidos contra o código
  depois da ativação do tema Neo. Estado medido: **618 testes
  unitários, 0 falhas** (`:core:domain` 133, `:core:data` 302,
  `:core:ui` 88, `:app` 24, features 71); Kover com 84,9 % de linhas em
  `:core:domain` e 84,1 % em `:core:data` (piso de 60 %); Android Lint
  sem erros (109 avisos, majoritariamente `UnusedResources`);
  `:app:assembleProdRelease` com R8/minify **BUILD SUCCESSFUL**;
  convenção de cabeçalho de autoria
  `// João Pedro G M Silva - PUC Goiás ADS - 20251012000740` na linha 1
  de todo `.kt` sem nenhuma violação. Correções aplicadas nesta
  revisão: R11 (tema Neo ativo + `NeoContrastRatioTest`), R13 (E5.2
  entregue; histórico real de 129 commits, PRs #12–#139) e R14
  (veredito único Parcial, com a divergência contra
  `RELATORIO-TECNICO.md:159` registrada). **R1–R10 e R12 seguem
  Atendidos**, com os caminhos citados reconferidos.
- Cada caminho foi conferido diretamente no código da branch
  `fix/revisao-tecnica-2026-09-24` em **25/09/2026**
  (grep/leitura dos arquivos citados) — nenhuma referência foi
  copiada do roadmap sem checagem. Essa revisão reflete a
  conclusão pós-merge dos marcos **E3.3** (sincronização
  bidirecional Room ↔ backend), **E3.4** (banner offline +
  reconciliação automática em `onAvailable`) e **E2.7** (visão
  consolidada no Dashboard com Canvas bar chart) — todos já em
  `main` via PR #66 (`feat(sync): E3.3 sincronização offline-first
  + E3.4 banner offline e reconciliação`, commit `9dd8696`) e PR
  `feat/dashboard-e27`.
- **R5/R6 (Atendido, com ressalva registrada na tabela):** E3.3 e
  E3.4 entregues — fila `pending_ops` (Room v3→v4, migração não
  destrutiva), `SyncWorker` com política `SyncOutcome`,
  `BrainOutSyncDispatcher` roteando CREATE/UPDATE → PUT upsert
  idempotente com `id` cliente-supplied, banner persistente "Sem
  conexão" + "X alterações aguardando sincronização", reconciliação
  automática via `SyncConnectivityWatcher` no callback `onAvailable`.
  Permissões `INTERNET` + `ACCESS_NETWORK_STATE` adicionadas ao
  manifest em E3.4. **Ressalva:** o que existe é sincronização
  **Room → backend**; não há caminho de *pull* (nenhum código de
  produção consome os endpoints GET) nem resolução de conflito por
  versão (sem `updated_at`). A expressão «bidirecional» que consta em
  `docs/RELATORIO-TECNICO.md` e na nota E3.3 do ROADMAP descreve
  intenção, não comportamento — convém ajustar lá também.
- **R9 (Atendido):** E2.7 entregue — `DashboardViewModel` combinando
  três Flows reativos do Room e `DashboardScreen` com gráfico de
  barras desenhado em Compose Canvas puro (`DashboardPriorityChart.kt`),
  acessível pela aba "Painel" da bottom bar da Home.
- **R14 (Parcial):** geração do pacote assinado já funcional
  (`v0.3.1-ciclo3`, run `35599507321` do workflow
  `.github/workflows/release-apk.yml`, com `jarsigner -verify`
  aprovado e artefato de 4,2 MB, registrado em
  `docs/qa/run-log-t_333ab600.md`); a validação em ≥ 2
  dispositivos físicos (E5.3) permanece pendente —
  `docs/DISPOSITIVOS.md` ainda tem campos `⏳` no primário e o
  secundário não tem modelo definido. **Veredito único: Parcial.**
  Registrando a divergência entre documentos, sem editar os demais:
  `docs/RELATORIO-TECNICO.md:159` afirma **Atendido** e cita o teste
  em dois dispositivos como registrado, mas não existe registro de
  execução — nem em `docs/DISPOSITIVOS.md`, nem em run log, nem em
  roteiro; e `docs/ROADMAP.md:688` mantém **E5.3 como `[ ]`
  (não entregue)**, que é o estado real. Este apêndice adota o veredito
  **Parcial**, coerente com o ROADMAP. **Pêndice:** alinhar
  `RELATORIO-TECNICO.md:159` ao mesmo veredito antes da entrega N2 —
  os dois documentos não podem afirmar coisas diferentes sobre o mesmo
  requisito.
- **R13:** `README.md` atende E1.10 e o marco E5.2 consta como
  entregue no ROADMAP, com troubleshooting e links para o relatório
  técnico e para o documento norteador.
- Casos de teste funcionais relacionados: `docs/ROTEIRO-TESTES.md`,
  seção 3 (cobertura por requisito); smoke E3.3/E3.4 em modo avião
  documentado em `docs/SMOKE-TEST-CRUD.md` §11.

---

**João Pedro G M Silva - PUC Goiás ADS - 20251012000740**