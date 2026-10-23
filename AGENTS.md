# Repository Guidelines

## Approved product direction — 2026-10-16

The owner approved a fully local Android product. Read
[docs/TRANSICAO-LOCAL.md](docs/TRANSICAO-LOCAL.md) for the current inventory,
transition backlog and review of open PRs. This direction takes precedence
over older remote-product plans below. Network/sync code was removed in #212; portable backup is still pending (#214).
Room v5 removes the legacy queue entirely; `LocalTransaction` is the single write path.
All Android modules are unflavored: use testDebugUnitTest, lintDebug,
:app:assembleDebug and :core:data:assembleDebugAndroidTest. The legacy flavor,
network and dual-write instructions below are historical and must not be
reintroduced. WorkManager remains for deadline notifications; LegacySyncCleanup
cancels only old sync jobs. Preserve local
data, authentication and deadline notifications during migration. Do not
add remote features or describe the target architecture as already shipped.

Use executable configuration as the version source: Kotlin 2.4.21, Compose
BOM 2026.09.00, Gradle 9.8.1 and detekt 1.23.8 are currently configured.
The subproject ktlint gate now uses `ignoreFailures = false` (DEF-10 fixed).
Older version/gate descriptions below are historical until reconciled.

Practical guide for AI assistants working on the **BrainOut** codebase. Verified against the current `main` tree (AGP 9.4.1, KGP 2.4.21, Gradle 9.8.1, JDK 17 bytecode, Compose BOM 2026.09.00).

## Project Overview

BrainOut is a **personal project/task manager** Android app — Kotlin + Jetpack Compose + Material 3, **fully local**: Room + DataStore for persistence, WorkManager for deadline notifications. No backend, no network layer, no sync queue. Built as the PUC Goiás ADS 2026/2 *Projeto Integrador* (author: João Pedro G M Silva, matrícula `20251012000740`). Single developer; Kover 60% coverage gate on `:core:domain` and `:core:data`.

## Architecture & Data Flow

Strict multi-module graph (no cycles). Dependencies only point downward:

```
:app  →  :feature:{auth,projects,tasks,settings}
              └─→  :core:ui
              └─→  :core:data  ──→  :core:domain  (pure JVM, leaf)
              └─→  :core:domain
```

- **`:app`** — orchestrator: `BrainOutApplication` (`@HiltAndroidApp`, `Configuration.Provider` for HiltWorkerFactory), `MainActivity` (`@AndroidEntryPoint` + Compose), `BrainOutNavHost`. Holds the WorkManager workers of deadline notification (`DeadlineWorker`, `CompleteTaskWorker`) and `WorkManagerDeadlineScheduler` (impl of the domain port).
- **`:core:domain`** — pure **Kotlin JVM** (no Android, no Hilt, no Compose). Only `kotlinx-coroutines-core` + `javax.inject` (`@Inject`). Holds repository interfaces, use cases (`CreateUserUseCase`, `ChangeTaskStatusUseCase`, …), domain models, and the `DeadlineNotificationScheduler` port.
- **`:core:data`** — Android library **sem flavors**. Room (`BrainOutDatabase` v5, exported schemas), DataStore Preferences, Hilt. **Sem Retrofit, sem HTTP, sem conectividade e sem fila de sync.** DI wiring lives mostly in `DataModule` (`@InstallIn(SingletonComponent::class)`), com uma exceção (ver *Conventions*).
- **`:core:ui`** — Compose-only module (Material 3 theme, tokens, cores, formas, tipografia). No Hilt, no Room, no DataStore. Sem flavors **e sem** `missingDimensionStrategy`.
- **`:feature:*`** — each owns its screens + ViewModels + test tags. All depend on `:core:domain` + `:core:data` + `:core:ui`.

**Data flow per request** (canonical example — see `ProjectDetailViewModel`):

```
Compose Screen
    → @HiltViewModel + SavedStateHandle
        → use case (:core:domain)
            → repository (:core:data)
                → DAO (Room) — emits via Flow
                → LocalTransaction.run { … } (uma transação Room por escrita)
    ← StateFlow<UiState> (stateIn with WhileSubscribed(5_000))
```

