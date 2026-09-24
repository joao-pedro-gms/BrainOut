# BrainOut — Revisão técnica e plano de melhorias

**Data:** 2026-09-24T13:49Z · **Commit:** 5ce89d9 · **Branch:** main
**Escopo:** 185 arquivos `.kt/.py/.yml`, 9 módulos Gradle + backend-stub + CI.
Esta página é a síntese priorizada em P0/P1/P2 das não-conformidades e melhorias.

---

## P0 — bugs críticos, corrigir imediatamente

| # | Local | Problema | Fix |
|---|---|---|---|
| **P0-1** | `app/.../navigation/BrainOutNavHost.kt:163-177` | `signOut()` em `scope = rememberCoroutineScope()` cancela quando Settings deixa composição → DataStore write interrompido → sessão persiste → próximo cold-start ainda loga. | Mover para `viewModelScope`/lifecycleScope e `await` antes do `navigate(Login) { popUpTo(0) }`. |
| **P0-2** | `app/.../notifications/CompleteTaskWorker.kt:35-48` | `catch (Exception) → Result.retry()` quebra idempotência em task deletada: `IllegalArgumentException` re-retraya para sempre. Contradiz KDoc e nome do teste. | Distinguir `IllegalArgumentException`/`NoSuchElementException` (idempotente → `Result.success`) de `IOException`/`SQLiteException` (transient → `retry`). |
| **P0-3** | `core/data/.../security/PepperProvider.kt:48-50` | Pepper derivado de `masterKey.toString()` ("MasterKey{keyAlias=…, isKeyStoreBacked=…}"). Como o alias default é constante pública `_androidx_security_master_key_`, pepper é **SHA-256 de string pública idêntica em todo dispositivo**. KDoc declara "pepper nunca deixa o hardware" — falso; material do Keystore **nunca** é lido. | Gerar salt aleatório uma vez; persistir via `EncryptedSharedPreferences` ou KeyStore-protegido. |
| **P0-4** | `core/data/.../sync/BrainOutSyncDispatcher.kt:101-107` + `TagSyncPayload` + `TagCreateDto` | DELETE de tag nunca chega ao servidor: servidor gera id próprio no POST, cliente envia só `{name, color}` sem id, dispatcher descarta resposta. `DELETE /v1/tags/{localUUID}` → 404 → tag orfã no servidor. | Adicionar `id` client-supplied em `TagSyncPayload`/`TagCreateDto` (paridade com projetos/tasks via R6) **ou** capturar id do servidor. |
| **P0-5** | `ci.yml:107,122,217` (`testDebugUnitTest`, `lintDebug`, `connectedDebugAndroidTest`) | Tarefas não-flavor não resolvem em `:app`/`:core:data` (variants são `testDevDebugUnitTest`, `lintDevDebug`, `connectedDevDebugAndroidTest`). CI roda pra outros módulos mas **silenciosamente pula** `:app` unit tests (`SyncWorkerTest`, `CompleteTaskWorkerTest`, `WorkManagerDeadlineSchedulerTest`) + **gate MissingTranslation de `:app` não dispara** + `:app` lint invisível. | Trocar para tasks flavor-qualified ou aggregate `check`. Validar com `./gradlew tasks --all`. |
| **P0-6** | `release-apk.yml:52-56` + `app/build.gradle.kts:24-27,106-110` | Só `BRAINOUT_KEYSTORE_BASE64` é validado. Se os 3 secrets de senha faltarem, `hasReleaseSigning=false` → `bundleRelease` roda **sem assinatura** e sobe o `.aab` não-assinado como release verde. | Validar 4 secrets em `release-apk.yml` (`[ -z "...$VAR" ]`) + `jarsigner -verify` antes do upload. |
| **P0-7** | `core/domain/.../error/TagNotFoundException.kt:4` | Estende `RuntimeException`, não `DomainException`. Quebra capturas agregadas de domínio → app não captura o erro estruturado. | `class TagNotFoundException(tagId: String) : DomainException(...)` + ajustar teste. |
| **P0-8** | `core/domain/.../usecase/ChangeTaskStatusUseCase.kt:59` | `kotlin.error("Tarefa não encontrada: $taskId")` lança `IllegalStateException` cru. Exceção de fronteira não-mapeada (sem teste). | Criar `TaskNotFoundException(taskId)` em `error/` + teste. |

---

## P1 — correções adjacentes / qualidade alta

### App module (`brainOut :app`)

- `MainActivity.kt:87-89` + AGENTS: permission request prompt roda em **toda composição**, não só na Home como documentado.
- `MainActivity.kt:152-161` `withContext(Dispatchers.IO) { currentActiveUser }` sem `runCatching` → spinner eterno em DataStore/Room falho no cold-start.
- `BrainOutAppSmokeTest.kt:32-52` — `actual == expected` tautologia (sets idênticos).
- `DeadlineWorker.kt:104` + `NotificationDismissal.kt:22` — `task.id.hashCode()` como notification ID: colide, pode ser negativo; clamp em `Int.MAX_VALUE`/`0x7FFF_FFFF` ou map estável.
- `SyncConnectivityWatcher.kt:55-60` — `registerNetworkCallback` dispara `onAvailable` na primeira rede disponível; sync duplica com o `ensurePeriodicSync` 15-min. Pular o primeiro.
- `DeadlineReceiver.kt:27-30` + `DeadlineWorker.kt:33-35` — KDoc + manifest mencionam ação "Dispensar" que não é adicionada ao `addAction`. Implementar ou remover das referências.
- `notifications/NotificationPermissionStore.kt` + `DeadlineReceiver` carecem de teste Robolectric (goAsync + cancel + dismiss).

