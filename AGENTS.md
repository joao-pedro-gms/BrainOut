# BrainOut — Agent Guide

## Project

Personal project/task manager. Android, Kotlin + Compose + Material 3, offline-first (Room + WorkManager sync queue), FastAPI backend stub.
Single developer: João Pedro G M Silva, PUC Goiás ADS 2026/2 Projeto Integrador (matrícula `20251012000740`). Requirements are tracked as `R1`–`R14` and cycles as `E1.x`–`E5.x` — commits reference them in the `Refs:` footer.

## Modules

```
:app → :feature:{auth,projects,tasks,settings} → :core:ui, :core:data → :core:domain (pure JVM leaf)
```

- **`:app`** — `BrainOutApplication` (`@HiltAndroidApp` + `Configuration.Provider`), `MainActivity` (`@AndroidEntryPoint`, `POST_NOTIFICATIONS` on Tiramisu+), `navigation/BrainOutNavHost.kt`, `sync/` (`SyncWorker`, `SyncDispatcher`, `SyncConnectivityWatcher`, `SyncScheduler`), `notifications/` (`WorkManagerDeadlineScheduler`, `DeadlineWorker`, `CompleteTaskWorker`).
- **`:core:domain`** — pure `kotlin-jvm`. No Android/Hilt/Room/Compose; deps are only `kotlinx-coroutines-core` + `javax.inject`. `error/`, `model/`, `repository/` (ports), `notification/`, `usecase/`.
- **`:core:data`** — flavors `dev`/`prod` in dimension `environment`; `BuildConfig.BASE_URL`, `BuildConfig.HOLIDAYS_BASE_URL` (BrasilAPI). Room `BrainOutDatabase` v4, Retrofit, DataStore, kotlinx-serialization, `di/DataModule.kt` (all wiring), `security/`, `session/`, `sync/`, `util/DataLogger.kt`.
- **`:core:ui`** — Compose only. `theme/` (Color/Type/Shape/Theme + `resolveBrainOutStaticColorScheme(darkTheme)` pure function kept testable) and redesign tokens `NeoColor.kt` / `NeoTokens.kt` / `NeoTypography.kt`. Assets in `res/font` + `res/drawable` (`neo_ic_*`, `neo_art_*`); `core/ui/src/test` asserts shipped asset bytes/sha256 against `docs/design/ASSETS.md` under a 1 MB ceiling.
- **`:feature:*`** — owns `navigation/*Routes.kt`, `ui/` screens, colocated `*TestTags`, ViewModels. `:feature:auth` keeps VMs in `viewmodel/`; other features colocate them in `ui/<screen>/`. **Colocate in new features — do not migrate `:feature:auth`.**

**Read the per-module reference instead of guessing:** `core/domain/AGENTS.md`, `core/data/AGENTS.md`, `feature/AGENTS.md`, `backend-stub/AGENTS.md`.

`PackageMarker.kt` exists only in `:core:data`, `:core:domain` and the four `:feature:*` — **not** `:app` or `:core:ui`. Root Kover exclusions reference `*.PackageMarker`; don't remove them from the covered modules.

## Build & test

**`:feature:*` and `:core:ui` have NO product flavors** (the four `:feature:*` call `missingDimensionStrategy("environment", "dev")`; `:core:ui` declares neither flavors nor that strategy), so their unit-test task is `testDebugUnitTest`, **not** `testDevDebugUnitTest`. Only `:app` and `:core:data` are flavor-aware. `./gradlew testDevDebugUnitTest` silently skips the flavorless modules — that is why CI invokes them explicitly.

```bash
# Build
./gradlew :app:assembleDevDebug            # :app:assembleProdDebug, :app:assembleRelease also exist

# Tests — CI's exact invocation list
./gradlew testDevDebugUnitTest                                            # :app + :core:data (+ :core:domain via root wire-up)
./gradlew :feature:projects:testDebugUnitTest :feature:tasks:testDebugUnitTest \
          :feature:auth:testDebugUnitTest :feature:settings:testDebugUnitTest
./gradlew :core:ui:testDebugUnitTest
./gradlew :core:domain:test                                                # pure JVM, fast
./gradlew :core:data:testDevDebugUnitTest                                  # Robolectric + MockWebServer

# Coverage (60% bound, :core:domain + :core:data only)
./gradlew :core:domain:koverVerify :core:data:koverVerify
./gradlew koverMergedHtmlReport      # root aggregator (there is no root koverHtmlReport)

# Static analysis
./gradlew ktlintCheck
./gradlew lintDevDebug                 # variant-qualified, so flavored modules participate

# Backend stub (Python 3.12)
cd backend-stub && python -m venv .venv && source .venv/bin/activate \
  && pip install -r requirements.txt \
  && python -m uvicorn server:app --host 0.0.0.0 --port 8000
```