Async patterns:
- `Flow` from DAOs → `.map { rows -> rows.map { it.toDomain() } }` → ViewModel `.stateIn(viewModelScope, WhileSubscribed(5_000), initial)` exposed as immutable `data class XxxUiState(...)`.
- Retry pattern: `_retryToken: MutableStateFlow<Int>` + `flatMapLatest` discards prior Flow and re-subscribes after failures (see `ProjectDetailViewModel`, `DashboardViewModel`).
- **Dispatchers:** os repositórios de `:core:data` **não** embrulham chamadas de DAO em `withContext(Dispatchers.IO)` — o I/O dos DAOs `suspend` roda no dispatcher interno do Room. A **exceção** é o KDF de senha (`PasswordHasherImpl`): PBKDF2-HMAC-SHA256 com 120 000 iterações é trabalho de **CPU**, não de banco, e roda dentro de `withContext(Dispatchers.IO)` — sem isso o login e o cadastro congelam a interface, porque o `AuthViewModel` chama o caso de uso de dentro do `viewModelScope` (dispatcher principal). O `Dispatchers.IO` explícito também aparece em `MainActivity` (resolução da start-destination) e em `DeadlineReceiver`. Isole a camada com `suspend`/`Flow`, não com troca de dispatcher.

Navigation (estado real, não o ideal):
- **Uma fonte de verdade de rotas.** `BrainOutRoutes` (`app/src/main/.../navigation/BrainOutRoutes.kt`) é o registro central do `NavHost` (Splash/Login/Register/MainGraph/ProjectsGraph/TasksGraph/DashboardGraph/SettingsGraph/ProjectDetail). `TasksRoutes` (`:feature:tasks`) é a **única** feature que expõe extensões de grafo (`NavGraphBuilder.tasksGraph()` e `dashboardGraph()`), então é dona das rotas que usa — e esses valores não existem em `BrainOutRoutes`. As features `auth`, `projects` e `settings` **não** têm objeto de rotas: um objeto desconectado do `NavHost` nasce morto. Ao adicionar rota, edite `BrainOutRoutes` (ou o `*Routes` da feature que expõe grafo) e não crie um terceiro objeto.
- Splash/Login/Register/Home/ProjectDetail/Settings são montados por funções **privadas** em `BrainOutNavHost.kt` (`addSplashRoute`, `addLoginRoute`, …), que instanciam os composables das features diretamente.
- Mesmo assim, **nunca hardcode string de rota**: use `object XxxRoutes` + helper `fun xRoute(args)`.
- Sign-out: `activeUserProvider.signOut()` then `navigate(login) { popUpTo(0) { inclusive = true } }`.

Persistência local:
- Toda escrita dos repositórios passa por `LocalTransaction.run { … }` — a transação Room que garante que entidade e associações são persistidas juntas (`core/data/.../local/LocalTransaction.kt`). A antiga fila `pending_ops` foi removida na migração 4→5.
- Cascatas usam métodos `@Transaction` do DAO (`cascadeCompleteTask`, `cascadeReopenTask`).
- O lembrete de prazo tem **uma** regra, `reconcileTaskReminder` (`core/domain/.../notification/ReminderReconciler.kt`), usada por criar, editar e mudar status — o agendamento nunca diverge do estado da tarefa.

## Key Directories

