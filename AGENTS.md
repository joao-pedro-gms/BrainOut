# Repository Guidelines

Practical guide for AI assistants working on the **BrainOut** codebase. Verified against the current `main` tree (AGP 9.4.1, KGP 2.3.20, Gradle 9.7.1, JDK 17 bytecode, Compose BOM 2024.10.01).

## Project Overview

BrainOut is a **personal project/task manager** Android app — Kotlin + Jetpack Compose + Material 3, offline-first with Room + WorkManager sync, backend driven by a FastAPI stub at `/v1/*`. Built as the PUC Goiás ADS 2026/2 *Projeto Integrador* (author: João Pedro G M Silva, matrícula `20251012000740`). Single developer; Kover 60% coverage gate on `:core:domain` and `:core:data`.

## Architecture & Data Flow

Strict multi-module graph (no cycles). Dependencies only point downward:

```
:app  →  :feature:{auth,projects,tasks,settings}
              └─→  :core:ui
              └─→  :core:data  ──→  :core:domain  (pure JVM, leaf)
              └─→  :core:domain
```

- **`:app`** — orchestrator: `BrainOutApplication` (`@HiltAndroidApp`, `Configuration.Provider` for HiltWorkerFactory), `MainActivity` (`@AndroidEntryPoint` + Compose), `BrainOutNavHost`. Holds WorkManager workers (`SyncWorker`, `DeadlineWorker`) and `WorkManagerDeadlineScheduler` (impl of the domain port).
- **`:core:domain`** — pure **Kotlin JVM** (no Android, no Hilt, no Compose). Only `kotlinx-coroutines-core` + `javax.inject` (`@Inject`). Holds repository interfaces, use cases (`CreateUserUseCase`, `ChangeTaskStatusUseCase`, …), domain models, and the `DeadlineNotificationScheduler` port.
- **`:core:data`** — Android library with **flavors** `environment`/`dev`+`prod`; `BaseUrl` from `BuildConfig.BASE_URL`. Room (`BrainOutDatabase` v4, exported schemas), Retrofit (`BrainOutApi`), DataStore Preferences, kotlinx-serialization, Hilt. DI wiring lives mostly in `DataModule` (`@InstallIn(SingletonComponent::class)`), com uma exceção (ver *Conventions*).
- **`:core:ui`** — Compose-only module (Material 3 theme, tokens, cores, formas, tipografia). No Hilt, no Room, no DataStore. Sem flavors **e sem** `missingDimensionStrategy` — é a única lib Android do grafo que não consome `:core:data`, logo não precisa fixar a dimensão `environment`.
- **`:feature:*`** — each owns its `*Routes.kt` + ViewModels + Compose screens + test tags. All depend on `:core:domain` + `:core:data` + `:core:ui`. Each declares `missingDimensionStrategy("environment", "dev")` to match `:core:data` flavors.

**Data flow per request** (canonical example — see `ProjectDetailViewModel`):

```
Compose Screen
    → @HiltViewModel + SavedStateHandle
        → use case (:core:domain)
            → repository (:core:data)
                → DAO (Room) — emits via Flow
                → enqueues PendingOpEntity in same Room transaction (offline-first)
    ← StateFlow<UiState> (stateIn with WhileSubscribed(5_000))
```

Async patterns:
- `Flow` from DAOs → `.map { rows -> rows.map { it.toDomain() } }` → ViewModel `.stateIn(viewModelScope, WhileSubscribed(5_000), initial)` exposed as immutable `data class XxxUiState(...)`.
- Retry pattern: `_retryToken: MutableStateFlow<Int>` + `flatMapLatest` discards prior Flow and re-subscribes after failures (see `ProjectDetailViewModel`, `DashboardViewModel`).
- Connectivity via `AndroidConnectivityObserver`; cross-cutting `PendingSyncMonitor` combined into per-screen sync state.
- **Dispatchers:** os repositórios de `:core:data` **não** fazem `withContext(Dispatchers.IO)` — `grep 'Dispatchers.IO' core/data/src/main` retorna **zero** ocorrências. O I/O dos DAOs `suspend` roda no dispatcher interno do Room, e o `Dispatchers.IO` explícito só aparece em `MainActivity` (resolução da start-destination) e em `DeadlineReceiver`. Isole a camada com `suspend`/`Flow`, não com troca de dispatcher.