**Instrumented tests are NOT in CI.** `connectedDevDebugAndroidTest` needs an emulator; the backend-integration job replaced it with JVM suites. Only two `androidTest` sources exist: `core/data` (`UserDaoInstrumentedTest`, `MigrationTest`) and `feature/auth` (`LoginScreenTest`). `README.md`'s claim that CI runs `connectedDebugAndroidTest` is stale — don't repeat it.

## Environment & toolchain gotchas

- **Bytecode Java 17**; `kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }` — `kotlinOptions` is gone in AGP 9.
- **Build JDK 21** (Temurin, per `ci.yml` / `release-apk.yml`). `org.gradle.java.installations.auto-download=false` — no toolchain auto-provisioning. Locally `export JAVA_HOME=/opt/android-studio/jbr`.
- **detekt cannot run under Android Studio's JBR 25**: detekt 1.23.7 derives `--jvm-target` from the Gradle JVM and rejects 25. A local detekt failure is **not** a red gate — run `ktlintCheck`, the test suites and `koverVerify` locally and let CI adjudicate. Don't add per-module workarounds.
- **Do not create `gradle/gradle-daemon-jvm.properties`.** An untracked copy demanding JDK 25 killed every local build (with `auto-download=false` it can never resolve); it was removed and no `.bak` survives. See `docs/design/BASELINE.md`.
- `compileSdk 37`, `targetSdk 37`, `minSdk 24`; installed platform `android-37.0` (final, not preview). `buildToolsVersion` intentionally not declared. `README.md`'s compileSdk 35 is stale. `coreLibraryDesugaring` (`desugar_jdk_libs` 2.1.5) only in `:app`, for `Instant#toEpochMilli` on API 24/25.
- **`kotlin-android` was deliberately removed** from `gradle/libs.versions.toml` — AGP 9 activates Kotlin automatically. Do not re-add. Remaining: `kotlin-jvm` (`:core:domain` only), `kotlin-compose`, `kotlin-serialization` (`:core:data`).
- Versions pinned in `gradle/libs.versions.toml` (single source of truth, no inline versions): KGP 2.3.20 (AGP 9.4.1 floor is 2.2.10), KSP 2.3.12, Hilt 2.60.1, Room 2.8.4, ktlint plugin 14.2.0, detekt 1.23.7, Kover 0.9.9, Compose BOM 2024.10.01, Gradle wrapper 9.7.1.
- **URLs**: dev `BASE_URL` precedence = env var `BASE_URL` (CI) > `local.properties` `brainout.baseUrl.dev` > default `http://10.0.2.2:8000/`. Emulator reaches the host at `10.0.2.2`, **never localhost**; a physical device needs the host's LAN IP and `uvicorn --host 0.0.0.0`. `prod` is the placeholder `https://TBD/`. Never hard-code a URL — use `BuildConfig.BASE_URL` / `HOLIDAYS_BASE_URL`.
- `.worktrees/` is gitignored and holds ~23 git worktrees on `wt/*` / `chore/brainout-bo-*` branches from a parallel workflow. Don't delete them or their branches casually; `git status` may surface them.

## Conventions