| Dir | Purpose |
|---|---|
| `app/src/main/kotlin/.../BrainOutApplication.kt` | Hilt bootstrap + WorkManager Configuration provider. |
| `app/src/main/kotlin/.../MainActivity.kt` | Compose host, start-destination resolution, `POST_NOTIFICATIONS` request (Tiramisu+). |
| `app/src/main/kotlin/.../navigation/BrainOutNavHost.kt` | Root NavHost; funções privadas por destino + `tasksGraph()`/`dashboardGraph()`. |
| `app/src/main/kotlin/.../navigation/BrainOutRoutes.kt` | Registro central de rotas do NavHost (Splash→Login→Home→ProjectDetail/Settings). |
| `app/src/main/kotlin/.../notifications/` | `WorkManagerDeadlineScheduler` (impl of domain port), `DeadlineWorker`, `DeadlineReceiver`, `CompleteTaskWorker`, `DeadlineKeys`, `DeadlineNotificationId`, `NotificationDismissal`, `NotificationPermissionStore`. |
| `core/domain/src/main/kotlin/.../usecase/` | Use cases. Kover 60% gate — every new use case needs a test under `src/test/`. |
| `core/domain/src/main/kotlin/.../notification/DeadlineNotificationScheduler.kt` | Port interface; Android-free. |
| `core/domain/src/main/kotlin/.../notification/ReminderReconciler.kt` | Regra única de agendamento/cancelamento do lembrete de prazo. |
| `core/data/src/main/kotlin/.../local/BrainOutDatabase.kt` | Room DB v5. Schemas in `core/data/schemas/`. |
| `core/data/src/main/kotlin/.../local/LocalTransaction.kt` | Transação Room que envolve toda escrita dos repositórios. |
| `core/data/src/main/kotlin/.../di/DataModule.kt` | DI wiring principal do `:core:data`. `PepperProvider.Factory` fica **fora**, no próprio arquivo de `security/`. |
| `core/data/src/main/kotlin/.../security/PepperProvider.kt` | Pepper + `@Module object Factory` (segundo módulo Hilt do módulo). |
| `core/data/src/main/kotlin/.../repository/` | Repository impls; cada escrita passa por `LocalTransaction`. |
| `core/ui/src/main/kotlin/.../theme/` | Identidade Neo: `NeoColor.kt`, `NeoTokens.kt`, `NeoTypography.kt` (que declara `object NeoFonts`), `NeoColor.kt` (papéis → `ColorScheme`), `BrainOutNeoTheme.kt`, `Shape.kt` (`NeoShapes`), `Type.kt` (`NeoMaterialTypography`). `BrainOutTheme` delega para o tema Neo; **não existe mais paleta legada**. |
| `core/ui/src/main/res/` | `font/` com **5 fontes** locais (3× Archivo — bold/extra bold/semi bold — e 2× Public Sans — regular/semi bold) e `drawable/` com **28 drawables `neo_*`** (ícones/vetores da identidade). |
| `brand/` | Fonte da marca do app: `brainout-icon.svg` (fonte única do ícone), `generate_icons.py` (gera arte do `:core:ui`, adaptive icon e PNGs legados) e `README.md` com a proveniência. Regerar com `python brand/generate_icons.py` — nunca editar os recursos derivados à mão. |
| `feature/<x>/src/main/kotlin/.../ui/` | Screens + `*TestTags` objects for Compose UI tests. |
| `feature/<x>/src/main/kotlin/.../viewmodel/` or `ui/<x>/` | `@HiltViewModel` + `data class XxxUiState`. |
| `feature/tasks/src/main/kotlin/.../navigation/TasksRoutes.kt` | Única feature que expõe extensões de grafo — dona das rotas que usa. |
| `docs/ROADMAP.md`, `docs/ARQUITETURA.md`, `docs/CI-CD.md`, `docs/CONTRIBUTING.md` | Requirements R1–R14, E1.x–E5.x cycles, pipeline, conventional commits. |
| `Documentos/` | Deliverable PDFs only — never edit. |

## Development Commands

All Gradle commands from repo root. The build is verified on JDK 21 (what `ci.yml` and `release-apk.yml` provision). With a standalone JDK 21+: `export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64`; with no standalone JDK, the Android Studio JBR works too (`export JAVA_HOME=/opt/android-studio/jbr`). Gradle 9.8.1/AGP 9.4.1 accept a floor of 17; bytecode target stays Java 17.

```bash
# Build
./gradlew :app:assembleDebug                 # daily build (sem flavors)
./gradlew :app:assembleRelease               # unsigned unless BRAINOUT_* secrets are set

# Unit tests (includes :core:domain via gradle.projectsEvaluated wire-up)
./gradlew testDebugUnitTest                  # what CI runs — todos os módulos Android
./gradlew :core:domain:test                  # pure JVM tests
./gradlew :core:data:testDebugUnitTest       # Robolectric
./gradlew :core:ui:testDebugUnitTest         # tokens/tema Neo (Robolectric, recursos Android)
./gradlew :feature:auth:testDebugUnitTest    # ViewModel tests (R2)
./gradlew :feature:projects:testDebugUnitTest
./gradlew :feature:tasks:testDebugUnitTest
./gradlew :feature:settings:testDebugUnitTest
./gradlew :app:testDebugUnitTest              # smoke + WorkManager + notificação

# Instrumented tests (emulator) — compilam; rodam em `connected*`
./gradlew :core:data:assembleDebugAndroidTest    # MigrationTest + UserDaoInstrumentedTest
./gradlew :feature:auth:assembleDebugAndroidTest   # LoginScreenTest

# Coverage (60% bound, core only)
./gradlew :core:domain:koverVerify :core:data:koverVerify
./gradlew koverMergedHtmlReport              # aggregator

# Static analysis (CI runs these together)
./gradlew ktlintCheck detekt
./gradlew :app:lintDebug                     # Android lint — i18n MissingTranslation gate
```