### core/domain

- `CanPerformActionUseCase.kt:19-27` — sem `@Inject constructor` e `invoke` não é `suspend`. Quebra template AGENTS duas vezes.
- `Task.transitionTo` + `CreateTaskUseCase.scheduleReminder` + `ChangeTaskStatusUseCase.reconcileReminder` + `UpdateTaskUseCase.reconcileReminder` usam `Instant.now()` direto (não injetável → RN03 não-testável; `TaskTest` **não afirma** populating/clearing em transições).
- `TaskPriority.fromCodeOrThrow` é alias morto de `fromCode`. Remover ou documentar e estender.
- `TaskPriority.VALID_CODES` + `LOW_CODE..CRITICAL_CODE` são dead constants.
- `SortOrder.fromStorageKey` tem dois branches com o mesmo fallback (`CreatedDesc`).
- `TaskRepository.kt` KDoc vaza "Room" / "invalidação do Room" / "@Transaction" para o domínio.
- `UpdateTaskUseCase.kt:50-53` prossegue silenciosamente quando `findById` retorna null → RN02 não roda.
- 4 use cases **sem teste** (gap conhecido, AGENTS): `CreateTagUseCase`, `UpdateProjectUseCase`, `DeleteProjectUseCase`, `DeleteTagUseCase`. Cada um é 1-line delegate mas zerar cobertura derruba o piso Kover.
- `TaskPriorityRulesTest.update integral` duplica cenário `change priority from DONE is blocked` e nem exercita `copy`.

### core/data

- Redundância `DataModule.@Provides` para classes já `@Inject @Singleton`: `UserRepositoryImpl`, `ProjectRepositoryImpl`, `TaskRepositoryImpl`, `TagRepositoryImpl`, `HolidayRepositoryImpl`, `PasswordHasherImpl`, `AndroidConnectivityObserver`. Cortar.
- `BrainOutDatabase.kt:70` `const val NAME = DATABASE_NAME` é alias morto.
- Kover exclusion `*.remote.*` em `build.gradle.kts:87` mascara `RemoteDataSource` (error prop) e `HolidayRemoteDataSource` (cache+mutex) — não conta pra 60%.
- `RemoteDataSource.kt:48-53 vs :56-175` — `ping()` engole Exception; outros relançam. Inconsistente.
- `RemoteDataSource.kt:31-33` — `HttpLoggingInterceptor.Level.BASIC` sempre on, inclusive release. Loga URL em prod.
- `RemoteDataSource.kt:50-60` — `catch (Exception)` engole `CancellationException`. BrainOutSyncDispatcher já corrigiu; alinhar.
- `ProjectRepositoryImpl.create/update` + `TaskRepositoryImpl.delete` — `@Transaction` aninhado (`enqueueInTx`→`replaceProjectTags`/`cascade*`) depende de SQLite savepoints. Sem teste de roll-back.
- `ProjectRepositoryImpl.kt:136-138` — `require(tag.ownerId == ownerId)` lança `IllegalArgumentException` cru; criar `TagOwnershipException`.
- `UserDao.deleteAll()` + `count()` mortos em prod (`@VisibleForTesting` ou remover).
- `TaskRepositoryImpl.delete` enfileira DELETE op mesmo quando task já deletada por cascade.
- `RemoteDtos.kt:101-109` + `RemoteDataSource.kt:80-88` — `ProjectCreateDto.id: String? = null`; kotlinx-serialization omite null do PUT body → stub rejeita com 400 (path/body mismatch). Id obrigatório.
- `Documented updated_at / last-writer-wins` ausente do data layer e stub. Zero colunas `updated_at` em entidades/payloads/stub. PUT é blind upsert. Resolver antes de multi-device.
- `ConnectivityObserver.kt:73,83` — `connectivityManager.activeNetwork` deprecated API 29+.
- `PasswordHasherImpl.verify` ignora `parsed.iterations` (campo morto). Latente.
- `HttpLoggingInterceptor` em release (mesmo finding acima).

### core/ui

- `ThemeSelectionTest.kt:86-120` é tautológico (não invoca `BrainOutTheme` nem `isSystemInDarkTheme()`).
- `ContrastRatioTest` cobre subset apenas: faltam `outline`/`surfaceVariant`, `onSurface`/`surfaceVariant`, error/background, inverseSurface.
- Dynamic color path (`Theme.kt:114-116`) sem teste e sem garantia WCAG AA.
- `Shape.kt` contrato violado por 23 hardcoded `RoundedCornerShape(...)` em `feature/*` — sem enforcement.

### feature/auth