Navigation (estado real, não o ideal):
- **Duas fontes de verdade de rotas.** Em uso: `BrainOutRoutes` (`app/src/main/.../navigation/BrainOutRoutes.kt`) para Splash/Login/Register/Home/Settings/ProjectDetail, e `TasksRoutes` (`:feature:tasks`) para Tasks/Dashboard. **Não usados em produção:** `AuthRoutes`, `ProjectsRoutes` e `SettingsRoutes` existem nos respectivos módulos mas são consumidos **apenas pelos próprios testes** (`*RoutesTest.kt`) — o `NavHost` não os importa. Ao adicionar rota, edite `BrainOutRoutes` (ou o `*Routes` da feature que a expõe) e não crie um terceiro objeto.
- **Só `:feature:tasks` expõe extensões de grafo:** `NavGraphBuilder.tasksGraph()` e `dashboardGraph()`. Splash/Login/Register/Home/ProjectDetail/Settings são montados por funções **privadas** em `BrainOutNavHost.kt` (`addSplashRoute`, `addLoginRoute`, …), que instanciam os composables das features diretamente.
- Mesmo assim, **nunca hardcode string de rota**: use `object XxxRoutes` + helper `fun xRoute(args)`.
- Sign-out: `activeUserProvider.signOut()` then `navigate(login) { popUpTo(0) { inclusive = true } }`.

Sync (offline-first):
- Mutations call `pendingOpDao.enqueueInTx(op) { ... }` in one Room transaction (see `TaskRepositoryImpl`).
- `SyncWorker` (`@HiltWorker` + `CoroutineWorker`) drains `pending_ops` with batch size 50; 4xx → discard (`Result.success`), 5xx/IOException → retry (`Result.retry`).
- **Onde o glue de sync mora (duas camadas):** `app/src/main/.../sync/` tem `SyncWorker`, `SyncScheduler`, `SyncConnectivityWatcher` e `SyncModule` (trabalho periódico do WorkManager). A fila/offline em si fica em `:core:data`: `sync/SyncDispatcher.kt` (a interface), `BrainOutSyncDispatcher.kt`, `PendingSyncMonitor.kt`, `ConnectivityObserver.kt`. Não há `SyncDispatcher.kt` em `:app`.

## Key Directories

