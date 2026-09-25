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
- **`:core:data`** — Android library with **flavors** `environment`/`dev`+`prod`; `BaseUrl` from `BuildConfig.BASE_URL`. Room (`BrainOutDatabase` v4, exported schemas), Retrofit (`BrainOutApi`), DataStore Preferences, kotlinx-serialization, Hilt. DI wiring lives in `DataModule` (`@InstallIn(SingletonComponent::class)`).
- **`:core:ui`** — Compose-only module (Material 3 theme, colors, shapes, typography). No Hilt, no Room, no DataStore.
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
- Long-running / network IO hops to `Dispatchers.IO` (`MainActivity` start-destination resolution; repository functions).

Navigation:
- `BrainOutRoutes` (root) + per-feature `*Routes` object with snake_case route constants.
- `NavGraphBuilder.tasksGraph()` / `dashboardGraph()` extensions (in `:feature:tasks`) are registered into the root `NavHost`.
- Sign-out: `activeUserProvider.signOut()` then `navigate(login) { popUpTo(0) { inclusive = true } }`.

Sync (offline-first):
- Mutations call `pendingOpDao.enqueueInTx(op) { ... }` in one Room transaction (see `TaskRepositoryImpl`).
- `SyncWorker` (`@HiltWorker` + `CoroutineWorker`) drains `pending_ops` with batch size 50; 4xx → discard (`Result.success`), 5xx/IOException → retry (`Result.retry`).
- `SyncDispatcher` + `SyncConnectivityWatcher` (both in `:core:data`) schedule the periodic work; `BrainOutApplication.onCreate` calls `syncScheduler.ensurePeriodicSync()`.

## Key Directories

| Dir | Purpose |
|---|---|
| `app/src/main/kotlin/.../BrainOutApplication.kt` | Hilt bootstrap + WorkManager Configuration provider. |
| `app/src/main/kotlin/.../MainActivity.kt` | Compose host, start-destination resolution, `POST_NOTIFICATIONS` request (Tiramisu+). |
| `app/src/main/kotlin/.../navigation/BrainOutNavHost.kt` | Root NavHost; calls feature `*Graph()` extensions. |
| `app/src/main/kotlin/.../sync/` | `SyncWorker`, `SyncDispatcher`, `SyncConnectivityWatcher`, `SyncScheduler`. |
| `app/src/main/kotlin/.../notifications/` | `WorkManagerDeadlineScheduler` (impl of domain port), `DeadlineWorker`. |
| `core/domain/src/main/kotlin/.../usecase/` | Use cases. Kover 60% gate — every new use case needs a test under `src/test/`. |
| `core/domain/src/main/kotlin/.../notification/DeadlineNotificationScheduler.kt` | Port interface; Android-free. |
| `core/data/src/main/kotlin/.../local/BrainOutDatabase.kt` | Room DB v4; `pending_ops` table is the sync queue. Schemas in `core/data/schemas/`. |
| `core/data/src/main/kotlin/.../remote/BrainOutApi.kt` | Retrofit interface; all `suspend`. |
| `core/data/src/main/kotlin/.../di/DataModule.kt` | All `:core:data` DI wiring. Register new modules here. |
| `core/data/src/main/kotlin/.../repository/` | Repository impls; each delegates mutations to `pendingOpDao.enqueueInTx`. |
| `core/ui/` | `Color`, `Shape`, `Theme`, `Type`. |
| `feature/<x>/src/main/kotlin/.../ui/` | Screens + `*TestTags` objects for Compose UI tests. |
| `feature/<x>/src/main/kotlin/.../viewmodel/` or `ui/<x>/` | `@HiltViewModel` + `data class XxxUiState`. |
| `feature/<x>/src/main/kotlin/.../navigation/*Routes.kt` | Route constants + `NavGraphBuilder` extensions. |
| `backend-stub/server.py` | FastAPI stub — source of truth for `/v1/*` contract. |
| `docs/ROADMAP.md`, `docs/ARQUITETURA.md`, `docs/CI-CD.md`, `docs/CONTRIBUTING.md` | Requirements R1–R14, E1.x–E5.x cycles, pipeline, conventional commits. |
| `Documentos/` | Deliverable PDFs only — never edit. |

## Development Commands

All Gradle commands from repo root. With only JDK 21 installed, export `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64` (or use Temurin 17).

```bash
# Build
./gradlew :app:assembleDevDebug              # daily dev build
./gradlew :app:assembleProdDebug             # prod build (BASE_URL placeholder https://TBD/)
./gradlew :app:assembleRelease               # unsigned unless BRAINOUT_* secrets are set

# Unit tests (includes :core:domain via gradle.projectsEvaluated wire-up)
./gradlew testDevDebugUnitTest               # what CI actually runs
./gradlew :core:domain:test                  # pure JVM tests
./gradlew :core:data:testDevDebugUnitTest    # Robolectric + MockWebServer
./gradlew :feature:auth:testDevDebugUnitTest # ViewModel tests
./gradlew :app:testDevDebugUnitTest          # smoke + WorkManager + sync tests

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

Override dev URL via `local.properties` (key `brainout.baseUrl.dev`). Emulator reaches host at `http://10.0.2.2:8000/`.