- `AuthViewModel.kt:100-108, 188-194` — `emailError`/`passwordError` **e** `errorMessage` setados com a mesma string → banner + `supportingText` mostram a mesma mensagem em duplicidade.
- `AuthViewModel.kt:51` `MutableSharedFlow()` default (replay=0, buffer=0, SUSPEND) → `NavigateHome` emitido durante rotation pode perder-se. Trocar para `Channel(BUFFERED)` ou `MutableSharedFlow(extraBufferCapacity=1, onBufferOverflow=DROP_OLDEST)`.
- `AuthViewModel.kt:109, 128, 170, 196` emite `AuthEvent.FocusField` + `AuthField` enum — dead code, não consumido; UI usa `LaunchedEffect(uiState.*Error)`.
- `AuthUiState.MIN_PASSWORD_LENGTH = 8` enforçado só no VM, não em `CreateUserUseCase`/`AuthenticateUserUseCase` (apenas `isNotBlank()`). Parity gap.
- `LoginScreen.kt:246-270` `resolveAuthMessage` `else -> message` vaza domain PT raw em locale EN (`InvalidModelException` no catch `DomainException`).
- `LoginScreen.kt:3-4` comentário referencia `SessionViewModel` em `:feature:auth` que **não existe**.
- `AuthViewModel.reset()` morto (nunca invocado).
- `LoginScreen.kt:78` `onLoginSubmit` (mesmo em RegisterScreen) deveria ser `onLoginSuccess` — nome mente.
- `feature/auth/res` contém 4 strings `home_*` **duplicadas** em `feature/projects/res` — drift risk → eventual `Duplicate resources`.
- Sem `RegisterScreenTest` + `SplashScreenTest`.

### feature/projects

- `HomeViewModel.buildHomeUiState` linhas 380-385 — `tags = inputs.tagChips` (todas tags do owner) atribuído a **cada** ProjectCard → cards mostram tags erradas. DAO tem `observeTagsFor`; falta port `TagRepository.observeForProject`.
- `HomeViewModel.createProject/updateProject/deleteProject/createTag` (`HomeViewModel.kt:419,433,445,459`) gravam em `_errorMessage` que **não é observado** por `HomeScreen` (UI só lê `uiState.errorMessage` de Room). CRUD falha silenciosamente.
- `DeleteProjectDialog` usa `R.string.project_detail_description` (descrição do app) como corpo de confirmação. Texto errado.
- `NewTaskDialog.onConfirm = { viewModel.addTask(...); showCreateDialog = false }` — dialog fecha antes da validação; erro perde-se.
- `AddTagDialog` passa color raw; `Tag.requireValidColor` lança, catch em `_errorMessage` (não exibido).
- `DeadlineField.kt:47` hardcoded `ofPattern("dd/MM/yyyy")`; `R.string.project_detail_new_task_due_date_format` (MM/dd/yyyy em EN) existe mas nunca referenciado. EN vê formato pt.
- `R.string.home_fab_disabled_label`, `project_detail_back`, `project_detail_new_task_due_date_picker_title`, `project_detail_new_task_due_date_format`, `project_detail_task_priority_locked_info` são chaves mortas.
- `HomeScreen.kt:843, 921` ícones errados (`Add` como checkmark, `Sort` como leading de search).
- `ProjectDetailBody` usa `verticalScroll` + `forEach` em vez de `LazyColumn` — jank com muitas tasks.
- `HomeScreen.kt` + `ProjectDetailScreen.kt` Pré-visualizações dependem de `hiltViewModel()` default, não compilam no preview sem Hilt.
- `DefaultHomeUser` hardcode "João Pedro"/"JP" flashing em Loading/SignedOut.
- `renameTask` em `ProjectDetailScreen.kt:314` tem `@Suppress("unused")` — fio morto.

### feature/tasks

- `clearError()` quebrado em ambos VMs. Testes verificam só `viewModel.errorMessage.value`, nunca `uiState.errorMessage` → banner nunca dismiss.
- `feature/tasks` é **read-only** — zero CRUD no escopo R7-R8 (sem `transitionTo`, `changePriority`, `dueDate` display).
- `PriorityBarsCanvas` raw `cornerRadius = CornerRadius(8f, 8f)` em pixels, density-dependent.
- `PriorityChart` contrato frágil: `barColors[index]` + `counts.size` divisor sem `require`. Empty list → NaN.
- `TasksViewModel.toRow` rebuilds `associateBy` per task (O(n*m)). Hoist.
- Nested `flatMapLatest` re-subscribes projects flow on every task emission → use `combine`.
- `DashboardViewModel.currentWeekStartMillis()` avaliado por subscription; `Instant.now()` no `Clock` injectable → week-rollover stale + untestable.
- UTC Monday hardcoded (en expects Sunday).
- `AssistChip(onClick = {})` decorativo em PriorityChip/StatusChip — ainda focusável, TalkBack engana.
- `TasksErrorBanner(message: String)` + `DashboardErrorBanner` suprime `message` (`@Suppress("UNUSED_PARAMETER")`) — sempre mostra a string canônica.
- `retryToken` público em ambos VMs.
- `tasksGraph()` + `dashboardGraph()` split em `TasksRoutes.kt` — fácil esquecer dashboard.
- `TasksViewModel.NO_PROJECT_LABEL = "(sem projeto)"` hardcoded PT em VM companion.
- `TasksViewModel.ERROR_LOAD_FAILED` / `DashboardViewModel.ERROR_LOAD_FAILED` hardcoded PT (UI usa `R.string.tasks_error_load_failed`).
- Pré-visualizações sem `BrainOutTheme { Surface { ... } }`.
- `TasksTestTags.LOADING/EMPTY/LIST` dead (composables não recebem `testTag`).
- Sem `TasksScreenTest` + `TasksRoutesTest`.
- `abortOnError = false` em `feature/tasks/build.gradle.kts:57` esconde mais que só `Instant.parse` — fix com `@SuppressLint` ou baseline, restaurar gate `MissingTranslation`.