- Author header `// João Pedro G M Silva - PUC Goiás ADS - 20251012000740` on every `.kt` / `.kts` / `.xml`.
- Strings: snake_case with a screen prefix (`login_`, `home_`, `task_`, `project_detail_`, `bottom_tab_`, `common_`). `values/strings.xml` and `values-en/strings.xml` must stay key-for-key in sync.
- **Lint gates**: `abortOnError = true` + `error += "MissingTranslation"` in `:app`, `:feature:auth`, `:feature:projects`, `:feature:settings`. `abortOnError = false` in `:core:data` (NewApi backlog) and `:feature:tasks` (`Instant.parse` in an `@Preview`, minSdk 24). `:core:ui` has no lint block. **Never suppress `MissingTranslation`** — add the translation.
- **Logging**: tags are prefixed `BrainOut:` (`BrainOut:App`, `BrainOut:Sync`, …). `core/data/util/DataLogger.kt` wraps `android.util.Log` in `runCatching` (`logDebug`/`logWarn`/`logError`) because non-Robolectric JVM tests throw `Method d in android.util.Log not mocked`, and provides `String.maskEmail()` (`jo***@domain`) — **PII must never reach logcat in clear text**. Any new module that logs must route through `DataLogger` or set `isReturnDefaultValues = true` in `testOptions` (`:feature:auth`, `:feature:projects`, `:feature:tasks` do; without it 16 JVM tests fail).
- **Data flow**: Screen → `@HiltViewModel` + `SavedStateHandle` → use case (`:core:domain`) → repository (`:core:data`) → DAO (`Flow`) **plus** a `PendingOpEntity` enqueued in the *same* Room transaction (`pendingOpDao.enqueueInTx(op) { ... }`). Mutations always go through that dual-write.
- ViewModels expose an immutable `data class XxxUiState(...)` via `stateIn(viewModelScope, WhileSubscribed(5_000), initial)`. Retry idiom: `_retryToken: MutableStateFlow<Int>` + `flatMapLatest`. One-shot errors land in a nullable `errorMessage` slot, never a sealed class.
- Domain errors live in `core.domain.error` (`DomainException` hierarchy). Repositories throw; ViewModels catch.
- `SyncWorker` drains `pending_ops` in batches of 50; 4xx discards (`Result.success`), 5xx/`IOException` retries (`Result.retry`). Conflicts resolve last-writer-wins on `updated_at`.
- Room migrations are hand-rolled in `local/Migrations.kt` (`MIGRATION_1_2`, `MIGRATION_2_3`, `MIGRATION_3_4`), registered in `DataModule`. On bump: update `@Database(version = …)` **and** commit the exported schema JSON in `core/data/schemas/`. **Never** `fallbackToDestructiveMigration`. The redesign plan anticipates `MIGRATION_4_5` — do not pre-create it.
- Navigation: never hard-code route strings — `object XxxRoutes` constants + builder functions, feature `NavGraphBuilder.<name>Graph()` extensions registered by `:app`. **The bottom bar is not in `:app`** — it's `feature/projects/.../ui/home/HomeBottomBar.kt` (`HomeTab`, current tab in `rememberSaveable`). Sign-out: `activeUserProvider.signOut()` then `navigate(login) { popUpTo(0) { inclusive = true } }`.
- DTOs are snake_case via `@SerialName`; domain mapping happens in `RemoteDataSource`, never in a DAO or entity. `Instant` ⇄ TEXT via `local/converter/InstantConverter.kt`. Client-supplied UUIDs everywhere (R6) so backend upserts are idempotent.
- **No commented-out code** (R12; the PR template enforces it). Delete dead code.

## Testing

- Stack: **JUnit 4 only** (no JUnit 5), Google Truth, MockK, Turbine, kotlinx-coroutines-test, MockWebServer, Compose UI Test.
- Robolectric 4.15.1: `@RunWith(RobolectricTestRunner::class) @Config(sdk = [34], manifest = Config.NONE)` is the dominant form; add `qualifiers = "pt-rBR"` when the test resolves `values/` strings.
- Compose UI tests inject ViewModels through the constructor with mocked use cases — **no `HiltAndroidRule`**. Locate nodes via `onNodeWithTag` and the colocated `*TestTags` objects.
- DAOs use hand-written `Fake*Dao` fakes (no MockK); Retrofit/worker tests use MockWebServer.
- Test names are backtick-quoted Portuguese sentences.
- **New `:core:domain/usecase/` requires a matching test** in `core/domain/src/test/.../usecase/`.
- Kover 60% line bound on `:core:domain` + `:core:data` **only**. Exclusions live centrally in the root `build.gradle.kts` (`*_Impl`, `Hilt_*`, `*.di.*`, `*.remote.*`, `*.BuildConfig`, `*.PackageMarker`).
- **Do not remove** the root `gradle.projectsEvaluated` block that makes every `test*UnitTest` depend on `:core:domain:test` — that wire-up is why `testDevDebugUnitTest` still exercises the pure-JVM module.
- Known gaps (recorded in the sub-AGENTS files): `core/domain` use cases `CreateTag`/`UpdateProject`/`DeleteProject`/`DeleteTag` untested; `:core:data` Task/Project/Tag repository impls have no direct tests; `:feature:settings` has no ViewModel and only a `RoutesTest`.