| Dir | Purpose |
|---|---|
| `app/src/main/kotlin/.../BrainOutApplication.kt` | Hilt bootstrap + WorkManager Configuration provider. |
| `app/src/main/kotlin/.../MainActivity.kt` | Compose host, start-destination resolution, `POST_NOTIFICATIONS` request (Tiramisu+). |
| `app/src/main/kotlin/.../navigation/BrainOutNavHost.kt` | Root NavHost; funções privadas por destino + `tasksGraph()`/`dashboardGraph()`. |
| `app/src/main/kotlin/.../navigation/BrainOutRoutes.kt` | Rotas raiz em uso (Splash→Login→Home→ProjectDetail/Settings). |
| `app/src/main/kotlin/.../sync/` | `SyncWorker`, `SyncScheduler`, `SyncConnectivityWatcher`, `SyncModule` (glue WorkManager). |
| `app/src/main/kotlin/.../notifications/` | `WorkManagerDeadlineScheduler` (impl of domain port), `DeadlineWorker`, `DeadlineReceiver`, `CompleteTaskWorker`, `NotificationModule`. |
| `core/domain/src/main/kotlin/.../usecase/` | Use cases. Kover 60% gate — every new use case needs a test under `src/test/`. |
| `core/domain/src/main/kotlin/.../notification/DeadlineNotificationScheduler.kt` | Port interface; Android-free. |
| `core/data/src/main/kotlin/.../local/BrainOutDatabase.kt` | Room DB v4; `pending_ops` table is the sync queue. Schemas in `core/data/schemas/`. |
| `core/data/src/main/kotlin/.../remote/BrainOutApi.kt` | Retrofit interface; all `suspend`. |
| `core/data/src/main/kotlin/.../di/DataModule.kt` | DI wiring principal do `:core:data`. `PepperProvider.Factory` fica **fora**, no próprio arquivo de `security/`. |
| `core/data/src/main/kotlin/.../security/PepperProvider.kt` | Pepper + `@Module object Factory` (segundo módulo Hilt do módulo). |
| `core/data/src/main/kotlin/.../repository/` | Repository impls; each delegates mutations to `pendingOpDao.enqueueInTx`. |
| `core/data/src/main/kotlin/.../sync/` | `SyncDispatcher` (interface), `BrainOutSyncDispatcher`, `PendingSyncMonitor`, `ConnectivityObserver`. |
| `core/ui/src/main/kotlin/.../theme/` | Material 3 base (`Color/Shape/Theme/Type`) **+ identidade Neo**: `NeoColor.kt`, `NeoTokens.kt`, `NeoTypography.kt` (que declara `object NeoFonts`), `BrainOutNeoTheme.kt`. `BrainOutTheme` já delega para o tema Neo. |
| `core/ui/src/main/res/` | `font/` com **5 fontes** locais (3× Archivo — bold/extra bold/semi bold — e 2× Public Sans — regular/semi bold) e `drawable/` com **20 drawables `neo_*`** (ícones/vetores da identidade). |
| `feature/<x>/src/main/kotlin/.../ui/` | Screens + `*TestTags` objects for Compose UI tests. |
| `feature/<x>/src/main/kotlin/.../viewmodel/` or `ui/<x>/` | `@HiltViewModel` + `data class XxxUiState`. |
| `feature/<x>/src/main/kotlin/.../navigation/*Routes.kt` | Constantes de rota por feature; ver *Navigation* acima (uso desigual). |
| `backend-stub/server.py` | FastAPI stub — source of truth for `/v1/*` contract. |
| `docs/ROADMAP.md`, `docs/ARQUITETURA.md`, `docs/CI-CD.md`, `docs/CONTRIBUTING.md` | Requirements R1–R14, E1.x–E5.x cycles, pipeline, conventional commits. |
| `Documentos/` | Deliverable PDFs only — never edit. |

## Development Commands

All Gradle commands from repo root. The build is verified on JDK 21 (what `ci.yml` and `release-apk.yml` provision). With a standalone JDK 21+: `export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64`; with no standalone JDK, the Android Studio JBR works too (`export JAVA_HOME=/opt/android-studio/jbr`). Gradle 9.7.1/AGP 9.4.1 accept a floor of 17; bytecode target stays Java 17.

```bash
# Build
./gradlew :app:assembleDevDebug              # daily dev build
./gradlew :app:assembleProdDebug             # prod build (BASE_URL placeholder https://TBD/)
./gradlew :app:assembleRelease               # unsigned unless BRAINOUT_* secrets are set

# Unit tests (includes :core:domain via gradle.projectsEvaluated wire-up)
./gradlew testDevDebugUnitTest               # what CI runs for flavored modules (:app, :core:data)
./gradlew testDebugUnitTest                  # un-flavored Android modules (:feature:*, :core:ui)
./gradlew :core:domain:test                  # pure JVM tests
./gradlew :core:data:testDevDebugUnitTest    # Robolectric + MockWebServer
./gradlew :core:ui:testDebugUnitTest         # tokens/tema Neo (Robolectric, recursos Android)
./gradlew :feature:auth:testDebugUnitTest    # ViewModel tests (R2)
./gradlew :feature:projects:testDebugUnitTest
./gradlew :feature:tasks:testDebugUnitTest
./gradlew :feature:settings:testDebugUnitTest
./gradlew :app:testDevDebugUnitTest          # smoke + WorkManager + sync tests

# Instrumented tests (emulator) — compilam; rodam em `connected*`
./gradlew :core:data:assembleDevDebugAndroidTest    # MigrationTest + UserDaoInstrumentedTest
./gradlew :feature:auth:assembleDebugAndroidTest   # LoginScreenTest

# Coverage (60% bound, core only)
./gradlew :core:domain:koverVerify :core:data:koverVerify
./gradlew koverMergedHtmlReport              # aggregator

# Static analysis (CI runs these together)
./gradlew ktlintCheck detekt
./gradlew :app:lintDevDebug                  # Android lint — i18n MissingTranslation gate

# Backend stub (Python 3.12)
cd backend-stub && python -m venv .venv && source .venv/bin/activate \
  && pip install -r requirements.txt \
  && python -m uvicorn server:app --host 0.0.0.0 --port 8000
```