### feature/settings

- `app/src/main/res/values*/strings.xml:58-66` **duplica** as 9 chaves `settings_*` já em `feature/settings/res` — dual source of truth, eventual `Duplicate resources`. Deletar de `:app`.
- `SettingsRoutes.SettingsPattern` constante morta. NavHost usa `BrainOutRoutes.Settings`. Remover ou tornar `private`.
- Build.gradle.kts tem `lifecycle-viewmodel-compose`, `hilt-android`, `hilt-navigation-compose`, KSP mas **não tem** `SettingsViewModel` — todas dead deps.
- `SettingsScreen.kt:174-178` `@Preview` sem `BrainOutTheme { Surface { ... } }`.
- `Profile`/`Notifications`/`Theme` cliques são no-op silenciosos (BrainOutNavHost L179-183). No Snackbar/Toast — broken UX.
- `SettingsStructure.kt:4-6` comentário envelhece mal ("não precisa de ViewModel próprio"). Reavaliar em E2.6+.
- `FEATURE_SETTINGS_PACKAGE` constante não referenciada fora do próprio arquivo.

### backend-stub

- `smoke_e2e.py:18-23` — quando `want_status is None`, `return 200, json.loads(out)` hardcoda 200 → asserts `code == 200` são **tautológicos**. Toda POST/PUT/GET (lines 35, 41, 48, 68, 71, 78, 89) aceita 4xx/5xx silencioso.
- `server.py:96-110` — `TaskIn`/`TaskUpsert.project_id: str` sem UUID validator → 404 (project not found) em vez de **400** exigido por §5.1.
- `server.py:65-72 vs 61-62` — `_normalize_id` (body) aceita hex-32 + canonical; `_require_uuid` (path) só canonical. PUT com id hex-32 no path → 400 mesmo UUID lógico.
- `server.py:142-145` — `datetime.utcnow()` deprecated Python 3.12.
- `server.py:174-197, 290-310` — POST idempotente ignora payload divergente (devolve existing). Drift silencioso. 409 ou update quando payload differs.
- `server.py:78-125` — `Project`/`Task`/`Tag` response models nunca bound (`response_model=` ausente). OpenAPI drift vs RemoteDtos.
- Endpoints sem teste: `GET /v1/projects/{id}`, `POST /v1/tasks`, `GET /v1/tasks/{id}`, validation 422 boundaries (color/name/title max_length), PUT body id mismatch em tasks.

### CI / Gradle / docs

- `ci.yml:123, 218` `continue-on-error: true` esconde falhas reais (lint, instrumented).
- `ci.yml:148-217` `backend-integration` sem emulator + sem `setup-java` → `connectedDebugAndroidTest` sempre morre, masked by `continue-on-error`.
- **Versões de actions inválidas**: `upload-artifact@v7` (ci.yml:71,127,139,222), `configure-pages@v6` (pages.yml:39), `deploy-pages@v5`, `upload-pages-artifact@v5` — major atual é `@v4`. Vão quebrar upload.
- `codeql.yml:41` `build-mode: none` para Java/Kotlin (compiladas) → análise vazia. `continue-on-error: true` (L26) mascara.
- JDK drift: CI 17 vs release 21 vs backend-integration sem JDK; cache key única compartilhada entre JDKs (cache pollution).
- `core/data/build.gradle.kts:118-121` `abortOnError = false` esconde `MissingTranslation`/NewApi do data layer.
- `feature/tasks/build.gradle.kts:57` `abortOnError = false` esconde lint da feature.
- `koverLog` line 87 — exclusão `*.remote.*` mascara código de produção (`RemoteDataSource` error propagation + `HolidayRemoteDataSource` cache mutex).
- `gradle.projectsEvaluated` hook frágil (string matching, late evaluation).
- Common Android block (compileSdk/minSdk/javaVersion/jvmTarget/buildTypes) duplicado ×7 módulos. Detekt block duplicado ×8. `missingDimensionStrategy` ×4. `testOptions.unitTests.isIncludeAndroidResources` duplicado em `:app` (linhas 139 **e** 165).
- `local.properties` leitura duplicada (`:app:32-38` + `:core/data:19-24`).
- `:app:139-143 E 165-169` `testOptions` block declarado duas vezes — segundo silenciosamente override.
- `composeUiTest = "1.7.5"` em `libs.versions.toml:48` é alias morto. Não referenciado.
- `README.md:17` link para `./docs/RELATORIO-TECNICO.md` quebrado.
- `ARQUITETURA.md:99,100,103` — stack table stale (Gradle 8.7/AGP 8.7; Ktor nunca usado; escolhemos WorkManager não AlarmManager).
- `ARQUITETURA.md:243` path `:core:data/network/` deveria ser `remote/`.
- `ARQUITETURA.md:193-197` AD-1, AD-2, AD-4, AD-5 ainda com "a definir". AD-6 (L249) é row órfã fora da tabela §8.
- `ROADMAP.md:623` + `CONTRIBUTING.md:106` placeholder "GitHub Projects (link a adicionar)".
- `CONTRIBUTING.md:63` exemplo `Kotlin 2.0.21` stale.
- `keystore.properties.example` é stale — build ignora, assinatura é via env.

