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
| R5 | Persistência local com tratamento de ausência de conectividade | **Parcial** | Persistência local atendida: `core/data/.../local/BrainOutDatabase.kt` (Room, versão 3), DAOs em `core/data/.../local/dao/`, migrações versionadas em `Migrations.kt`, testes Robolectric (`UserDaoTest`, `ProjectTaskCascadeTest`). Tratamento de conectividade (banner offline + fila visível) pendente do marco E3.4 | E1.5, E1.8 (atendidos); E3.3, E3.4 (em andamento) |
| R6 | Persistência remota com sincronização de dados | **Parcial** | Cliente HTTP Retrofit por flavor implementado: `core/data/.../remote/RemoteDataSource.kt`, `BrainOutApi.kt` (`BuildConfig.BASE_URL` injetada por flavor `dev`/`prod` em `core/data/build.gradle.kts`), stub FastAPI em `backend-stub/server.py`, testes com MockWebServer (`BrainOutApiTest`). Sincronização bidirecional Room ↔ backend pendente do marco E3.3 | E3.1, E3.2 (atendidos); E3.3 (em andamento) |
| R7 | Consumo de serviço ou interface de programação externa | **Atendido** | BrasilAPI (feriados nacionais): `core/data/.../remote/HolidayApi.kt` e `HolidayRemoteDataSource.kt` (`BuildConfig.HOLIDAYS_BASE_URL`), repositório `core/data/.../repository/HolidayRepositoryImpl.kt`, modelo `core/domain/.../model/Holiday.kt` e `CheckDeadlineUseCase`; teste com MockWebServer em `core/data/src/test/.../remote/HolidayRepositoryTest.kt` | E3.5 |
| R8 | Uso de recurso nativo do dispositivo | **Atendido** | Notificações locais de prazo: `app/src/main/kotlin/.../notifications/DeadlineWorker.kt`, `DeadlineReceiver.kt`, `CompleteTaskWorker.kt`, `WorkManagerDeadlineScheduler.kt`, `NotificationPermissionStore.kt`; permissão runtime `POST_NOTIFICATIONS` e canal em `app/src/main/AndroidManifest.xml` | E3.6 |
| R9 | Listagens com filtro ou busca e visão consolidada de dados | **Parcial** | Busca/filtro/ordenação atendidos: `core/data/.../local/dao/ProjectDao.kt` (`searchProjects`), `core/data/.../preferences/ListingPreferencesRepositoryImpl.kt` (DataStore), `feature/projects/.../ui/home/HomeScreen.kt` + `HomeViewModel.kt` (E2.6, PR #62). Visão consolidada (Dashboard) pendente do marco E2.7 | E2.6 (atendido); E2.7 (em andamento) |
| R10 | Tratamento de erros e indicação de estados de interface | **Atendido** | `feature/projects/.../ui/home/HomeViewModel.kt` (`errorMessage`, `retry`), `ProjectDetailViewModel` e `feature/tasks/.../ui/TasksViewModel.kt` — `.catch` nos Flows do Room, empty states e loaders nas screens; validação inline; mensagens localizadas em `app/src/main/res/values[-en]/strings.xml` | E2.8, E1.6 |
| R11 | Usabilidade e acessibilidade conforme diretrizes da plataforma | **Atendido** | Tokens de tema em `core/ui/.../theme/` (`Color.kt`, `Theme.kt`); contraste WCAG AA coberto por `core/ui/src/test/.../ContrastRatioTest.kt` (24 testes); áreas de toque ≥ 48dp em `AssistChip`/`FilterChip` de `:feature:projects` e `:feature:tasks`; `contentDescription` e TalkBack conforme `docs/ACESSIBILIDADE.md` | E4.4, E4.5 |
| R12 | Organização do código em camadas, sem credenciais versionadas | **Atendido** | Camadas: `:core:domain` (modelos + use cases puros), `:core:data` (Room/Retrofit/DataStore + DI em `DataModule.kt`), `:core:ui`, `:feature:*` — raiz `pucgo.joaopedrogmsilva.brainout` (E1.1/E1.2). Credenciais: `local.properties` fora do versionamento (`.gitignore`), keystore via secrets (`docs/CI-CD.md`), cobertura ≥ 60% por Kover em `:core:domain`/`:core:data` (E4.7) | E1.1, E3.2, E4.7 |
| R13 | Repositório Git com histórico distribuído e README | **Atendido** | `README.md` (instruções de build/execução), `docs/CONTRIBUTING.md`, histórico Git contínuo (61+ commits, PRs #53–#62); pipeline com ktlint, detekt e testes em `.github/workflows/ci.yml` | E1.9, E1.10, E5.2 (parcial — README a completar em E5.2) |
| R14 | Pacote instalável gerado e testado em dispositivo físico | **Parcial** | Workflow de release assinado implementado: `.github/workflows/release-apk.yml` (`.aab` via `bundleRelease` com keystore em secrets, disparado por tags `v*`; executado com sucesso no run 35599507321 da tag `v0.3.1-ciclo3`), procedimento em `docs/CI-CD.md` e `docs/DISPOSITIVOS.md`. Teste em ≥ 2 dispositivos físicos pendente do marco E5.3 | E3.7 (atendido); E5.3 (pendente) |

## Notas de verificação

- Cada caminho foi conferido diretamente no código da branch `main`
  em 22/09/2026 (grep/leitura dos arquivos citados) — nenhum
  referência foi copiada do roadmap sem checagem.
- **R5/R6 (Parcial):** E3.3 (sincronização bidirecional Room ↔ backend)
  e E3.4 (banner offline) seguem em execução; as linhas serão
  atualizadas para **Atendido** quando os marcos forem mergeados em
  `main`.
- **R9 (Parcial):** E2.7 (Dashboard consolidado) segue em execução;
  busca/filtro (E2.6) já atendidos em `main` (PR #62).
- **R14 (Parcial):** geração do pacote assinado já funcional
  (`v0.3.1-ciclo3`); a validação em 2 dispositivos físicos (E5.3)
  ocorre antes da entrega N2.
- **R13:** `README.md` já atende E1.10; a versão completa de E5.2
  (troubleshooting e links para o relatório técnico) é pré-requisito
  para a entrega N2 e não bloqueia a conformidade.
- Casos de teste funcionais relacionados: `docs/ROTEIRO-TESTES.md`,
  seção 3 (cobertura por requisito).