> **Task names dependem do flavor.** Só `:app` e `:core:data` declaram `dev`/`prod`; as features e o `:core:ui` **não têm flavor próprio** e apenas fixam a dimensão, então a task delas é `testDebugUnitTest` (`assembleDebugAndroidTest`), **não** `testDevDebugUnitTest`. O CI roda `testDevDebugUnitTest` + `:core:ui:testDebugUnitTest` explicitamente.

Override dev URL via env var `BASE_URL` (precedence, used by CI) or `local.properties` (key `brainout.baseUrl.dev`). Emulator reaches host at `http://10.0.2.2:8000/`.

## Code Conventions & Common Patterns

- **Author header** — every `.kt`/`.kts`/`.xml` file starts with `// João Pedro G M Silva - PUC Goiás ADS - 20251012000740`.
- **Packages** lowercase only; strings `snake_case` with screen/feature prefix (`login_`, `home_`, `task_`, `project_detail_`, `bottom_tab_`, `common_`). Detekt `PackageNaming` enforces.
- **i18n gate** — `values/strings.xml` and `values-en/strings.xml` must stay in sync. `:app`, `:feature:auth`, `:feature:projects`, `:feature:settings` enforce `MissingTranslation = error` + `lint { abortOnError = true }`. **Dois módulos estão relaxados e isso é conhecido, não intencional:** `:feature:tasks` (`Instant#parse` em `@Preview`) e `:core:data` (8 erros `[NewApi]` pendentes de `java.time`/`java.util.Base64` com minSdk 24, aguardando `coreLibraryDesugaring`) usam `abortOnError = false`. Nos módulos rígidos, nunca suprima — traduza.
- **AGP 9 built-in Kotlin** — `kotlin-android` plugin is **removed** from the version catalog. Do not re-add. Remaining Kotlin plugins: `kotlin-jvm` (only `:core:domain`), `kotlin-compose`, `kotlin-serialization` (`:core:data`).
- **Bytecode target** — Java 17 in all 8 modules (`jvmTarget.set(JvmTarget.JVM_17)`); build JDK is 21 (CI + release provision Temurin 21; JBR via `JAVA_HOME` locally). Use `kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }` — `kotlinOptions` is gone in AGP 9.
- **Flavors** — `dev` (BASE_URL `http://10.0.2.2:8000/`, overridable via env var `BASE_URL` — CI points it at the dockerized stub — or `local.properties`) and `prod` (placeholder `https://TBD/`, decision E3.1). Only `:app` and `:core:data` declare flavors; every `:feature:*` fixes `missingDimensionStrategy("environment", "dev")`. `:core:ui` não consome `:core:data` e não precisa da dimensão.
- **DI** — Hilt everywhere; ViewModels via `@HiltViewModel` + `@Inject` constructor. Domain layer may use `javax.inject.Inject` only. New `:core:data` bindings go in `DataModule` **ou** em um `@Module` dedicado quando o binding for de um componente autocontido — hoje são exatamente dois: `di/DataModule.kt` e o `PepperProvider.Factory` (`security/PepperProvider.kt:77-83`), que fornece o pepper ao hasher.
- **State management** — immutable `data class XxxUiState(...)` (per-screen error slots as nullable fields, no sealed class); expose `val uiState: StateFlow<XxxUiState>` via `stateIn(viewModelScope, WhileSubscribed(5_000), initial)`. One-shot events via `Channel`/`SharedFlow` or `MutableStateFlow<String>` for errorMessage + retry pattern with `_retryToken` + `flatMapLatest`.
- **Module marker** — `PackageMarker.kt` (`internal const val <MODULE>_PACKAGE: String`) existe em **6 dos 8** módulos. **Lacuna conhecida:** falta em `:app` e em `:core:ui` — o Kover exclui `*.PackageMarker` por padrão, então a ausência não quebra nada, mas padronize ao criar arquivo novo nesses dois módulos.
- **Repository mutations** — always go through `pendingOpDao.enqueueInTx(op) { ... }` inside one Room transaction (offline-first dual-write, E3.3). Cascade ops use `@Transaction` DAO methods (`cascadeCompleteTask`, `cascadeReopenTask`).
- **Navigation** — never hardcode route strings; use `object XxxRoutes` constants + helper `fun xRoute(args)` builders. Só `:feature:tasks` expõe extensões `NavGraphBuilder.*Graph()` (registradas por `:app`); as demais features são registradas por funções privadas do `BrainOutNavHost`, que conhece os composables. Ao padronizar, beware das duas fontes de verdade de rotas descritas em *Navigation*.
- **Compose test tags** — colocated `object XxxTestTags` per screen (e.g. `project_detail_new_task_fab`); snake_case, screen-prefixed. Used by Robolectric + Compose UI tests via `onNodeWithTag`.
- **Domain errors** — `DomainException` é a **raiz única** de `core.domain.error.*` e estende `IllegalArgumentException` (contrato de `kotlin.require()` nos modelos, coberto por testes). `BusinessRuleException` também estende `DomainException` (hierarquia única — um `catch (DomainException)` agora captura violações de RN). Filhos: `InvalidModelException`, `DuplicateEmailException`, `InvalidCredentialsException`, `InvalidStateTransitionException`, `TaskNotFoundException`, `ProjectNotFoundException`, `TagOwnershipException`, `TagNotFoundException`. Repositories throw domain exceptions; ViewModels catch and surface into `errorMessage`.
- **No commented-out code** (`CONTRIBUTING.md`, R12). Delete dead code; don't disable with comments.
- **Commit format** — Conventional Commits, footer `Refs: R#, E#` (e.g. `Refs: R3, E2.4`). Branch `feat/<issue>-<slug>`. Single approver `@joao-pedro-gms` (`.github/CODEOWNERS`).
- **Signing** — release reads 4 env vars only (`BRAINOUT_KEYSTORE_PATH/PASSWORD/KEY_ALIAS/KEY_PASSWORD`); CI uses base64-encoded `BRAINOUT_KEYSTORE_BASE64`. Without them, release builds intentionally stay unsigned (does not break CI).