### Kover 60% gate em risco

P0+P1 uncovered LoC ≈ 550 :core:data + 60 :core:domain + ~300 feature. Sem testes pra `TaskRepositoryImpl`/`ProjectRepositoryImpl`/`TagRepositoryImpl`/`BrainOutSyncDispatcher`/`PepperProvider`, porta do gate cai bem abaixo de 60%.

---

## P2 — refactor técnico, decompor quando conveniente

- **Decompose:** `HomeScreen.kt (1403)`, `ProjectDetailScreen.kt (1027)`, `DashboardScreen.kt (593)`, `HomeViewModel.kt (583)`, `ProjectDetailScreen.kt (1027)`. Extrair `*TopBar`, `*BottomBar`, `*Fab`, `*OfflineBanner`, `*States`, `*FilterRow`, `*SearchFilter`, `*EmptyStates`, `*Card`, `*Dialogs`, `*TestTags` em arquivos de ≤200 LoC.
- **Mover `resolveAuthMessage`/`resolveHomeErrorMessage`/`resolveProjectDetailMessage`/`resolveCreateProjectNameError`** para um `ui/common/Messages.kt` (atualmente espalhados por feature).
- **`SettingsRoutesTest`, `ProjectsRoutesTest`, `AuthRoutesTest`** — todos fracos. Expandir para validar estabilidade cross-module ou deletar.
- **`AuthViewModel.submitLogin` etc** — adicionar `runCatching { withContext(Dispatchers.IO) { ... } }` (CPU-bound hash roda em Main hoje).
- **`RemoteDtos.id`** — explícito required em tasks/projects (R6).
- **Refatorar `core/data` + `core/domain` ParaConstantsHelper** — extrair `CHECK_DEADLINE_PRE_WINDOW_DAYS`, `REMINDER_OFFSET_HOURS` para shared constants.
- **Composition cleanup:** `HomeScreen.kt:1088` `AssistChipDefaults.assistChipBorder(enabled = true, ...)` default redundante.
- **`@file:Suppress("TooManyFunctions", "MagicNumber", "LongMethod")`** suprime sinais de bloat (corrigir decompondo, não suprimindo).
- **`SnapshotStateList<String>`** leak em `CreateProjectDialogBody:1225` — tipar `List<String>` + callbacks.
- **`HomeUser.kt:23-26`** `DefaultHomeUser("João Pedro"/"JP")` flashing em Loading/SignedOut. Trocar por shimmer/genérico.

---

## Gaps de teste (priorizado)

| Severidade | Módulo | Arquivo | Falta |
|---|---|---|---|
| P0 | core/data | `repository/{Task,Project,Tag}RepositoryImpl.kt` | nenhum teste (dual-write, cascade, validation, sync enqueue) |
| P0 | core/data | `sync/BrainOutSyncDispatcher.kt` | nenhum teste (route map, IOException/5xx/4xx classification, payload decode) |
| P0 | core/data | `security/PepperProvider.kt` | nenhum teste (B1 capturável: `bytes()` constante para um dado alias) |
| P0 | core/domain | `usecase/{CreateTag,UpdateProject,DeleteProject,DeleteTag}UseCaseTest.kt` | ausentes (alvos: piso Kover 60%) |
| P0 | app | `notifications/DeadlineReceiver.kt` | sem teste (goAsync, cancel pending, dismiss) |
| P0 | app | `notifications/DeadlineWorker.kt` | sem teste (publish path; reload Room; permission gate) |
| P1 | core/data | `local/Migrations.kt` 2→3 e 3→4 | só 1→2 em `MigrationTest`. `completed_at`, `pending_ops` CREATE não-rodam em SQLite real |
| P1 | core/data | `session/ActiveUserProvider.kt` | nenhum teste (orphan-session clear, flatMapLatest) |
| P1 | core/data | `sync/ConnectivityObserver.kt` | nenhum teste (`ShadowConnectivityManager` disponível) |
| P1 | core/domain | `TaskTest` | não afirma `completedAt` populating/clearing em transições (precisa clock injection) |
| P1 | core/domain | `TaskPriorityRulesTest.update integral` | duplica cenário; nem exercita `copy` |
| P1 | feature/settings | `ui/SettingsScreen.kt` | `SettingsScreenTest` + `SettingsViewModelTest` (VM não existe; gap maior do repo) |
| P1 | feature/projects | `ui/home/HomeScreen.kt` + `ui/projectdetail/ProjectDetailScreen.kt` | Compose UI test (testTag infra pronta) |
| P1 | feature/projects | `DeadlineField.kt` | `pickerDateToDeadline`/`deadlineToPickerDate` round-trip + DST |
| P1 | feature/auth | `ui/register/RegisterScreen.kt` | `RegisterScreenTest` |
| P1 | feature/auth | `viewmodel/AuthViewModelTest.kt` | `validateName` (empty/>120), unexpected `Throwable` path, DomainException fallback |
| P1 | feature/tasks | `ui/TasksScreen.kt` | `TasksScreenTest` + `TasksRoutesTest` |
| P2 | core/data | `session/SessionStore.kt` | DataStore round-trip + `clear` |
| P2 | core/data | `local/dao/DashboardAggregates.kt` | `observeCountByPriority` 0..4 fill |
| P2 | core/domain | `TaskCompletionStats` / `TaskPriorityCount` / `SortOrder` | sem teste |
| P2 | feature/auth | `ui/splash/SplashScreen.kt` | `SplashScreenTest` |

