# feature/ — Módulos de feature (Compose + Hilt + ViewModel + StateFlow)

// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Convenção operacional dos 4 módulos :feature:* — referência operacional para
// quem vai criar uma tela nova, um caso de uso novo ou um teste novo.

## OVERVIEW

Quatro módulos Gradle independentes (auth, projects, tasks, settings). Cada um
implementa um recorte funcional da UI em Compose + Material 3, consumindo
`:core:domain`, `:core:data` e `:core:ui`. Mesmo template, mesma política de
testes, mesmo par de idiomas.

## MODULE TEMPLATE

Layout canônico por módulo (a partir de `feature/<name>/src/`):

```
main/kotlin/pucgo/joaopedrogmsilva/brainout/feature/<name>/
  PackageMarker.kt                 // marcador de convenção (não deletar)
  navigation/<Name>Routes.kt       // objeto/função com rotas + NavGraph
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

## WHERE TO LOOK

| Funcionalidade         | Telas                                                                 | ViewModel                | Rotas                | Notas                                                                              |
|------------------------|-----------------------------------------------------------------------|--------------------------|----------------------|------------------------------------------------------------------------------------|
| `:feature:auth` (R2)   | `ui/login/LoginScreen.kt`, `ui/register/RegisterScreen.kt`, `ui/splash/SplashScreen.kt` | `viewmodel/AuthViewModel.kt` + `AuthUiState.kt` | `navigation/AuthRoutes.kt` | Único módulo com `androidTest/` (LoginScreenTest). VMs em `viewmodel/`.            |
| `:feature:projects` (R3–R5) | `ui/home/HomeScreen.kt` (+ HomeTab, HomeUser), `ui/projectdetail/ProjectDetailScreen.kt` (+ `DeadlineField.kt`) | `ui/home/HomeViewModel.kt`, `ui/projectdetail/ProjectDetailViewModel.kt` | `navigation/ProjectsRoutes.kt` | `DeadlineField.kt` é composable reutilizável de prazo; consome `CheckDeadlineUseCase` de `:core:domain`. |
| `:feature:tasks` (R7–R8) | `ui/TasksScreen.kt`, `ui/DashboardScreen.kt`                        | `ui/TasksViewModel.kt`, `ui/DashboardViewModel.kt` | `navigation/TasksRoutes.kt` | `DashboardScreenTest` roda em JVM via Robolectric (`@Config(qualifiers="pt-rBR")`). |
| `:feature:settings` (R9) | `ui/SettingsScreen.kt` (+ `SettingsStructure.kt`)                   | — (sem VM)               | `navigation/SettingsRoutes.kt` | **Maior gap:** só tem `RoutesTest`; falta `SettingsViewModelTest` e teste de UI. |

## CONVENTIONS

- `defaultConfig.missingDimensionStrategy("environment", "dev")` em **todos** os
  `build.gradle.kts` — herdam flavor do `:app` e `:core:data` fixando a
  dimensão.
- Toda string nova vai em `values/strings.xml` **e** `values-en/strings.xml`,
  com a mesma chave. `lint { abortOnError = true; error += "MissingTranslation" }`
  falha o build se faltar a par — não suprimir.
- `@Preview` em todo composable reutilizável (telas inteiras e blocos
  parametrizados). Wrapper: `BrainOutTheme { Surface { ... } }`.
- ViewModel expõe `StateFlow<UiState>` imutável (data class) + `Channel<Effect>`
  para eventos one-shot. `viewModelScope` + `Dispatchers.IO` para I/O.
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
  `*Routes.kt` (rotas + extensão `NavGraphBuilder.<name>()`); o grafo raiz
  fica em `:app/navigation/BrainOutNavHost.kt`.
- **Não** acessar `Context`/`Resources` direto do ViewModel para string —
  ViewModel emite estado, composable resolve `stringResource(...)`.

## TESTES — pontos de atenção

- Nomenclatura: `<Classe>Test.kt`. Tela Compose → `<Screen>ScreenTest.kt` (ex.:
  `LoginScreenTest`, `DashboardScreenTest`). Rota → `<Feature>RoutesTest.kt`.
- Cobertura obrigatória de Kover (60%) só em `:core:domain` e `:core:data` —
  features **não** entram no gate.
- `androidTest/` só em `:feature:auth` (gancho histórico para LoginScreenTest).
  Demais telas testam em JVM com Robolectric (`@Config(sdk=[34])`,
  `qualifiers="pt-rBR"` para garantir resolução de `values/`).
- Gaps conhecidos: `:feature:settings` sem VM/UI test;
  `:feature:projects` e `:feature:tasks` sem Compose UI test para `HomeScreen`,
  `ProjectDetailScreen`, `TasksScreen`. Cobertura Compose não é gate do CI;
  tratar antes do marco E5.1.