## Important Files

| File | Role |
|---|---|
| `settings.gradle.kts` | 8-module list; `FAIL_ON_PROJECT_REPOS`; google()+mavenCentral() only. |
| `build.gradle.kts` (root) | Plugin declarations; `gradle.projectsEvaluated` wire-up; Kover 60% em `:core:domain` + `:core:data` com filtros de classes geradas; `koverMergedHtmlReport`; **ktlint aplicado a todos os subprojetos** (ver *Static Analysis*). |
| `gradle/libs.versions.toml` | Version catalog — **single source of truth for dependencies**. No inline versions. |
| `gradle/wrapper/gradle-wrapper.properties` | Gradle 9.7.1. |
| `gradle.properties` | `-Xmx4g`, parallel + caching enabled, `useAndroidX=true`, `nonTransitiveRClass=true`. |
| `app/build.gradle.kts` | Flavors, `BuildConfig.BASE_URL`, `coreLibraryDesugaring`, signing from env vars, lint gate. |
| `core/data/build.gradle.kts` | Flavors + `BuildConfig.HOLIDAYS_BASE_URL` (BrasilAPI), Room `schemaLocation`, `testInstrumentationRunner`, Kover, `lint { abortOnError = false }`. |
| `core/domain/build.gradle.kts` | Pure Kotlin JVM; Jacoco + Kover; no Android. |
| `core/ui/build.gradle.kts` | Lib Android sem flavor e **sem** `missingDimensionStrategy`; `isIncludeAndroidResources = true` (necessário para o teste Robolectric do `ColorScheme`). |
| `config/detekt/detekt.yml` | Single detekt config. `build.maxIssues=0`. Ignores `@Composable`/`@Preview`/`@HiltAndroidApp`/`@AndroidEntryPoint` for `FunctionNaming`. |
| `backend-stub/server.py` | FastAPI stub — contract is authoritative (idempotent upsert, client UUIDs). |
| `backend-stub/tests/test_contract.py` | Suíte pytest do contrato (`/v1/*`). |
| `.github/workflows/ci.yml` | 3 jobs: `static-analysis` → `unit-tests` (incl. Kover 60%) → `backend-integration` (docker stub + pytest + JVM sync). Roda `ktlintCheck`, `detekt`, `testDevDebugUnitTest`, `:core:ui:testDebugUnitTest`, `koverVerify`, `lintDevDebug`. |
| `.github/workflows/release-apk.yml` | Tag-triggered signed `.aab` build (4 secrets). |
| `.github/CODEOWNERS` | Single approver. |
| `.github/PULL_REQUEST_TEMPLATE.md` | Requires `Refs: R#` and forbids commented code. |
| `Documentos/` | Read-only PDFs (deliverable). Never edit. |