A camada de rede foi removida no #212 e o diretorio `backend-stub/` foi apagado por inteiro no #218 (commit `05acf32`), junto com a fila `pending_ops` e o job `backend-integration`. O produto e local: nao reintroduza `BASE_URL`, `BuildConfig.BASE_URL`, flavors nem um cliente HTTP.

> **Nenhum modulo declara flavor.** Todas as tasks Android sao `testDebugUnitTest` / `assembleDebug` / `lintDebug` - nao existe `testDevDebugUnitTest`. O CI roda `testDebugUnitTest` (cobre os 8 modulos) mais os gates de Kover.

## Code Conventions & Common Patterns

- **Author header** — every `.kt`/`.kts`/`.xml` file starts with `// João Pedro G M Silva - PUC Goiás ADS - 20251012000740`.
- **Packages** lowercase only; strings `snake_case` with screen/feature prefix (`login_`, `home_`, `task_`, `project_detail_`, `bottom_tab_`, `common_`). Detekt `PackageNaming` enforces.
- **i18n gate** — `values/strings.xml` and `values-en/strings.xml` must stay in sync. Todo módulo com string de interface (`:app`, `:core:ui`, `:feature:auth`, `:feature:projects`, `:feature:settings` e `:feature:tasks`) enforce `MissingTranslation = error` + `lint { abortOnError = true }` — o `:feature:tasks`, que era o único relaxado, voltou ao gate rígido quando o `coreLibraryDesugaring` (DEF-63) eliminou o `NewApi` do `Instant#parse` no `@Preview`. `:core:data` também aborta em erro, mas não promove `MissingTranslation` porque não tem string de interface. Nunca suprima — traduza.
- **AGP 9 built-in Kotlin** — `kotlin-android` plugin is **removed** from the version catalog. Do not re-add. Remaining Kotlin plugins: `kotlin-jvm` (only `:core:domain`) and `kotlin-compose`.
- **Bytecode target** — Java 17 in all 8 modules (`jvmTarget.set(JvmTarget.JVM_17)`); build JDK is 21 (CI + release provision Temurin 21; JBR via `JAVA_HOME` locally). Use `kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }` — `kotlinOptions` is gone in AGP 9.
- **Sem flavors** — nenhum módulo declara `productFlavors`; não existe `missingDimensionStrategy`, `BuildConfig.BASE_URL` nem tarefa `*DevDebug*`. A direção é produto local (ver `docs/TRANSICAO-LOCAL.md`).
- **DI** — Hilt everywhere; ViewModels via `@HiltViewModel` + `@Inject` constructor. Domain layer may use `javax.inject.Inject` only. New `:core:data` bindings go in `DataModule` **ou** em um `@Module` dedicado quando o binding for de um componente autocontido — hoje são exatamente dois: `di/DataModule.kt` e o `PepperProvider.Factory` (`security/PepperProvider.kt:77-83`), que fornece o pepper ao hasher.
- **State management** — immutable `data class XxxUiState(...)` (per-screen error slots as nullable fields, no sealed class); expose `val uiState: StateFlow<XxxUiState>` via `stateIn(viewModelScope, WhileSubscribed(5_000), initial)`. One-shot events via `Channel`/`SharedFlow` or `MutableStateFlow<String>` for errorMessage + retry pattern with `_retryToken` + `flatMapLatest`.
- **Erros na UI são tipados, nunca texto solto** — o ViewModel emite um **enum** de erro (ex.: `AuthErrorType`) e a camada de UI escolhe o `stringResource`. O caminho antigo — ViewModel emitindo string em português e a UI fazendo `when` por casamento de texto — vazava português cru e quebrava silenciosamente em qualquer erro novo.
- **Module marker** — `PackageMarker.kt` (`internal const val <MODULE>_PACKAGE: String`) existe nos **8** módulos. O Kover exclui `*.PackageMarker` por padrão.
- **Repository mutations** — toda escrita passa por `LocalTransaction.run { … }` (uma transação Room). Cascade ops use `@Transaction` DAO methods (`cascadeCompleteTask`, `cascadeReopenTask`).
- **Navigation** — never hardcode route strings; use `object XxxRoutes` constants + helper `fun xRoute(args)` builders. Só `:feature:tasks` expõe extensões `NavGraphBuilder.*Graph()` (registradas por `:app`); as demais features são registradas por funções privadas do `BrainOutNavHost`, que conhece os composables. `BrainOutRoutes` é o registro central — não crie um objeto de rotas paralelo (ver *Navigation*).
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
| `gradle/wrapper/gradle-wrapper.properties` | Gradle 9.8.1. |
| `gradle.properties` | `-Xmx4g`, parallel + caching enabled, `useAndroidX=true`, `nonTransitiveRClass=true`. |
| `app/build.gradle.kts` | `coreLibraryDesugaring`, signing from env vars, lint gate. Sem flavors, sem `BuildConfig.BASE_URL`. |
| `core/data/build.gradle.kts` | Room `schemaLocation`, `testInstrumentationRunner`, `coreLibraryDesugaring`, Kover, `lint { abortOnError = true }`. Sem flavors. |
| `core/domain/build.gradle.kts` | Pure Kotlin JVM; Jacoco + Kover; no Android. |
| `core/ui/build.gradle.kts` | Lib Android sem flavor e **sem** `missingDimensionStrategy`; `isIncludeAndroidResources = true` (necessário para o teste Robolectric do `ColorScheme`). |
| `config/detekt/detekt.yml` | Single detekt config. `build.maxIssues=0`. Ignores `@Composable`/`@Preview`/`@HiltAndroidApp`/`@AndroidEntryPoint` for `FunctionNaming`. |
| `.github/workflows/ci.yml` | Jobs de análise estática, testes unitários (incl. Kover 60%) e lint. Roda `ktlintCheck`, `detekt`, `testDebugUnitTest`, `koverVerify`, `lintDebug`. |
| `.github/workflows/release-apk.yml` | Tag-triggered signed `.aab` build (4 secrets). |
| `.github/CODEOWNERS` | Single approver. |
| `.github/PULL_REQUEST_TEMPLATE.md` | Requires `Refs: R#` and forbids commented code. |
| `Documentos/` | Read-only PDFs (deliverable). Never edit. |

