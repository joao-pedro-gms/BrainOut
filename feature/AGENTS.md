# feature/ — Módulos de feature (Compose + Hilt + ViewModel + StateFlow)

// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Convenção operacional dos 4 módulos :feature:* — referência operacional para
// quem vai criar uma tela nova, um caso de uso novo ou um teste novo.

## OVERVIEW

Quatro módulos Gradle independentes (auth, projects, tasks, settings). Cada um
implementa um recorte funcional da UI em Compose + Material 3, consumindo
`:core:domain`, `:core:data` e `:core:ui`. Mesmo template, mesma política de
testes, mesmo par de idiomas.

Nenhum dos 4 tem flavor próprio: cada um fixa
`missingDimensionStrategy("environment", "dev")` para casar com a dimensão
`environment` de `:core:data`. **Consequência prática: a task de teste é
`testDebugUnitTest` (e `assembleDebugAndroidTest`), não `testDevDebugUnitTest`
como em `:app`/`:core:data`.**

## MODULE TEMPLATE

Layout canônico por módulo (a partir de `feature/<name>/src/`):

```
main/kotlin/pucgo/joaopedrogmsilva/brainout/feature/<name>/
  PackageMarker.kt                 // marcador de convenção (não deletar)
  navigation/<Name>Routes.kt       // constantes de rota (+ NavGraph só em :feature:tasks)
  ui/                              // uma pasta por tela: Screen.kt + opcional ViewModel.kt
  viewmodel/                       // SOMENTE :feature:auth usa este pacote
main/res/values/strings.xml        // pt-BR
main/res/values-en/strings.xml      // en — obrigatório, paridade chave a chave
test/kotlin/.../...                // espelha main; *Test.kt / *RoutesTest / *ViewModelTest / *ScreenTest
androidTest/kotlin/...             // só :feature:auth usa (LoginScreenTest)
```

**Inconsistência (sinalizar):** `:feature:auth` mantém VMs em pacote dedicado
`viewmodel/` (AuthViewModel + AuthUiState). `:feature:projects` e
`:feature:tasks` colacam VMs em `ui/<tela>/`. Ao criar feature nova, alinhe
pelo padrão mais recente (colocation em `ui/`); não migrar `:feature:auth`
agora (baixa prioridade, faria diff amplo).

## NAVEGAÇÃO — como o grafo é montado de fato

Estado verificado em `app/src/main/.../navigation/BrainOutNavHost.kt`:

- **Só `:feature:tasks` expõe extensões de grafo**: `NavGraphBuilder.tasksGraph()`
  e `NavGraphBuilder.dashboardGraph()` (`feature/tasks/.../navigation/TasksRoutes.kt`).
  São as únicas duas registradas pelo NavHost raiz.
- **auth, projects e settings NÃO expõem `*Graph()`.** Splash/Login/Register/
  Home/ProjectDetail/Settings são montados por funções **privadas** dentro de
  `BrainOutNavHost.kt` (`addSplashRoute`, `addLoginRoute`, `addRegisterRoute`,
  `addHomeRoute`, `addProjectDetailRoute`, `addSettingsRoute`), que chamam
  `composable(...)` e instanciam os composables das features **diretamente**.
  Consequência: `:app` conhece os composables, não só as rotas.
- **As constantes de rota de auth/projects/settings estão duplicadas.**
  `AuthRoutes`, `ProjectsRoutes` e `SettingsRoutes` existem nos módulos, mas são
  consumidos **apenas pelos próprios testes** (`*RoutesTest.kt`) — o `NavHost`
  usa `BrainOutRoutes` (em `:app`) e `TasksRoutes` (real, usado em
  `BrainOutNavHost` e em `HomeScreen`). `TasksRoutes` é a única fonte de rota de
  feature efetivamente em produção.
  Ao criar feature nova: exponha as rotas em `*Routes.kt` **e** registre o
  destino — no estilo `tasksGraph()`, ou via função privada no `BrainOutNavHost`.
  Um objeto de rotas desconectado do `NavHost` nasce morto.

## WHERE TO LOOK

| Funcionalidade         | Telas                                                                 | ViewModel                | Rotas                | Notas                                                                              |
|------------------------|-----------------------------------------------------------------------|--------------------------|----------------------|------------------------------------------------------------------------------------|
| `:feature:auth` (R2)   | `ui/login/LoginScreen.kt`, `ui/register/RegisterScreen.kt`, `ui/splash/SplashScreen.kt` | `viewmodel/AuthViewModel.kt` + `AuthUiState.kt` | `navigation/AuthRoutes.kt` | Único módulo com `androidTest/` (LoginScreenTest). VMs em `viewmodel/`. `AuthRoutes` só usado pelo próprio teste. |
| `:feature:projects` (R3–R5) | `ui/home/HomeScreen.kt` (+ HomeTab, HomeUser), `ui/projectdetail/ProjectDetailScreen.kt` (+ `DeadlineField.kt`) | `ui/home/HomeViewModel.kt`, `ui/projectdetail/ProjectDetailViewModel.kt` | `navigation/ProjectsRoutes.kt` | `DeadlineField.kt` é composable reutilizável de prazo; consome `CheckDeadlineUseCase` de `:core:domain`. `ProjectsRoutes` só usado pelo próprio teste. |
| `:feature:tasks` (R7–R8) | `ui/TasksScreen.kt`, `ui/DashboardScreen.kt`                        | `ui/TasksViewModel.kt`, `ui/DashboardViewModel.kt` | `navigation/TasksRoutes.kt` | **Única feature com `*Graph()`** (`tasksGraph()`/`dashboardGraph()`) e única com `TasksRoutes` em produção. `DashboardScreenTest` roda em JVM via Robolectric (`@Config(qualifiers="pt-rBR")`). |
| `:feature:settings` (R9) | `ui/SettingsScreen.kt` (+ `SettingsStructure.kt`)                   | — (sem VM)               | `navigation/SettingsRoutes.kt` | **Maior gap de VM:** sem ViewModel; cobertura é `RoutesTest` + `SettingsScreenTest`. `SettingsRoutes` só usado pelo próprio teste. |