## Static Analysis

- **ktlint é aplicado a todos os subprojetos.** Antes o plugin vivia só na raiz, o que fazia `ktlintCheck` inspecionar 2 arquivos de build e **zero** dos 221 `.kt` versionados; hoje o root faz `subprojects { apply(plugin = ktlint); android = true }`. Consequência: a engine 1.x do ktlint 14 sinaliza violações cosméticas pré-existentes (registradas como DEF-10 em `docs/DEFEITOS.md`), então `ignoreFailures` está em `true` até a passada de reformatação. Não comente isso como "ktlint está desligado" — o gate **inspeciona** tudo e **não bloqueia** por enquanto.
- **detekt** é aplicado por módulo (no `build.gradle.kts` de cada um) com config centralizada em `config/detekt/detekt.yml`. Roda com `build.maxIssues = 0`.
- **`.editorconfig` na raiz** — `ktlint_code_style = ktlint_official` (o estilo que o código segue de fato; `android_studio` foi testado e rejeitado por pedir reformatação massiva) e `ktlint_function_naming_ignore_when_annotated_with = Composable`, que espelha o `FunctionNaming.ignoreAnnotated` do detekt (`config/detekt/detekt.yml:82-88`) para as funções `@Composable` em PascalCase. É aditivo: não substitui detekt nem o catálogo de regras.
- **Android lint** — gate rígido de i18n em `:app`, `:feature:auth`, `:feature:projects`, `:feature:settings`; relaxado em `:feature:tasks` e `:core:data` (ver *i18n gate*).

## Runtime / Tooling Preferences

- **Android SDK**: `compileSdk 37`, `targetSdk 37` (só em `:app`), `minSdk 24` em todos os módulos Android. **`coreLibraryDesugaring` está ligado só em `:app`** (`desugar_jdk_libs 2.1.5`), o que é verdade — mas o estado real é mais nuançado: `:core:data` usa `java.time.Instant`/`LocalDate` (`InstantConverter`, entidades, `HolidayRemoteDataSource`, `TaskRepositoryImpl`) e `java.util.Base64` (`PasswordHasherImpl`) com `minSdk 24` **sem** desugaring próprio, e por isso carrega 8 erros `[NewApi]` pendentes com `lint { abortOnError = false }`. O desugaring de `:core:data` está fora de escopo (comentado no `build.gradle.kts` do módulo) e é dívida conhecida — não trate `abortOnError = false` como configuração sadia. O platform instalado é `android-37.0` (Android 17, final — não é preview). `buildToolsVersion` não é declarado de propósito; o AGP 9.4.1 escolhe.
- **JDK**: bytecode target Java 17. CI (`ci.yml`) e release (`release-apk.yml`) provisionam **Temurin 21** (Gradle 9.7.1 roda em JVM 17–27 e o piso do AGP 9.4.1 é 17 — 21 é o padrão unificado do projeto). Localmente: `export JAVA_HOME=/opt/android-studio/jbr` (JBR 25 do Android Studio) antes de `./gradlew`. `local.properties` precisa de `sdk.dir`.
- **No package manager other than Gradle** for the Android side. **Python 3.12 + pip + venv** for the backend stub (also shipped as Docker image `python:3.12-slim` in `backend-stub/Dockerfile`).
- **Android Studio / lint / ktlint / detekt** are the only static analysis tools. No Checkstyle, no Spotless, no Sonar.
- **Kover 0.9.9** only on `:core:domain` and `:core:data`; everywhere else is exempt.

## Delivery Workflow (commit / push / PR / merge)