## Static Analysis

- **ktlint é aplicado a todos os subprojetos.** O root faz `subprojects { apply(plugin = ktlint); android = true }`, então `ktlintCheck` inspeciona **todos** os arquivos `.kt` versionados. O gate **bloqueia**: `ignoreFailures = false` (DEF-10 corrigido). Não comente isso como "ktlint está desligado" — se `ktlintCheck` falha, o build falha.
- **detekt** é aplicado por módulo (no `build.gradle.kts` de cada um) com config centralizada em `config/detekt/detekt.yml`. Roda com `build.maxIssues = 0`.
- **`.editorconfig` na raiz** — `ktlint_code_style = ktlint_official` (o estilo que o código segue de fato; `android_studio` foi testado e rejeitado por pedir reformatação massiva) e `ktlint_function_naming_ignore_when_annotated_with = Composable`, que espelha o `FunctionNaming.ignoreAnnotated` do detekt (`config/detekt/detekt.yml:82-88`) para as funções `@Composable` em PascalCase. É aditivo: não substitui detekt nem o catálogo de regras.
- **Android lint** — gate rígido de i18n em todo módulo com string de interface (`:app`, `:core:ui`, `:feature:auth`, `:feature:projects`, `:feature:tasks`, `:feature:settings`); `:core:data` aborta em erro sem promover `MissingTranslation` (não tem string de interface). Nenhum módulo de interface está relaxado.

## Runtime / Tooling Preferences