## CONVENTIONS

- `defaultConfig.missingDimensionStrategy("environment", "dev")` em **todos** os
  `build.gradle.kts` — herdam flavor do `:app` e `:core:data` fixando a
  dimensão.
- Toda string nova vai em `values/strings.xml` **e** `values-en/strings.xml`,
  com a mesma chave. `:feature:auth`, `:feature:projects` e `:feature:settings`
  usam `lint { abortOnError = true; error += "MissingTranslation" }`, que falha
  o build se faltar a par — não suprimir. (`:feature:tasks` é a exceção: roda
  `abortOnError = false` por causa de um `[NewApi]` pré-existente de
  `Instant#parse` em `@Preview` com minSdk 24.)
- `@Preview` em todo composable reutilizável (telas inteiras e blocos
  parametrizados). Wrapper: **`BrainOutTheme`** de `:core:ui` — ela já delega
  para a identidade Neo (`BrainOutNeoTheme`), então basta
  `BrainOutTheme { Surface { ... } }`; não instancie `BrainOutNeoTheme`
  diretamente nas features.
- ViewModel expõe `StateFlow<UiState>` imutável (data class) + `Channel<Effect>`
  para eventos one-shot. Use `viewModelScope` para I/O — os repositórios já
  saem da main thread (DAOs `suspend` do Room), então não embrulhe tudo em
  `Dispatchers.IO`.
- Teste de VM: `runTest` + `StandardTestDispatcher` + Turbine (`flow.test {}`)
  + Mockk + Truth. Teste Compose UI: `createComposeRule` + `BrainOutTheme` +
  VM injetado por construtor (mocks dos use cases) — **sem HiltAndroidRule**.

## ANTI-PATTERNS

- **Não** colocar regra de negócio em composable. Cálculos, validações,
  combinações de campos → use case em `:core:domain`. Composable só observa
  `UiState` e despacha intenções.
- **Não** importar diretamente outro `:feature/*` (acoplamento entre features).
  Dependência entre features só via portas em `:core:domain` (interfaces) +
  implementações expostas por `:core:data`.
- **Não** duplicar `NavHost` dentro de uma feature. Cada feature expõe apenas
  as rotas em `*Routes.kt`; o grafo raiz fica em
  `:app/navigation/BrainOutNavHost.kt`. Não registre `composable(...)` da sua
  própria feature de dentro dela — quem registra o destino é o NavHost (via
  `*Graph()` ou função privada).
- **Não** criar um objeto de rotas que ninguém navega. Hoje `AuthRoutes`,
  `ProjectsRoutes` e `SettingsRoutes` existem e só os testes usam; rotas novas
  precisam ser conectadas ao `NavHost` ou nascem mortas.
- **Não** acessar `Context`/`Resources` direto do ViewModel para string —
  ViewModel emite estado, composable resolve `stringResource(...)`.
- **Não** suprimir `MissingTranslation` nos módulos com lint rígido — adicione a
  tradução em `values-en/`.

## TESTES — pontos de atenção

- Nomenclatura: `<Classe>Test.kt`. Tela Compose → `<Screen>ScreenTest.kt` (ex.:
  `LoginScreenTest`, `DashboardScreenTest`). Rota → `<Feature>RoutesTest.kt`.
- **Task de execução:** `./gradlew :feature:<name>:testDebugUnitTest` (módulos
  sem flavor). `:feature:auth` ainda tem `androidTest/` →
  `assembleDebugAndroidTest` / `connectedDebugAndroidTest`.
- Cobertura obrigatória de Kover (60%) só em `:core:domain` e `:core:data` —
  features **não** entram no gate.
- `androidTest/` só em `:feature:auth` (gancho histórico para LoginScreenTest).
  Demais telas testam em JVM com Robolectric (`@Config(sdk=[34])`,
  `qualifiers="pt-rBR"` para garantir resolução de `values/`).
- Gaps conhecidos: `:feature:settings` sem ViewModel (só `RoutesTest` +
  `SettingsScreenTest`); `:feature:projects` e `:feature:tasks` sem Compose UI
  test para `HomeScreen`, `ProjectDetailScreen` e `TasksScreen` (`:feature:tasks`
  cobre `DashboardScreen`, mas não `TasksScreen`). Cobertura Compose não é gate
  do CI; tratar antes do marco E5.1.