## Code Conventions & Common Patterns

- **Author header** — every `.kt`/`.kts`/`.xml` file starts with `// João Pedro G M Silva - PUC Goiás ADS - 20251012000740`.
- **Packages** lowercase only; strings `snake_case` with screen/feature prefix (`login_`, `home_`, `task_`, `project_detail_`, `bottom_tab_`, `common_`). Detekt `PackageNaming` enforces.
- **i18n gate** — `values/strings.xml` and `values-en/strings.xml` must stay in sync. `:app`, `:feature:auth`, `:feature:projects`, `:feature:settings` enforce `MissingTranslation = error` + `lint { abortOnError = true }`. `:feature:tasks` and `:core:data` are relaxed (pre-existing `NewApi` issues). Never suppress; translate instead.
- **AGP 9 built-in Kotlin** — `kotlin-android` plugin is **removed** from the version catalog. Do not re-add. Remaining Kotlin plugins: `kotlin-jvm` (only `:core:domain`), `kotlin-compose`, `kotlin-serialization` (`:core:data`).
- **Bytecode target** — Java 17 (JDK 21 acceptable locally via `JAVA_HOME`). Use `kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }` — `kotlinOptions` is gone in AGP 9.
- **Flavors** — `dev` (BASE_URL `http://10.0.2.2:8000/`, overridable via `local.properties`) and `prod` (placeholder `https://TBD/`, decision E3.1). Only `:app` and `:core:data` declare flavors; every `:feature:*` fixes `missingDimensionStrategy("environment", "dev")`.
- **DI** — Hilt everywhere; ViewModels via `@HiltViewModel` + `@Inject` constructor. Domain layer may use `javax.inject.Inject` only. New `:core:data` bindings go in `DataModule`.
- **State management** — immutable `data class XxxUiState(...)` (per-screen error slots as nullable fields, no sealed class); expose `val uiState: StateFlow<XxxUiState>` via `stateIn(viewModelScope, WhileSubscribed(5_000), initial)`. One-shot events via `Channel`/`SharedFlow` or `MutableStateFlow<String>` for errorMessage + retry pattern with `_retryToken` + `flatMapLatest`.
- **Module marker** — every module has `PackageMarker.kt` exposing `internal const val <MODULE>_PACKAGE: String`. Kover exclusions list relies on these.
- **Repository mutations** — always go through `pendingOpDao.enqueueInTx(op) { ... }` inside one Room transaction (offline-first dual-write, E3.3). Cascade ops use `@Transaction` DAO methods (`cascadeCompleteTask`, `cascadeReopenTask`).
- **Navigation** — never hardcode route strings; use `object XxxRoutes` constants + helper `fun xRoute(args)` builders. Graph extensions (`NavGraphBuilder.tasksGraph()`) are registered by `:app`.
- **Compose test tags** — colocated `object XxxTestTags` per screen (e.g. `project_detail_new_task_fab`); snake_case, screen-prefixed. Used by Robolectric + Compose UI tests via `onNodeWithTag`.
- **Domain errors** — exception types in `core.domain.error.*` (`TagNotFoundException`, etc.). Repositories throw domain exceptions; ViewModels catch and surface into `errorMessage`.
- **No commented-out code** (`CONTRIBUTING.md`, R12). Delete dead code; don't disable with comments.
- **Commit format** — Conventional Commits, footer `Refs: R#, E#` (e.g. `Refs: R3, E2.4`). Branch `feat/<issue>-<slug>`. Single approver `@joao-pedro-gms` (`.github/CODEOWNERS`).
- **Signing** — release reads 4 env vars only (`BRAINOUT_KEYSTORE_PATH/PASSWORD/KEY_ALIAS/KEY_PASSWORD`); CI uses base64-encoded `BRAINOUT_KEYSTORE_BASE64`. Without them, release builds intentionally stay unsigned (does not break CI).

## Important Files

| File | Role |
|---|---|
| `settings.gradle.kts` | 8-module list; `FAIL_ON_PROJECT_REPOS`; google()+mavenCentral() only. |
| `build.gradle.kts` (root) | Plugin declarations; `gradle.projectsEvaluated` wire-up; Kover 60% on `:core:domain` + `:core:data`; `koverMergedHtmlReport` task. |
| `gradle/libs.versions.toml` | Version catalog — **single source of truth for dependencies**. No inline versions. |
| `gradle/wrapper/gradle-wrapper.properties` | Gradle 9.7.1. |
| `gradle.properties` | `-Xmx4g`, parallel + caching enabled, `useAndroidX=true`, `nonTransitiveRClass=true`. |
| `app/build.gradle.kts` | Flavors, `BuildConfig.BASE_URL`, `coreLibraryDesugaring`, signing from env vars, lint gate. |
| `core/data/build.gradle.kts` | Flavors + `BuildConfig.HOLIDAYS_BASE_URL` (BrasilAPI), Room `schemaLocation`, Kover. |
| `core/domain/build.gradle.kts` | Pure Kotlin JVM; Jacoco + Kover; no Android. |
| `config/detekt/detekt.yml` | Single detekt config. `build.maxIssues=0`. Ignores `@Composable`/`@Preview`/`@HiltAndroidApp`/`@AndroidEntryPoint` for `FunctionNaming`. |
| `backend-stub/server.py` | FastAPI stub — contract is authoritative (idempotent upsert, client UUIDs). |
| `.github/workflows/ci.yml` | 3 jobs: `static-analysis` → `unit-tests` (incl. Kover 60%) → `backend-integration` (docker stub + pytest + JVM sync). |
| `.github/workflows/release-apk.yml` | Tag-triggered signed `.aab` build (4 secrets). |
| `.github/CODEOWNERS` | Single approver. |
| `.github/PULL_REQUEST_TEMPLATE.md` | Requires `Refs: R#` and forbids commented code. |
| `Documentos/` | Read-only PDFs (deliverable). Never edit. |