Standing rule — applies automatically at the end of every development task, without waiting to be asked.

**Trigger.** After changing code or docs, once the relevant checks pass. Purely investigative work (reading, analysis, no edits) never produces a commit.

**Steps, in order:**

1. Create a branch off `main`: `feat/<slug>`, `fix/<slug>`, `docs/<slug>`, `chore/<slug>` — matching the Conventional Commit type of the work.
2. Stage only files this task changed. `git add <paths>` explicitly; never `git add -A` / `git add .`.
3. Commit with Conventional Commits + `Refs:` footer:
   ```
   <type>(<scope>): <subject>

   <body if needed>

   Refs: R#, E#
   ```
4. `git push -u origin <branch>`.
5. Open the PR with `gh pr create` (fill the body per `.github/PULL_REQUEST_TEMPLATE.md`).
6. Merge with `gh pr merge --merge` — **merge commit, not squash**, to match the repo's existing history (PRs #65–#69 all landed as merge commits). Delete the remote branch after merge.
7. Return to `main` and `git pull`.

**Hard gates — never violate:**

- **Never commit or push when checks fail.** If `ktlintCheck`, `detekt`, `testDevDebugUnitTest`, or `koverVerify` fail, stop, report exactly what broke, and leave the work uncommitted. Do not commit "so we can fix it later" and do not push a red branch.
- **Never stage gitignored files or secrets** — `local.properties`, `keystore.properties`, `*.jks`, `.env`, any credential. `git status --short` must be inspected before staging.
- **Never commit changes under `Documentos/`, `.omo/`, or `docs/ATAS/`** (read-only by repo rule).
- **Never commit changes to files this task did not author.** If the working tree already had unrelated modifications before the task started, leave them unstaged and say so.
- **Never merge a PR whose CI is red.** If CI is still running, either wait for it or report the pending state — a pending check is not a green check.

**Local check limitation.** `detekt` cannot run locally with the Android Studio JBR (25): detekt 1.23.7 derives `--jvm-target` from the Gradle JVM and rejects `25`. It runs fine in CI on Temurin 21, so a *local* detekt failure is not a red gate — run `ktlintCheck`, `testDevDebugUnitTest`, and `koverVerify`, which do work locally. Do not add per-module detekt workarounds to silence this.

## Testing & QA

Frameworks (all from `gradle/libs.versions.toml`):

- **JUnit 4** (4.13.2) — no JUnit 5. `androidx.test.ext:junit` 1.2.1 for Android.
- **Google Truth** (1.4.4) — primary assertion lib.
- **MockK** (1.13.13) — `coEvery` / `coVerify` / `mockk`. `mockk-android` only in `:feature:auth/androidTest`.
- **Robolectric** (4.15.1) — Room/Hilt/WorkManager/tema JVM tests: `@RunWith(RobolectricTestRunner::class)`. Muitos usam `@Config(sdk = [34], manifest = Config.NONE)`; as telas Compose usam `@Config(qualifiers = "pt-rBR")` para resolver `values/`. **`:core:ui` precisa de `isIncludeAndroidResources = true`** para o teste de `ColorScheme`/tokens Neo.
- **Turbine** (1.2.0) — every ViewModel test uses `app.cash.turbine.test`.
- **kotlinx-coroutines-test** (1.9.0) — `runTest { ... }`.
- **Compose UI Test** (JUnit4 runner, BOM-pinned) — `createComposeRule`, `onNodeWithTag`, `assertIsDisplayed`.
- **MockWebServer** (4.12.0) — Retrofit contract tests + `:app/.../sync/SyncWorkerTest`.

How to run:

```bash
./gradlew testDevDebugUnitTest                   # modules com flavor (:app, :core:data)
./gradlew testDebugUnitTest                      # :feature:* e :core:ui (sem flavor)
./gradlew :core:domain:test                      # pure JVM, fast
./gradlew :core:domain:koverVerify :core:data:koverVerify   # 60% gate
./gradlew koverMergedHtmlReport                  # aggregated HTML
./gradlew connectedDevDebugAndroidTest           # :core:data instrumented (migrations + DAO) — emulador
./gradlew connectedDebugAndroidTest              # :feature:auth instrumented
```