- **Android SDK**: `compileSdk 37`, `targetSdk 37` (só em `:app`), `minSdk 24` em todos os módulos Android. `coreLibraryDesugaring` está ligado em `:app` **e** em `:core:data` (`desugar_jdk_libs 2.1.5`) — o que fecha a dívida de `java.time`/`java.util.Base64` com `minSdk 24`; `:core:data` hoje roda com `lint { abortOnError = true }`. O platform instalado é `android-37.0` (Android 17, final — não é preview). `buildToolsVersion` não é declarado de propósito; o AGP 9.4.1 escolhe.
- **JDK**: bytecode target Java 17. CI (`ci.yml`) e release (`release-apk.yml`) provisionam **Temurin 21** (Gradle 9.8.1 roda em JVM 17–27 e o piso do AGP 9.4.1 é 17 — 21 é o padrão unificado do projeto). Localmente: `export JAVA_HOME=/opt/android-studio/jbr` (JBR 25 do Android Studio) antes de `./gradlew`. `local.properties` precisa de `sdk.dir`.
- **No package manager other than Gradle** for the Android side. **Python 3.12**, sem `pip`/`venv` no caminho do dia a dia: os verificadores documentais que a CI executa (`.github/scripts/verify_*.py`, `docs/design/verify_*.py`, `.github/tests/test_release_tag.py`) são só stdlib; `brand/generate_icons.py` (arte da marca) é o único script com terceiros (`cairosvg`, `Pillow`) e roda num venv próprio.
- **Android Studio / lint / ktlint / detekt** are the only static analysis tools. No Checkstyle, no Spotless, no Sonar.
- **Kover 0.9.11** only on `:core:domain` and `:core:data`; everywhere else is exempt.

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

**Local check limitation.** `detekt` deriva `--jvm-target` do JVM do daemon Gradle e **rejeita `25`** (o JBR 25 do Android Studio). Ele roda bem em JDK 21 — localmente, aponte o daemon para um JDK 21 (`gradle/gradle-daemon-jvm.properties` fixa o critério do daemon; `toolchainVersion=21` resolve) e rode `detekt` junto de `ktlintCheck`, `testDebugUnitTest` e `koverVerify`. Não adicione workarounds por módulo para silenciar isso.

## Testing & QA

Frameworks (all from `gradle/libs.versions.toml`):

- **JUnit 4** (4.13.2) — no JUnit 5. `androidx.test.ext:junit` 1.3.0 for Android.
- **Google Truth** (1.4.5) — primary assertion lib.
- **MockK** (1.14.11) — `coEvery` / `coVerify` / `mockk`. `mockk-android` only in `:feature:auth/androidTest`.
- **Robolectric** (4.17) — Room/Hilt/WorkManager/tema JVM tests: `@RunWith(RobolectricTestRunner::class)`. Muitos usam `@Config(sdk = [34], manifest = Config.NONE)`; as telas Compose usam `@Config(qualifiers = "pt-rBR")` para resolver `values/`. **`:core:ui` precisa de `isIncludeAndroidResources = true`** para o teste de `ColorScheme`/tokens Neo.
- **Turbine** (1.2.1) — every ViewModel test uses `app.cash.turbine.test`.
- **kotlinx-coroutines-test** (1.11.0) — `runTest { ... }`.
- **Compose UI Test** (JUnit4 runner, BOM-pinned) — `createComposeRule`, `onNodeWithTag`, `assertIsDisplayed`.

How to run:

```bash
./gradlew testDebugUnitTest                      # todos os módulos Android
./gradlew :core:domain:test                      # pure JVM, fast
./gradlew :core:domain:koverVerify :core:data:koverVerify   # 60% gate
./gradlew koverMergedHtmlReport                  # aggregated HTML
./gradlew connectedDebugAndroidTest              # instrumented — emulador
```

Suite instrumentada de `:core:data` (`src/androidTest/`): **`MigrationTest`** cobre as 4 migrations — `MIGRATION_1_2`, `MIGRATION_2_3`, `MIGRATION_3_4`, `MIGRATION_4_5` — cada uma individualmente e a cadeia 1→5 — e **`UserDaoInstrumentedTest`** faz CRUD real. As duas compilam (`:core:data:assembleDebugAndroidTest`); execução exige emulador.

Coverage expectations:
- **Kover 60% line bound** enforced on `:core:domain` and `:core:data` only.
- Class exclusions centralized in root `build.gradle.kts` (`*Dao_Impl`, `*Database_Impl`, `*_Factory`, `Hilt_*`, `*.di.*`, `*.BuildConfig`, `*.PackageMarker`) — nunca uma classe de produção com lógica.
- `gradle.projectsEvaluated` in root makes every `test*UnitTest` Android task depend on `:core:domain:test` — do not remove; without it, CI won't run domain tests when invoked via an Android module.
- New `:core:domain/usecase/` requires a test in `:core:domain/src/test/.../usecase/` (`XxxUseCaseTest`).
- New repository impl in `:core:data` follows the `Fake*Dao` pattern (`FakeProjectDao`, `FakeTagDao` private classes inside the test) — no MockK for DAOs.
- Test naming: backtick-quoted Portuguese descriptive sentences (e.g. `fun \`atualizar tarefa valida erro de titulo vazio\``). **Sem `:` no nome** — o KSP gera stub Java do método e `:` vira identificador JVM inválido (`Name contains illegal characters`).