## Runtime / Tooling Preferences

- **Android SDK**: `compileSdk 35`, `targetSdk 35`, `minSdk 24`. `coreLibraryDesugaring` only in `:app` (uses `desugar_jdk_libs 2.1.5` for `java.time.Instant#toEpochMilli` on 24/25).
- **JDK**: target Java 17. CI uses Temurin 17 for the main jobs, Temurin 21 for the release workflow. Locally with only JDK 21: `export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64` before `./gradlew`.
- **No package manager other than Gradle** for the Android side. **Python 3.12 + pip + venv** for the backend stub (also shipped as Docker image `python:3.12-slim` in `backend-stub/Dockerfile`).
- **Android Studio / lint / ktlint / detekt** are the only static analysis tools. No Checkstyle, no Spotless, no Sonar.
- **Kover 0.9.9** only on `:core:domain` and `:core:data`; everywhere else is exempt.

## Testing & QA

Frameworks (all from `gradle/libs.versions.toml`):

- **JUnit 4** (4.13.2) — no JUnit 5. `androidx.test.ext:junit` 1.2.1 for Android.
- **Google Truth** (1.4.4) — primary assertion lib.
- **MockK** (1.13.13) — `coEvery` / `coVerify` / `mockk`. `mockk-android` only in `:feature:auth/androidTest`.
- **Robolectric** (4.15.1) — every Room/Hilt/WorkManager JVM test: `@RunWith(RobolectricTestRunner::class) @Config(sdk = [34], manifest = Config.NONE)`.
- **Turbine** (1.2.0) — every ViewModel test uses `app.cash.turbine.test`.
- **kotlinx-coroutines-test** (1.9.0) — `runTest { ... }`.
- **Compose UI Test** (JUnit4 runner, BOM-pinned) — `createComposeRule`, `onNodeWithTag`, `assertIsDisplayed`.
- **MockWebServer** (4.12.0) — Retrofit contract tests + `:app/.../sync/SyncWorkerTest`.

How to run:

```bash
./gradlew testDevDebugUnitTest                   # everything (CI target)
./gradlew :core:domain:test                      # pure JVM, fast
./gradlew :core:domain:koverVerify :core:data:koverVerify   # 60% gate
./gradlew koverMergedHtmlReport                  # aggregated HTML
./gradlew connectedDevDebugAndroidTest           # Room migration + DAO tests (emulator)
```

Coverage expectations:
- **Kover 60% line bound** enforced on `:core:domain` and `:core:data` only.
- Class exclusions centralized in root `build.gradle.kts` (`*_Impl`, `Hilt_*`, `*.di.*`, `*.remote.*`, `*.PackageMarker`).
- `gradle.projectsEvaluated` in root makes every `test*UnitTest` Android task depend on `:core:domain:test` — do not remove; without it, CI won't run domain tests when invoked via an Android module.
- New `:core:domain/usecase/` requires a test in `:core:domain/src/test/.../usecase/` (`XxxUseCaseTest`).
- New repository impl in `:core:data` follows the `Fake*Dao` pattern (`FakeProjectDao`, `FakePendingOpDao`, `FakeTagDao` private classes inside the test) — no MockK for DAOs.
- Test naming: backtick-quoted Portuguese descriptive sentences (e.g. `fun \`atualizar tarefa valida erro de titulo vazio\``).

Anti-patterns (do NOT):
- Do not add Android/Room/Hilt dependencies to `:core:domain`.
- Do not commit `local.properties`, `keystore.properties`, `*.jks`, or any keystore file (all gitignored). CI uses `BRAINOUT_KEYSTORE_BASE64`.
- Do not write `fallbackToDestructiveMigration`; write a hand-rolled `Migration` and add it to `DataModule` (existing: `MIGRATION_1_2`, `MIGRATION_2_3`, `MIGRATION_3_4`).
- Do not hard-code URLs anywhere — always `BuildConfig.BASE_URL`.
- Do not leave commented-out code.
- Do not suppress `MissingTranslation`; add the `values-en/` translation.
- Do not edit `Documentos/`, `.omo/`, `docs/ATAS/`.
- Do not run `assembleRelease` locally expecting signed output without the 4 env vars set.