Suite instrumentada de `:core:data` (`src/androidTest/`): **`MigrationTest`** cobre as 3 migrations — `MIGRATION_1_2`, `MIGRATION_2_3`, `MIGRATION_3_4`, cada uma individualmente e a cadeia 1→4 — e **`UserDaoInstrumentedTest`** faz CRUD real. As duas compilam (`:core:data:assembleDevDebugAndroidTest`); execução exige emulador.

Coverage expectations:
- **Kover 60% line bound** enforced on `:core:domain` and `:core:data` only.
- Class exclusions centralized in root `build.gradle.kts` (`*_Impl`, `Hilt_*`, `*.di.*`, `*.remote.*`, `*.PackageMarker`).
- `gradle.projectsEvaluated` in root makes every `test*UnitTest` Android task depend on `:core:domain:test` — do not remove; without it, CI won't run domain tests when invoked via an Android module.
- New `:core:domain/usecase/` requires a test in `:core:domain/src/test/.../usecase/` (`XxxUseCaseTest`).
- New repository impl in `:core:data` follows the `Fake*Dao` pattern (`FakeProjectDao`, `FakePendingOpDao`, `FakeTagDao` private classes inside the test) — no MockK for DAOs.
- Test naming: backtick-quoted Portuguese descriptive sentences (e.g. `fun \`atualizar tarefa valida erro de titulo vazio\``).

Correções recentes que valem como contrato de teste (não reverta sem atualizar o teste):
- `PasswordHasherImpl.verify` **lê as iterações do próprio hash armazenado** (`alg$iter$salt$hash`) em vez de usar uma constante fixa, e valida a faixa em `pbkdf2`. Hash gravado com iterações diferentes verifica corretamente.
- `SortOrder.toStorageKey()` emite a grafia `snake_case` (`name_asc`, `name_desc`, `created_desc`, `created_asc`) — exatamente a que a ordenação dinâmica de `ProjectDao.searchProjects` compara em SQL. `fromStorageKey` ainda aceita a grafia concatenada legada (`nameasc`) para preferências já gravadas.
- Hierarquia única de exceções: `BusinessRuleException : DomainException`, então `catch (DomainException)` captura violações de RN. Contrato fixado por `DomainExceptionHierarchyTest`.

Anti-patterns (do NOT):
- Do not add Android/Room/Hilt dependencies to `:core:domain`.
- Do not commit `local.properties`, `keystore.properties`, `*.jks`, or any keystore file (all gitignored). CI uses `BRAINOUT_KEYSTORE_BASE64`.
- Do not write `fallbackToDestructiveMigration`; write a hand-rolled `Migration` and add it to `DataModule` (existing: `MIGRATION_1_2`, `MIGRATION_2_3`, `MIGRATION_3_4`).
- Do not hard-code URLs anywhere — always `BuildConfig.BASE_URL`.
- Do not leave commented-out code.
- Do not suppress `MissingTranslation`; add the `values-en/` translation.
- Do not add a new `withContext(Dispatchers.IO)` to `:core:data` — the Room DAO suspend already runs off the main thread.
- Do not introduce a third route source of truth; extend `BrainOutRoutes` / the feature's `*Routes`.
- Do not edit `Documentos/`, `.omo/`, `docs/ATAS/`.
- Do not run `assembleRelease` locally expecting signed output without the 4 env vars set.

## Known gaps in this guide

Honest inventory so an agent does not assume the docs are complete:

- **`:core:ui` não tem `AGENTS.md`.** Só `:core:domain`, `:core:data` e `feature/` têm sub-guia; `:app` também não tem. As regras de `:core:ui` (tokens Neo, fontes, drawables, `isIncludeAndroidResources`) vivem apenas na seção *Key Directories* acima.
- **`:app` não tem `AGENTS.md`.** Suas regras (workers de sync/notificação, NavHost, flavors, lint rígido) estão só no root.
- **Rotas duplicadas** (`AuthRoutes`/`ProjectsRoutes`/`SettingsRoutes` vs `BrainOutRoutes`) — documentadas acima, não unificadas.
- **`PackageMarker.kt` ausente** em `:app` e `:core:ui`.
- **ktlint com `ignoreFailures = true`** até a reformatação DEF-10.