---

## Workflows produtivos (21 sugestões, agrupadas)

### Quick wins (< 1h, imediato)
1. **Configuration cache** em `gradle.properties` — `org.gradle.configuration-cache=true` + `-XX:+UseParallelGC -XX:MaxMetaspaceSize=512m`.
2. **Trocar `actions/cache@v4` → `gradle/actions/setup-gradle@v6`** (`cache-read-only: ${{ github.event_name == 'pull_request' }}`).
3. **`paths-ignore` em `ci.yml`** para docs-only PRs (`docs/**`, `*.md`).
4. **Pre-commit hook** (`scripts/pre-commit.sh`) rodando `./gradlew -Pprecommit=true detekt ktlintCheck` apenas nos arquivos staged (≈2-3s/commit).
5. **Pre-push hook** (`scripts/pre-push.sh`) rodando `ktlintCheck detekt testDebugUnitTest :core:domain:koverVerify :core:data:koverVerify` (~3-5min warm, salva 3-5min CI roundtrip).
6. **Alias `gw='./gradlew --build-cache --console=plain'`** no `~/.bashrc`.
7. **Android Studio**: Gradle runner (Build, Execution, Deployment → Build Tools → Gradle).

### Tooling upgrades (1-2 dias)
8. **`release-please`** (`.github/workflows/release-please.yml`) → `CHANGELOG.md` auto-generated, elimina bump manual.
9. **Translation drift detector** (`scripts/check-i18n.sh`) — `comm` entre `values/keys` e `values-en/keys` antes do `MissingTranslation` do lint.
10. **`@PreviewLightDark`** no `@Preview` (zero deps, dark + light).
11. **Robolectric SDK matrix** — `@Config(sdk = [24, 30, 35])` em `:core:data/migrations` e `:core:domain/deadlines`. Custo zero, cobertura cross-API.
12. **ADR directory** (`docs/adr/0001-*.md` em MADR) — separa "what" de "why-decided"; AD-6 já maduro, AD-1/2/4/5 pendentes de decidir.
13. **Coverage badge** (Shields.io SVG) — feedback permanente (atualmente artifact 14 dias).
14. **Timber wrapper** — substitui `Log.d/w/e` direto (preparação para logging centralizado sem Crashlytics).
15. **Dependabot auto-merge** (`.github/workflows/auto-merge-deps.yml` quando `actor == 'dependabot[bot]'` + label `dependencies`).

### Process discipline
16. **PR template** com seções `## Requisito(s) atendido(s) — Refs: R#, E#` e `## Test plan` (checklist).
17. **`scripts/merge-if-green.sh`** — `gh pr checks $PR --json name,state | jq -e 'all(.state=="SUCCESS")' && gh pr merge $PR --squash --delete-branch --auto`. Usar só em `chore:`/`docs:`/`ci:`.
18. **Weekly ROADMAP drift review** (calendar recurring 15min, Domingos 18:00 BRT): `git log --oneline main..HEAD` + GitHub Projects + issues `impediment` > 7 dias. Mantém FPI (CONTRIBUTING §Responsabilidade individual).
19. **ADR auto-merge** — adicionar `dev`-merge labels documentado em CONTRIBUTING §6.

### Skip / Defer
- **Renovate** (já tem Dependabot).
- **Sentry/Crashlytics** (overkill acadêmico, requer `google-services.json` gitignored).
- **Modularização adicional** (estrutura já está limpa).
- **Showkase** (suficiente `@Preview`).

---

## Ordem de ataque sugerida (1 fim de semana)

