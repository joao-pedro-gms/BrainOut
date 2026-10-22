# feature/ — Módulos de feature (Compose + Hilt + ViewModel + StateFlow)

> Atualização #212: todos os módulos agora são locais e sem flavors.
> Não usar missingDimensionStrategy nem tarefas DevDebug. ViewModels não
> consomem ConnectivityObserver/PendingSyncMonitor; erros e retry locais
> continuam válidos. As referências a flavors/sync abaixo são históricas.
>
> Limpeza #226: `AuthRoutes`, `ProjectsRoutes` e `SettingsRoutes` foram
> removidos — só os próprios testes os usavam. `BrainOutRoutes` (em
> `:app`) é o registro central; `TasksRoutes` (em `:feature:tasks`) é a
> única feature que expõe extensões de grafo e, portanto, dona das
> rotas que usa.

// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Convenção operacional dos 4 módulos :feature:* — referência operacional para
// quem vai criar uma tela nova, um caso de uso novo ou um teste novo.

## OVERVIEW

Quatro módulos Gradle independentes (auth, projects, tasks, settings). Cada um
implementa um recorte funcional da UI em Compose + Material 3, consumindo
`:core:domain`, `:core:data` e `:core:ui`. Mesmo template, mesma política de
testes, mesmo par de idiomas.

Nenhum dos 4 tem flavor — e nenhum precisa: não existe dimensão
`environment` para casar. **Consequência prática: a task de teste é
`testDebugUnitTest` (e `assembleDebugAndroidTest`), para os 4.**

## MODULE TEMPLATE

Layout canônico por módulo (a partir de `feature/<name>/src/`):

```
main/kotlin/pucgo/joaopedrogmsilva/brainout/feature/<name>/
  PackageMarker.kt                 // marcador de convenção (não deletar)
  navigation/<Name>Routes.kt       // só :feature:tasks (constantes + NavGraph)
  ui/                              // uma pasta por tela: Screen.kt + opcional ViewModel.kt
  viewmodel/                       // SOMENTE :feature:auth usa este pacote
main/res/values/strings.xml        // pt-BR
main/res/values-en/strings.xml      // en — obrigatório, paridade chave a chave
test/kotlin/.../...                // espelha main; *Test.kt / *ViewModelTest / *ScreenTest
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
- **Não existe objeto de rotas em auth/projects/settings.** As rotas vivem
  em `BrainOutRoutes` (em `:app`), que é o registro central do `NavHost`.
  `TasksRoutes` é a única feature que expõe extensões de grafo — e por
  isso é dona das rotas que usa; seus valores **não** existem em
  `BrainOutRoutes`.
  Ao criar feature nova: **não** crie um `*Routes.kt` desconectado do
  `NavHost` — ele nasce morto. Se a feature expuser extensões de grafo,
  registre-as no `NavHost` no mesmo PR (estilo `tasksGraph()`); senão,
  use `BrainOutRoutes`.

## WHERE TO LOOK

| Funcionalidade         | Telas                                                                 | ViewModel                | Rotas                | Notas                                                                              |
|------------------------|-----------------------------------------------------------------------|--------------------------|----------------------|------------------------------------------------------------------------------------|
| `:feature:auth` (R2)   | `ui/login/LoginScreen.kt`, `ui/register/RegisterScreen.kt`, `ui/splash/SplashScreen.kt` | `viewmodel/AuthViewModel.kt` + `AuthUiState.kt` + `AuthErrorType.kt` | — (`BrainOutRoutes`) | Único módulo com `androidTest/` (LoginScreenTest). VMs em `viewmodel/`. Erros tipados por enum, traduzidos na UI. |
| `:feature:projects` (R3–R5) | `ui/home/HomeScreen.kt` (+ `HomeUser.kt`, `HomeProjectCard.kt`), `ui/projectdetail/ProjectDetailScreen.kt` (+ `DeadlineField.kt`) | `ui/home/HomeViewModel.kt`, `ui/projectdetail/ProjectDetailViewModel.kt` | — (`BrainOutRoutes`) | `DeadlineField.kt` é composable reutilizável de prazo; consome `CheckDeadlineUseCase` de `:core:domain`. |
| `:feature:tasks` (R7–R8) | `ui/TasksScreen.kt`, `ui/DashboardScreen.kt`                        | `ui/TasksViewModel.kt`, `ui/DashboardViewModel.kt` | `navigation/TasksRoutes.kt` | **Única feature com `*Graph()`** (`tasksGraph()`/`dashboardGraph()`) — e por isso dona das rotas que usa; o `:app` as registra em `navigation/BrainOutShell.kt`. `DashboardScreenTest` roda em JVM via Robolectric (`@Config(qualifiers="pt-rBR")`). |
| `:feature:settings` (R9) | `ui/SettingsScreen.kt` (+ `SettingsStructure.kt`)                   | — (sem VM)               | — (`BrainOutRoutes`) | **Maior gap de VM:** sem ViewModel; cobertura é `SettingsScreenTest` + `BackupSectionTest`. |

## CONVENTIONS

- Nenhum módulo declara flavor e nenhum fixa `missingDimensionStrategy` —
  não existe dimensão `environment` para casar.
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
- **Não** duplicar `NavHost` dentro de uma feature. O grafo raiz fica em
  `:app/navigation/BrainOutNavHost.kt`. Não registre `composable(...)` da sua
  própria feature de dentro dela — quem registra o destino é o NavHost (via
  `*Graph()` ou função privada).
- **Não** criar um objeto de rotas que ninguém navega. As rotas de auth,
  projects e settings vivem em `BrainOutRoutes` (em `:app`); `AuthRoutes`,
  `ProjectsRoutes` e `SettingsRoutes` foram removidos exatamente por isso.
  Só `:feature:tasks` tem `*Routes.kt`, porque expõe extensões de grafo.
- **Não** emitir erro como texto solto do ViewModel. Emita um **enum** de
  erro e deixe a UI escolher o `stringResource` — texto solto vaza
  português cru e quebra em qualquer erro novo.
- **Não** acessar `Context`/`Resources` direto do ViewModel para string —
  ViewModel emite estado, composable resolve `stringResource(...)`.
- **Não** suprimir `MissingTranslation` nos módulos com lint rígido — adicione a
  tradução em `values-en/`.

## TESTES — pontos de atenção

- Nomenclatura: `<Classe>Test.kt`. Tela Compose → `<Screen>ScreenTest.kt` (ex.:
  `LoginScreenTest`, `DashboardScreenTest`). Não há mais `*RoutesTest.kt` — os
  objetos de rota que só os testes usavam foram removidos.
- **Task de execução:** `./gradlew :feature:<name>:testDebugUnitTest`.
  `:feature:auth` ainda tem `androidTest/` →
  `assembleDebugAndroidTest` / `connectedDebugAndroidTest`.
- Cobertura obrigatória de Kover (60%) só em `:core:domain` e `:core:data` —
  features **não** entram no gate.
- `androidTest/` só em `:feature:auth` (gancho histórico para LoginScreenTest).
  Demais telas testam em JVM com Robolectric (`@Config(sdk=[34])`,
  `qualifiers="pt-rBR"` para garantir resolução de `values/`).
- Gaps conhecidos: `:feature:settings` sem ViewModel (só `SettingsScreenTest` +
  `BackupSectionTest`); `:feature:projects` e `:feature:tasks` sem Compose UI
  test para `HomeScreen`, `ProjectDetailScreen` e `TasksScreen` (`:feature:tasks`
  cobre `DashboardScreen`, mas não `TasksScreen`). Cobertura Compose não é gate
  do CI; tratar antes do marco E5.1.