Correções recentes que valem como contrato de teste (não reverta sem atualizar o teste):
- `PasswordHasherImpl.verify` **lê as iterações do próprio hash armazenado** (`alg$iter$salt$hash`) em vez de usar uma constante fixa, e valida a faixa em `pbkdf2`. Hash gravado com iterações diferentes verifica corretamente.
- `SortOrder.toStorageKey()` emite a grafia `snake_case` (`name_asc`, `name_desc`, `created_desc`, `created_asc`) — exatamente a que a ordenação dinâmica de `ProjectDao.searchProjects` compara em SQL. `fromStorageKey` ainda aceita a grafia concatenada legada (`nameasc`) para preferências já gravadas.
- Hierarquia única de exceções: `BusinessRuleException : DomainException`, então `catch (DomainException)` captura violações de RN. Contrato fixado por `DomainExceptionHierarchyTest`.
- `TaskPriority.fromCode` é a única conversão de código de prioridade; a faixa válida vive em `TaskPriority.VALID_RANGE`.
- `reconcileTaskReminder` é a única regra de lembrete de prazo: concluída cancela, ativa com prazo futuro agenda, ativa sem prazo futuro cancela.

Anti-patterns (do NOT):
- Do not add Android/Room/Hilt dependencies to `:core:domain`.
- Do not commit `local.properties`, `keystore.properties`, `*.jks`, or any keystore file (all gitignored). CI uses `BRAINOUT_KEYSTORE_BASE64`.
- Do not write `fallbackToDestructiveMigration`; write a hand-rolled `Migration` and add it to `DataModule` (existing: `MIGRATION_1_2`, `MIGRATION_2_3`, `MIGRATION_3_4`, `MIGRATION_4_5`).
- Do not reintroduce a camada de rede — nem `BASE_URL`, nem Retrofit, nem flavors, nem fila de sync.
- Do not leave commented-out code.
- Do not suppress `MissingTranslation`; add the `values-en/` translation.
- Do not add a new `withContext(Dispatchers.IO)` to `:core:data` — the Room DAO suspend already runs off the main thread. A exceção é o KDF de senha (`PasswordHasherImpl`), que é CPU-bound e não I/O de banco.
- Do not introduce a second route source of truth; extend `BrainOutRoutes` (ou o `*Routes` da única feature que expõe grafo).
- Do not edit `Documentos/`, `.omo/`, `docs/ATAS/`.
- Do not run `assembleRelease` locally expecting signed output without the 4 env vars set.

## Known gaps in this guide

Honest inventory so an agent does not assume the docs are complete:

- **`:core:ui` não tem `AGENTS.md`.** Só `:core:domain`, `:core:data` e `feature/` têm sub-guia; `:app` também não tem. As regras de `:core:ui` (tokens Neo, fontes, drawables, `isIncludeAndroidResources`) vivem apenas na seção *Key Directories* acima.
- **`:app` não tem `AGENTS.md`.** Suas regras (workers de notificação, NavHost, lint rígido) estão só no root.
- **`:feature:projects`, `:feature:tasks` e `:feature:settings` não têm `AGENTS.md` próprio** — só `feature/AGENTS.md` cobre as regras comuns das features.
- **A camada de backup local (#214) ainda tem dívida**: `BackupSection.kt` (218 LoC) ainda concentra a seção, o cartão e o nome do arquivo sugerido. A mensagem do último resultado saiu para `BackupMessage.kt`, os diálogos para `BackupDialogs.kt` e o estado para `BackupUiState.kt`/`BackupViewModel.kt`; o que ainda pesa é o tamanho do arquivo.
- **O `HomeListingPipeline` ainda combina muitos fluxos** (`combine` de preferências, busca debounceada, tags e filtro) e o `homeUiStateFlow` tem dez parâmetros. A entrada já é explícita (`ListingInputs`) e a consulta ao Room é controlada só pelo texto debounceado (PR #281) — mas um agrupamento das dependências em um objeto de contexto ainda é trabalho em aberto.