## Redesign programme

- `DESIGN.md` (root) is the spec; `docs/design/` holds `BASELINE.md`, `MATRIZ_MIGRACAO.md`, `ESPECIFICACAO.md`, `PESQUISA.md`, `PERFORMANCE.md`, `ASSETS.md`, `tokens.json`.
- Plan: `docs/plans/2026-10-01-redesign-neobrutalista.md`, tasks `NB-01`..`NB-35` across phases `F0`..`F8`. Its header instructs agents to use the `executing-plans` skill.
- **Where `DESIGN.md` conflicts with the code, the code wins** (stated in `docs/design/MATRIZ_MIGRACAO.md`).
- Verifiers: `python docs/design/verify_tokens.py` (token references, light/dark parity, contrast) and `python docs/design/verify_roadmap.py` (plan links, task IDs, ordering, dependency acyclicity). Run both after touching tokens or the plan.

## CI & delivery

`.github/workflows/ci.yml`, three jobs:

1. **`static-analysis`** — `ktlintCheck` then `detekt` (15 min).
2. **`unit-tests`** (needs static-analysis) — tests + `koverVerify` + `koverHtmlReport` + `lintDevDebug` (30 min).
3. **`backend-integration`** (needs static-analysis) — docker-builds `backend-stub`, health-checks `:8000`, curl PUT/DELETE/tags smoke, `pytest`, live E2E via `tests/smoke_e2e.py`, then `:core:data:testDevDebugUnitTest :app:testDevDebugUnitTest` with `BASE_URL` pointed at the container (25 min).

Also `codeql.yml` (push to main, PRs, weekly) and `pages.yml` (publishes `docs/wireframes/` to GitHub Pages). `.github/CODEOWNERS` has a single approver.

**Standing delivery rule** — applies automatically after any code/doc change with green checks:

1. Branch off `main`: `feat|fix|docs|chore/<slug>`.
2. `git add <explicit paths>` — **never** `git add -A` / `git add .`.
3. Conventional Commits + `Refs: R#, E#` footer.
4. `git push -u origin <branch>`.
5. `gh pr create` — body follows `.github/PULL_REQUEST_TEMPLATE.md` (must reference an `R#`; forbids commented-out code).
6. `gh pr merge --merge` — **merge commit, not squash** (the actual history is `Merge pull request #…`; `docs/CONTRIBUTING.md` and `docs/CI-CD.md` say otherwise — trust the history). Delete the remote branch.
7. Return to `main` and `git pull`.

**Hard gates:**

- **Never commit or push red.** If `ktlintCheck`, `detekt`, the test suites or `koverVerify` fail, stop, report exactly what broke, and leave the work uncommitted.
- **Never stage secrets or gitignored files**: `local.properties`, `keystore.properties`, `*.jks`, `*.p12`, `*.keystore`, `*.env`.
- **Never commit changes under `Documentos/`, `docs/ATAS/`, `.omo/`, or `CLAUDE.md`** — read-only by repo rule.
- **Never commit files this task did not author.** If the tree already had unrelated modifications, leave them unstaged and say so.
- **Never merge with CI red or pending** — a pending check is not a green check.

**Release signing** reads exactly 4 env vars: `BRAINOUT_KEYSTORE_PATH` (absolute — `file()` resolves relative to `app/`), `BRAINOUT_KEYSTORE_PASSWORD`, `BRAINOUT_KEY_ALIAS`, `BRAINOUT_KEY_PASSWORD`. Absent → release builds are intentionally unsigned and CI still passes. `release-apk.yml` is tag-triggered on `v*` and additionally needs the `BRAINOUT_KEYSTORE_BASE64` secret. PKCS12 keystores need the same password for `-storepass` and `-keypass`.

**Backend stub** (`backend-stub/server.py` is the contract source of truth; `tests/test_contract.py` + `README.md` mirror it): FastAPI, `python:3.12-slim` image, in-memory dicts (restart wipes data by design), no auth, client-supplied UUIDs. Never point the `prod` flavor at it. A contract change must update `BrainOutApi`, `RemoteDtos`, `backend-stub/README.md` and `test_contract.py` together.