1. **P0-5, P0-6** (CI quebra silenciosamente) — corrigir comandos + secrets, validar com PR canário.
2. **P0-3** (pepper) — corrigir persistência; criar teste determinístico; rodar `koverVerify` antes/depois.
3. **P0-1** (signOut scope) + **P0-2** (CompleteTaskWorker idempotência) — escrever testes de regressão primeiro (TDD), depois fix.
4. **Workflow quick wins** (`#1`, `#2`, `#4`, `#5`) — config cache + setup-gradle + path-filter + pre-commit/push hooks.
5. **Workday 2** — `feature/auth` cleanup (duplicate errors, events channel, password 8 em domain), `feature/projects` B1-B5.
6. **Semana 2** — decompose `HomeScreen`/`ProjectDetailScreen`; cobertura core:data (P0 gaps); ADR doc; reabilitar `lint-abortOnError`.

---

## Estado atual já bom (manter)

- 100% dos `.kt/.kts/.xml` com header `// João Pedro G M Silva - PUC Goiás ADS - 20251012000740`.
- Comentários em pt-BR com `Refs: R#/E#`.
- Kover 60% gate funcionando em `:core:domain` + `:core:data`.
- Detekt maxIssues=0 + LongMethod 80 + LongParameterList 8 (convention-tuned).
- Bilinguismo 1:1 nas strings.xml (parity OK em todos os módulos lidos).
- Backend stub cliente-supplied UUID / upsert idempotente / DELETE idempotente — contrato sólido.
- `:core:domain` zero Android/Room/Hilt (R12) ✓.
- TaskStatus transition matrix + RN02 bloqueio em `changePriority` ✓.
- WorkManager `OneTimeWorkRequest` + sem `SCHEDULE_EXACT_ALARM` (AD-6) ✓.
- Holiday BrasilAPI via `BuildConfig.HOLIDAYS_BASE_URL`, network failure renderiza neutro (não crash) ✓.
- `coreLibraryDesugaring` no `:app` para `java.time#toEpochMilli` no minSdk 24 ✓.
- Backend stub `pytest -q` 14 casos verdes; `Dockerfile python:3.12-slim` válido ✓.

---

## Correções aplicadas (2026-09-24)

### P0 — corrigidos

- **P0-1** `BrainOutNavHost.kt:174` — `signOut()` migrado de `rememberCoroutineScope` para `LocalLifecycleOwner.lifecycleScope.launch { signOut(); navigate(Login) }`. DataStore write completa antes do navigate.
- **P0-2** `CompleteTaskWorker.kt:35-48` — captura `DomainException` (permanente, idempotente → `success()`) e `IOException` (transient → `retry()`). Teste atualizado (linhas 117-141) + `TaskRepositoryImpl.completeAndCascade/changeStatus/reopenAndCascade` agora lançam `TaskNotFoundException` (que estende `DomainException`).
- **P0-3** `PepperProvider.kt:48-50` — substituído SHA-256 de alias público por `EncryptedSharedPreferences` com salt aleatório de 16 bytes gerado uma vez por instalação via `SecureRandom`. Docstring antigo que afirmava "pepper nunca deixa o hardware" removido.
- **P0-4** Tag sync alinhada com R6 (client-supplied UUID). `TagSyncPayload` ganhou `id`. `TagCreateDto` ganhou `id: String? = null`. `RemoteDataSource.createTag` aceita id opcional. `BrainOutSyncDispatcher.route` TAG CREATE passa `payload.id`. Backend stub: `TagIn` aceita `id` opcional + UUID validator + `create_tag` retorna registro existente em vez de duplicar. `backend-stub/AGENTS.md` atualizado. 2 testes novos cobrindo o contrato (`test_tag_accepts_client_supplied_id_and_replay_is_idempotent`, `test_tag_rejects_non_canonical_client_id`).
- **P0-5** `ci.yml:107,122` — `testDebugUnitTest` → `testDevDebugUnitTest`, `lintDebug` → `lintDevDebug` (variantes explícitas para que `:app`/`:core:data` entrem no gate). `:app` lint agora honrado (antes pulado). `backend-integration`: removido `connectedDebugAndroidTest` (sem emulador + flavor-mismatch) e substituído por `:core:data:testDevDebugUnitTest :app:testDevDebugUnitTest` apontando para o stub live via `BASE_URL`. `upload-artifact@v7` (inválido) → `@v4`.
- **P0-6** `release-apk.yml` — adicionado `Validate signing secrets` step que checa os 4 secrets antes do build + `Verify .aab signature` (jarsigner) antes do upload. Impossível subir .aab não-assinado verde.
- **P0-7** `TagNotFoundException.kt:5-12` — agora estende `DomainException` (não `RuntimeException`). Teste atualizado.
- **P0-8** `ChangeTaskStatusUseCase.kt:60` — `error("…")` → `throw TaskNotFoundException(taskId)`. `DomainException.kt:32-37` ganhou `TaskNotFoundException` e `ProjectNotFoundException`. Novo teste em `ChangeTaskStatusUseCaseTest.kt:148-167`.

### P1 — corrigidos

- **P1 (auth)** `AuthViewModel.kt` — erros de validação de campo não duplicam mais em `errorMessage` (banner fica só para erros de domínio não-campo). Testes ajustados.
- **P1 (auth)** `AuthUiState.kt` — removida constante local `MIN_PASSWORD_LENGTH`. `CreateUserUseCase.kt:62-66` e `AuthenticateUserUseCase.kt:38-40` agora enforcam `length >= MIN_PASSWORD_LENGTH` (constante no domínio). Testes com `"secret"` (6 chars) atualizados para `"secretlong"`/`"wrongpwd"`.
- **P1 (projects)** `HomeScreen.kt:128-131` — `actionError by viewModel.errorMessage.collectAsStateWithLifecycle()` adicionado; passa `actionError ?: listState.errorMessage` ao `HomeProjectsContent` para que falhas de CRUD (createProject/updateProject/deleteProject/createTag) tornem-se visíveis (antes iam para `_errorMessage` privado, nunca observado).

### Não corrigidos (P2 / fora de escopo deste lote)

- Decomposição `HomeScreen.kt` (1403), `ProjectDetailScreen.kt` (1027), `DashboardScreen.kt` (593), `HomeViewModel.kt` (583), `ProjectDetailViewModel.kt` (443) em arquivos ≤200 LoC.
- `HomeViewModel.buildHomeUiState` mostra tags erradas (todas do owner em vez de tags por projeto) — exige port `TagRepository.observeForProject` + DAO join.
- `DeleteProjectDialog` usa `R.string.project_detail_description` (descrição do app) como corpo.
- `NewTaskDialog.onConfirm` fecha antes de validação.
- `DeadlineField` hardcoded `dd/MM/yyyy` em vez de `R.string.project_detail_new_task_due_date_format`.
- 5 chaves mortas em strings.xml: `home_fab_disabled_label`, `project_detail_back`, `project_detail_new_task_due_date_picker_title`, `project_detail_new_task_due_date_format`, `project_detail_task_priority_locked_info`.
- `feature/projects/src/main/res/values*/strings.xml` duplica 9 chaves `settings_*` em `feature/settings/res/`.
- `ProjectDetailBody` usa `verticalScroll + forEach` em vez de `LazyColumn`.
- `feature/auth/res` contém 4 chaves `home_*` duplicadas.
- Auth: `SharedFlow` com default config → `Channel(BUFFERED)`. `AuthEvent.FocusField` + `AuthField` enum são dead code.
- Auth: `resolveAuthMessage` em composable; `else -> message` vaza PT em EN.
- Auth: `LoginScreen.kt:3-4` comentário referencia `SessionViewModel` que não existe.
- `feature/tasks` — `clearError()` quebrado, read-only (zero CRUD), `PriorityBarsCanvas` raw px, UTC Monday hardcoded.
- `feature/settings` — sem VM, sem screen test, sem Compose `BrainOutTheme { Surface { … } }` wrapper em `@Preview`.
- core/data: `ProjectRepositoryImpl.require(tag.ownerId == ownerId)` lança `IllegalArgumentException` crua.
- core/data: `ProjectCreateDto.id: String? = null` em PUT pode omitir body → 400 stub.
- core/data: `RemoteDataSource.updateProject` silenciosamente loga + rethroa; sem cancelar CancellationException.
- core/data: `HttpLoggingInterceptor.Level.BASIC` sempre on (release vaza URL).
- core/data: TAG UPDATE rejeitado por design (aceitável).
- core/data: `last-writer-wins via updated_at` ausente end-to-end (precisa migration v5 + stub compare).
- core/data: 4 `*RepositoryImpl` sem testes diretos (gap conhecido, AGENTS).
- core/ui: `ThemeSelectionTest` tautológico; subset-only `ContrastRatioTest`; dynamic color path sem teste.
- core/ui: 23 hardcoded `RoundedCornerShape` em `feature/*` violando contrato do `Shape.kt`.
- backend-stub: `smoke_e2e.py:18-23` hardcoda `200` quando `want_status is None`; `TaskUpsert.project_id` sem UUID validator (404 em vez de 400); `_normalize_id` aceita hex-32 mas `_require_uuid` não (assimetria path/body); `datetime.utcnow()` deprecated.
- CI: CodeQL `build-mode: none` para Java/Kotlin (análise vazia); backend-integration continua sem `setup-java` (atualizei, mas só até onde tem JDK já implícito).
- docs: README link para RELATORIO-TECNICO.md quebrado; ARQUITETURA stack table stale.
- Workflow (docs/): pre-commit hook, pre-push hook, configuration cache, `gradle/actions/setup-gradle@v6`, `release-please`, ADRs — todos não implementados.

Próxima rodada: P2 decompose + workflow scripts + ADR directory (ref. §Workflows).

### Verificação executada

- `:core:domain` — `gradle :core:domain:test` BUILD SUCCESSFUL (118 testes passaram após correções).
- `:core:domain` — `:core:domain:detekt` + `ktlintCheck` BUILD SUCCESSFUL.
- `:core:data` / `:app` / `:feature:*` — não compila localmente (ambiente sem Android SDK); verificável em CI após merge.
- `backend-stub/server.py` + `tests/test_contract.py` — `python3 -m py_compile` OK em ambos.
- `pytest` — não executável (venv sem wheels de pydantic no ambiente); sintaxe OK.
