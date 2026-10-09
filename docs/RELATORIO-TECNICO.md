# BrainOut — Relatório técnico final (N2 / E5.1)

> Documento de fechamento do Projeto Integrador — Análise e
> Desenvolvimento de Sistemas — PUC Goiás — 2026/2.
>
> Insumo direto do item **N2 item 4** do documento norteador
> ([Documentos/Documento Norteador Projeto Integrador ADS 2026-2.pdf](../Documentos/Documento%20Norteador%20Projeto%20Integrador%20ADS%2026-2.pdf)).
> Consolida o mapeamento R1–R14 → componente, a cobertura de testes, o
> pipeline de CI/CD, instruções de instalação e credenciais por perfil.
>
> Versão PDF renderizada a partir deste Markdown disponível em
> [`docs/RELATORIO-TECNICO.pdf`](./RELATORIO-TECNICO.pdf) (26 páginas,
> A4; gerado via Chromium headless em 2026-09-25 a partir deste
> Markdown; título, tabelas, código e listas preservados).
>
> > ⚠️ **PDF superado — o Markdown é a fonte.** O arquivo
> > `RELATORIO-TECNICO.pdf` foi gerado em 2026-09-25 (commit `405cfe0`,
> > assunto *"docs(n2): inclui PDF do RELATORIO-TECNICO (26p A4)"*) e
> > **não** foi regenerado desde então. Ele é **anterior** a todas as
> > correções de outubro de 2026 (auditoria DEF-08..DEF-16, PR #143,
> > identidade Neo e a revisão de R14/DOZE deste documento) e por isso
> > **não reflete o estado atual do projeto**. A contagem de 26 páginas
> > foi reconferida contra o arquivo em disco em 2026-10-09
> > (`/Count 26` no nó raiz da árvore de páginas, 26 objetos
> > `/Type /Page`) e **bate com o arquivo real**. Para a versão
> > vigente, leia **este Markdown**; o PDF deve ser tratado como
> > histórico.
>
> Marco do roadmap: **E5.1** (`docs/ROADMAP.md`).

---

## 1. Capa

| Campo | Valor |
|-------|-------|
| **Instituição** | Pontifícia Universidade Católica de Goiás (PUC Goiás) |
| **Curso** | Análise e Desenvolvimento de Sistemas (ADS) |
| **Semestre** | 2026/2 |
| **Disciplina** | Projeto Integrador |
| **Aluno** | João Pedro G M Silva |
| **Matrícula** | 20251012000740 |
| **Projeto** | BrainOut — gerenciador de projetos e tarefas para uso individual em Android nativo |
| **Orientador** | _a definir pelo docente_ |
| **Repositório** | <https://github.com/joao-pedro-gms/BrainOut> |
| **Documento norteador** | `Documentos/Documento Norteador Projeto Integrador ADS 2026-2.pdf` |

---

## 2. Sumário

1. Capa
2. Sumário
3. Visão geral do produto
4. Mapeamento R1–R14 → componente
5. Arquitetura
6. Persistência
7. Sincronização
8. Notificações
9. Integração externa
10. Segurança
11. Acessibilidade
12. Testes e cobertura
13. CI/CD
14. Instalação e execução
15. Credenciais por perfil
16. Limitações conhecidas
17. Referências
18. Anexo: árvore de módulos

---

## 3. Visão geral do produto

### 3.1 Propósito

BrainOut é um aplicativo **Android nativo** para gestão individual de
projetos e tarefas. A proposta é entregar uma ferramenta enxuta para
estudantes universitários que precisam organizar trabalhos em grupo e
prazos pessoais sem a sobrecarga cognitiva de suítes corporativas de
gestão. O produto foi construído como projeto integrador da disciplina
de Projeto Integrador (ADS, PUC Goiás, 2026/2) e atende aos requisitos
**R1–R14** do documento norteador, com **R14 em estado Parcial** — o
pacote instalável assinado é gerado e verificado, mas o teste em ≥ 2
dispositivos físicos (E5.3) ainda não foi executado (ver §4 e §16.5).

### 3.2 Público-alvo

Estudantes de cursos tecnológicos (perfil ADS e correlatos) que:

- mantêm **trabalhos da faculdade** como projetos atômicos (cada
  disciplina ou trabalho em grupo é um `Project`);
- precisam **acompanhar tarefas** com prazo, prioridade, status e
  responsável dentro do projeto;
- operam em dispositivos Android (API 24+), frequentemente **offline**
  durante o deslocamento ou em sala sem Wi-Fi;
- valorizam acessibilidade (alvos de toque ≥ 48dp, contraste WCAG AA,
  suporte a TalkBack) e fluxos curtos em vez de formulários longos.

### 3.3 Escopo do MVP

A entrega N2 cobre o conjunto mínimo exigido pelo item 4 do documento
norteador e pelos marcos E1.1–E5.5 do `docs/ROADMAP.md`:

- **Autenticação local** com dois perfis de permissão (`Owner` /
  `Member`) — `CreateUserUseCase` +
  `AuthenticateUserUseCase` em `:core:domain`.
- **CRUD completo** das duas entidades centrais (`Project` e `Task`),
  com `Tag` como entidade auxiliar — `Create*`/`Update*`/`Delete*`
  use cases e `TaskRepository.completeAndCascade`/`reopenAndCascade`
  para RN03.
- **3 regras de negócio** (RN01 limite por projeto, RN02 alteração de
  prioridade proibida em `DONE`, RN03 conclusão em cascata).
- **Persistência local** com Room (esquema v4, migrações
  `MIGRATION_1_2`, `MIGRATION_2_3`, `MIGRATION_3_4`).
- **Persistência remota** com FastAPI próprio (decisão E3.1), cliente
  HTTP por flavor (`dev` → `http://10.0.2.2:8000/`, `prod` →
  `https://TBD/`).
- **Sincronização offline-first** com fila `pending_ops`, `SyncWorker`
  periódico (15 min) e reconciliação automática no retorno de rede
  (`SyncConnectivityWatcher`).
- **Recurso nativo**: notificações locais de prazo via WorkManager
  (`WorkManagerDeadlineScheduler`, `DeadlineWorker`,
  `CompleteTaskWorker`, `DeadlineReceiver`), sem
  `SCHEDULE_EXACT_ALARM`.
- **Integração externa**: BrasilAPI de feriados nacionais
  (`HolidayRepository` + `CheckDeadlineUseCase`).
- **Listagens com filtro, busca e ordenação** persistidas em
  DataStore Preferences, mais visão consolidada (Dashboard).
- **Acessibilidade WCAG AA** (contraste, 48dp, contentDescription,
  TalkBack), tema claro/escuro, localização pt/en.
- **CI/CD** com ktlint + detekt + testes + Kover ≥ 60% e pipeline de
  release assinado via tags `v*` (4 secrets `BRAINOUT_*`).

### 3.4 Fora do escopo desta entrega

- Sincronização em mais de um backend (a decisão E3.1 fechou em
  backend próprio; Supabase/Firebase foram descartados).
- Notificações push remotas via Firebase Cloud Messaging (apenas
  lembretes locais via `NotificationCompat`).
- Recursos colaborativos em tempo real entre dois `Owner` no mesmo
  projeto (a coluna `assignee_id` existe, mas o backend stub não
  implementa fan-out para múltiplos usuários; a sincronização é
  per-device).
- Conta de apresentação pré-criada (não há User seed no banco local —
  contas de demonstração devem ser criadas via tela de Cadastro).

---

## 4. Mapeamento R1–R14 → componente

A tabela a seguir consolida o Apêndice C
([docs/APENDICE-C-CONFORMIDADE.md](APENDICE-C-CONFORMIDADE.md)) com a
atualização de **R5, R6, R9 e R14** após a entrega dos marcos E3.3,
E3.4 e E2.7 (todos marcados `[x]` no
[docs/ROADMAP.md](ROADMAP.md)). Caminhos e símbolos são os arquivos
efetivamente presentes em `main` na data de fechamento do E5.1,
reconferidos em 09/10/2026 contra o commit `54f1d79`. Treze requisitos
estão **Atendidos** e **R14 está Parcial**: o pacote assinado é gerado e
verificado, mas o teste em ≥ 2 dispositivos físicos (E5.3) não foi
executado.

| Nº | Requisito | Status | Onde é verificável | Marco |
|----|-----------|--------|--------------------|-------|
| R1 | Mínimo de 6 telas funcionais com navegação estruturada | **Atendido** | Rotas em `app/src/main/kotlin/pucgo/joaopedrogmsilva/brainout/navigation/BrainOutRoutes.kt` (`Splash`, `Login`, `Register`, `Home`, `Settings`, `ProjectDetailPattern`) e `BrainOutNavHost.kt`; telas em `feature/auth/src/main/kotlin/.../ui/{splash,login,register}/`, `feature/projects/src/main/kotlin/.../ui/{home,projectdetail}/`, `feature/settings/src/main/kotlin/.../ui/SettingsScreen.kt`, `feature/tasks/src/main/kotlin/.../ui/TasksScreen.kt`, `feature/tasks/src/main/kotlin/.../ui/dashboard/DashboardScreen.kt` | E1.3, E1.6, E2.1, E2.2, E2.7 |
| R2 | Autenticação com 2 perfis de permissão distintos | **Atendido** | `core/domain/src/main/kotlin/.../core/domain/model/UserRole.kt` (enum `OWNER`/`MEMBER` com `Set<Permission>`); `Permission.kt`; `usecase/CanPerformActionUseCase.kt`; matriz coberta por `core/domain/src/test/.../model/PermissionMatrixTest.kt` e `usecase/CanPerformActionUseCaseTest.kt`; fluxo em `feature/auth/.../ui/login/LoginScreen.kt` e `ui/register/RegisterScreen.kt`; hashing via `core/data/.../security/PasswordHasherImpl.kt` + `PepperProvider.kt` | E1.6, E1.7, E1.8 |
| R3 | Manutenção completa de dados sobre 2 entidades | **Atendido** | `Project` e `Task` em `core/domain/.../model/`; use cases `CreateProjectUseCase`, `UpdateProjectUseCase`, `DeleteProjectUseCase`, `CreateTaskUseCase`, `UpdateTaskUseCase`, `DeleteTaskUseCase`, `ChangeTaskStatusUseCase` em `core/domain/.../usecase/`; DAOs em `core/data/.../local/dao/ProjectDao.kt` e `TaskDao.kt`; UI em `feature/projects/.../ui/` (Home + ProjectDetail) e `feature/tasks/.../ui/TasksScreen.kt` | E2.1, E2.2 |
| R4 | Mínimo de 3 regras de negócio não triviais | **Atendido** | **RN01** em `core/domain/.../usecase/CreateTaskUseCase.kt` (`MAX_ACTIVE_TASKS_PER_PROJECT`); **RN02** em `core/domain/.../model/Task.kt` (`changePriority` lança `TaskPriorityChangeForbiddenException`, definida em `core/domain/.../error/BusinessRuleException.kt`); **RN03** em `core/data/.../repository/TaskRepositoryImpl.kt` (`completeAndCascade`/`reopenAndCascade` em transação `@Transaction`). Testes: `TaskPriorityRulesTest`, `CreateTaskUseCaseTest`, `ProjectCompletionTest` em `core/domain/src/test/.../` | E2.3, E2.4, E2.5 |
| R5 | Persistência local com tratamento de ausência de conectividade | **Atendido** | Persistência local em `core/data/.../local/BrainOutDatabase.kt` (Room, versão 4), DAOs em `core/data/.../local/dao/`, migrações `MIGRATION_1_2`/`MIGRATION_2_3`/`MIGRATION_3_4` em `Migrations.kt`; testes Robolectric em `core/data/src/test/.../local/dao/UserDaoTest.kt`, `ProjectTaskCascadeTest.kt`, `ProjectTaskTagDaoTest.kt`, `PendingOpDaoTest.kt`. Tratamento de conectividade (banner offline + fila visível): `core/data/.../sync/ConnectivityObserver.kt` (`AndroidConnectivityObserver`), `app/src/main/.../sync/SyncConnectivityWatcher.kt`, banner em `feature/projects/.../ui/home/HomeScreen.kt` e `ui/projectdetail/ProjectDetailScreen.kt`. Reconciliação automática em `SyncConnectivityWatcher.start()` + `SyncScheduler.requestImmediateSync()` | E1.5, E1.8, E3.3, E3.4 |
| R6 | Persistência remota com sincronização de dados | **Atendido** | Cliente HTTP Retrofit por flavor: `core/data/.../remote/BrainOutApi.kt`, `RemoteDataSource.kt`; URL base via `BuildConfig.BASE_URL` injetada por flavor `dev`/`prod` em `core/data/build.gradle.kts`; stub FastAPI em `backend-stub/server.py` (contrato `/v1/*` cliente-supplied UUID, PUT upsert idempotente, DELETE 204 idempotente); DTOs em `RemoteDtos.kt` e `HolidayDto.kt`. Sincronização bidirecional Room ↔ backend: `core/data/.../sync/BrainOutSyncDispatcher.kt` (política `SyncOutcome`), `app/src/main/.../sync/SyncWorker.kt` (drena `pending_ops`), `SyncScheduler.kt` (periódico 15 min + OneTime KEEP). Testes: `MockWebServer` (`core/data/src/test/.../remote/BrainOutApiTest.kt`, `HolidayRepositoryTest.kt`), `PendingOpDaoTest.kt` (10 casos), `BrainOutSyncDispatcherTest.kt` (9 casos), `SyncWorkerTest.kt` (8 casos Robolectric + MockWebServer), `ConnectivityObserverTest.kt` | E3.1, E3.2, E3.3, E3.4 |
| R7 | Consumo de serviço ou interface de programação externa | **Atendido** | BrasilAPI (feriados nacionais): `core/data/.../remote/HolidayApi.kt` (`@GET("api/feriados/v1/{year}")`), `HolidayDto.kt`, `HolidayRemoteDataSource.kt` (cache `Map<Int, List<Holiday>>` + mutex), `core/data/.../repository/HolidayRepositoryImpl.kt`, `core/domain/.../model/Holiday.kt`, `core/domain/.../repository/HolidayRepository.kt`, `usecase/CheckDeadlineUseCase.kt`; URL via `BuildConfig.HOLIDAYS_BASE_URL` injetada por flavor. Testes com MockWebServer em `core/data/src/test/.../remote/HolidayRepositoryTest.kt` (3 cenários) e `usecase/CheckDeadlineUseCaseTest.kt` (5 cenários). Componente Compose `DeadlineField` em `feature/projects/src/main/kotlin/.../feature/projects/ui/projectdetail/DeadlineField.kt` | E3.5 |
| R8 | Uso de recurso nativo do dispositivo | **Atendido** | Notificações locais de prazo: `app/src/main/.../notifications/DeadlineWorker.kt` (`@HiltWorker`), `DeadlineReceiver.kt` (`BroadcastReceiver`, não exportado), `CompleteTaskWorker.kt`, `WorkManagerDeadlineScheduler.kt`, `NotificationPermissionStore.kt`. Permissão runtime `POST_NOTIFICATIONS` (Android 13+) pedida em `MainActivity`; canal `brainout_deadlines` (importance HIGH) criado por `WorkManagerDeadlineScheduler.ensureChannel(this)` em `BrainOutApplication.onCreate`. `DeadlineNotificationScheduler` no `:core:domain` (porta) e `WorkManagerDeadlineScheduler` no `:app` (implementação) preservam R12. Testes: `WorkManagerDeadlineSchedulerTest.kt`, `CompleteTaskWorkerTest.kt`, `DeadlineNotificationSchedulerContractTest.kt` | E3.6 |
| R9 | Listagens com filtro ou busca e visão consolidada de dados | **Atendido** | Busca/filtro/ordenação: `core/data/.../local/dao/ProjectDao.kt` (`searchProjects`), `core/data/.../preferences/ListingPreferencesRepositoryImpl.kt` (DataStore Preferences, namespace por `userId`), `feature/projects/.../ui/home/HomeScreen.kt` + `viewmodel/HomeViewModel.kt` (E2.6 — `combine(listingPrefs, debouncedSearchInput, tags, projectFilter)` + `Flow.debounce(300ms)`). Visão consolidada: `feature/tasks/.../ui/dashboard/DashboardScreen.kt` (Compose Canvas puro para histograma) + `DashboardViewModel.kt` combinando `observeCountByPriority` (GROUP BY) e `observeCompletionStats` (janela segunda 00:00 UTC) | E2.6, E2.7 |
| R10 | Tratamento de erros e indicação de estados de interface | **Atendido** | `feature/projects/.../ui/home/HomeViewModel.kt` (`errorMessage`, token de retry via `MutableStateFlow<Int>` + `flatMapLatest`), `ProjectDetailViewModel.kt`, `feature/tasks/.../ui/TasksViewModel.kt` e `DashboardViewModel.kt` — `.catch` nos Flows do Room (com `CancellationException` re-lançada); empty states (`HomeNoMatchesState`), loaders e banners de erro com prioridade explícita de renderização; validação inline em `OutlinedTextField` com `supportingText`; mensagens localizadas em `app/src/main/res/values/strings.xml` + `values-en/strings.xml` e nos módulos `feature/*` | E1.6, E2.8, E3.4 |
| R11 | Usabilidade e acessibilidade conforme diretrizes da plataforma | **Atendido** | Tokens de tema em `core/ui/.../theme/` (`Color.kt`, `Theme.kt`); contraste WCAG AA coberto por `core/ui/src/test/.../theme/ContrastRatioTest.kt` (24 testes: 22 texto ≥ 4.5:1 + 2 gráfico ≥ 3.0:1, ambos temas claro/escuro); áreas de toque ≥ 48dp via `Modifier.heightIn(min = 48.dp)` em todos os `AssistChip`/`FilterChip` interativos de `:feature:projects` e `:feature:tasks`; `contentDescription` em `IconButton(MoreVert)` da `ProjectDetailScreen` e demais ícones funcionais; `null` explícito em ícones decorativos; ordenação TalkBack documentada em `docs/ACESSIBILIDADE.md`; tema claro/escuro com `Theme.kt` + E4.5 | E4.4, E4.5 |
| R12 | Organização do código em camadas, sem credenciais versionadas | **Atendido** | Camadas: `:core:domain` (Kotlin JVM puro, sem Android), `:core:data` (Room/Retrofit/DataStore + `DataModule.kt`), `:core:ui`, `:feature:{auth,projects,tasks,settings}` — raiz `pucgo.joaopedrogmsilva.brainout` (E1.1/E1.2). Credenciais: `local.properties` e `keystore.properties` fora do versionamento (`.gitignore`); keystore lido de variáveis de ambiente (`BRAINOUT_KEYSTORE_PATH`/`PASSWORD`, `BRAINOUT_KEY_ALIAS`/`PASSWORD`) tanto local quanto em `release-apk.yml`; senhas persistidas com hash + pepper (`PasswordHasherImpl` + `PepperProvider` com `EncryptedSharedPreferences` + `MasterKey`); cobertura mínima 60% por Kover em `:core:domain` (87,9%) e `:core:data` (67,3%) | E1.1, E3.2, E4.7 |
| R13 | Repositório Git com histórico distribuído e README | **Atendido** | `README.md` (pré-requisitos, build, execução, troubleshooting, links para o documento norteador e para o relatório técnico); `docs/CONTRIBUTING.md`; histórico Git contínuo (mais de 60 commits, PRs #23–#66); pipeline com ktlint + detekt + testes + Kover em `.github/workflows/ci.yml`; 3 jobs sequenciais (`static-analysis`, `unit-tests`, `backend-integration`); `codeql.yml` semanal; `dependabot.yml` para actions/Gradle | E1.9, E1.10, E5.2 |
| R14 | Pacote instalável gerado e testado em dispositivo físico | **Parcial** | **Geração do pacote: entregue (E3.7).** Workflow de release assinado `.github/workflows/release-apk.yml` (gera `.aab` via `:app:bundleRelease`, valida os 4 secrets `BRAINOUT_*`, decodifica o keystore em `$RUNNER_TEMP`, roda `jarsigner -verify`, publica artifact `brainout-release-aab-<tag>` com retenção de 30 dias). Tag `v0.3.1-ciclo3` (CI run 35599507321) produziu artefato assinado de 4,2 MB documentado em `docs/qa/run-log-t_333ab600.md`. **Teste em ≥ 2 dispositivos físicos: não entregue (E5.3).** `docs/DISPOSITIVOS.md` ainda tem campos `⏳`: o secundário emprestado tem Modelo, Fabricante, versão do Android, API level e Tela/densidade **todos indefinidos** (`docs/DISPOSITIVOS.md:37-47`) e os checklists 3.1–3.8 (§3) e 5.1 estão inteiros em branco. **Nenhuma instalação foi executada ou registrada em dispositivo físico**, portanto o requisito não pode ser marcado como atendido. Mesmo veredito de [docs/APENDICE-C-CONFORMIDADE.md:27](APENDICE-C-CONFORMIDADE.md). O `targetSdk` citado no checklist de compatibilidade (`docs/DISPOSITIVOS.md:62-63`) foi corrigido para 37 em 2026-10-09. | E3.7 (atendido); E5.3 (pendente) |

> **Notas de verificação.**
> - Cada caminho foi conferido por `read`/`grep` direto no código de
>   `main` em 24/09/2026 (referência: auditoria
>   `docs/AUDITORIA-2026-09-24.md`, commit `5ce89d9`).
> - **R5/R6 (Parcial → Atendido):** E3.3 (`SyncWorker` + `SyncScheduler`)
>   e E3.4 (`SyncConnectivityWatcher` + banner offline + reconciliação
>   automática) estão mergeados em `main` (PR #66, squash `9dd8696`).
> - **R9 (Parcial → Atendido):** E2.7 (`DashboardScreen` +
>   `DashboardViewModel`) está mergeado em `main` (PR `feat/dashboard-e27`).
> - **R14 (Parcial → Parcial — veredito corrigido em 2026-10-09):** a
>   geração do pacote assinado está entregue e é real (`v0.3.1-ciclo3`,
>   run `35599507321`, artefato de 4,2 MB em
>   `docs/qa/run-log-t_333ab600.md`), mas o **teste em ≥ 2 dispositivos
>   físicos (E5.3) não foi executado**. Conferido em 2026-10-09 contra
>   `docs/DISPOSITIVOS.md`: a ficha do secundário emprestado está
>   integralmente em `⏳` (`docs/DISPOSITIVOS.md:37-47`) e os checklists
>   3.1–3.8 e 5.1 estão em branco. A versão anterior deste relatório
>   marcava R14 como **Atendido** e ao mesmo tempo afirmava, na própria
>   nota, que o teste físico ainda não tinha sido executado — contradição
>   interna corrigida nesta revisão. O veredito **Parcial** segue o
>   apêndice (`docs/APENDICE-C-CONFORMIDADE.md:27`).
> - **SDK (2026-10-09):** `compileSdk = 37` (`app/build.gradle.kts:54`) e
>   `targetSdk = 37` (`:59`), `minSdk = 24` (`:58`). As tabelas das §5.3 e
>   §14.1 citavam 35/35/24 e foram corrigidas.
> - **Números derivados de build (cobertura de linhas e contagem de
>   testes):** **não verificados nesta revisão e deliberadamente
>   mantidos como estão** (§12.2.1 e §12.3). Existem três versões
>   divergentes do mesmo número — este documento, o apêndice e o
>   `kover report.xml` — e nenhuma delas é confiável sem reexecutar o
>   build depois que a cadeia de correções (DEF-08..DEF-16, PR #143) aterrissar.
>   O refresh é feito em revisão à parte, após a cadeia. Enquanto isso,
>   **nenhum número de cobertura ou de testes deve ser citado como
>   medido atual**.

---

## 5. Arquitetura

A descrição arquitetural completa está em
[docs/ARQUITETURA.md](ARQUITETURA.md). Esta seção resume a estrutura
e referencia as seções do documento de arquitetura.

### 5.1 Decisão de plataforma

Aplicativo **Android nativo** em **Kotlin** com **Jetpack Compose** para
a camada de interface. Justificativas em `docs/ARQUITETURA.md` §1:
mercado, adequação ao domínio (uso de recursos nativos: notificações,
persistência), curva de aprendizado (Kotlin é exigido pelo curso;
Compose é recomendado pelo item 4 do documento norteador) e custo
ferramental zero. Seção 9 do mesmo documento formaliza a **decisão
E3.1** pelo backend **próprio FastAPI** sobre Firebase e Supabase
(controle de dados, custo zero na VPN Tailscale, contrato REST já
exercitado pelo CI via stub em `backend-stub/`).

### 5.2 Camadas e módulos

Estrutura multi-módulo Gradle (catálogo único em
`gradle/libs.versions.toml`):

```
BrainOut/
├── app/                  → entry point (Application + MainActivity + NavHost + workers + receivers)
├── core/
│   ├── domain/           → entidades imutáveis, regras, use cases (Kotlin JVM puro)
│   ├── data/             → Room + DataStore + Retrofit + repositórios + DI
│   │   └── remote/       → DTOs, BrainOutApi, RemoteDataSource, HolidayApi/Dto/DS
│   └── ui/               → tokens de tema, componentes Compose reutilizáveis
├── feature/
│   ├── auth/             → telas de login/cadastro + AuthViewModel
│   ├── projects/         → Home (lista/busca/filtro), ProjectDetail (tarefas), DeadlineField
│   ├── tasks/            → TasksScreen (CRUD) + DashboardScreen (visão consolidada E2.7)
│   └── settings/         → SettingsScreen + tema + sessão
└── backend-stub/         → FastAPI mínimo usado pelo CI (R6)
```

Dependências só apontam para baixo:

```
app → feature → core/{ui,data} → core/domain
```

`:core:domain` não depende de nenhum outro módulo, garantindo
portabilidade e testabilidade (regra R12). Detalhes adicionais em
`docs/ARQUITETURA.md` §2 e §2.1 (camada remote).

### 5.3 Stack técnica

Catálogo único em `gradle/libs.versions.toml`:

| Camada                   | Tecnologia (versão) |
|--------------------------|---------------------|
| Linguagem                | Kotlin 2.3.20 (JDK 17 bytecode) |
| Build                    | Gradle 9.7.1, AGP 9.4.1, KSP 2.3.12 |
| UI                       | Jetpack Compose (Compose BOM 2024.10.01) + Material 3 |
| Navegação                | `androidx.navigation:navigation-compose` 2.8.4 |
| Estado                   | `ViewModel` + `StateFlow` + Hilt 2.60.1 |
| Injeção de dependência   | Hilt + `hilt-work` 1.2.0 (workers) |
| Persistência local       | Room 2.8.4 (KSP) + DataStore Preferences 1.1.1 |
| Sincronização            | WorkManager 2.9.1 + Retrofit 2.11.0 + OkHttp 4.12.0 + kotlinx-serialization 1.7.3 |
| Notificações             | NotificationCompat + WorkManager (sem AlarmManager) |
| Criptografia             | `androidx.security:security-crypto` 1.1.0 + `desugar_jdk_libs` 2.1.5 |
| Testes                   | JUnit 4.13.2, Robolectric 4.15.1, Compose UI Test, MockWebServer 4.12.0, Turbine 1.2.0, MockK 1.13.13, Truth 1.4.4 |
| Qualidade                | ktlint 14.2.0, detekt 1.23.7, Android Lint, Kover 0.9.9 (E4.7) |
| `compileSdk` / `targetSdk` / `minSdk` | 37 / 37 / 24 |

### 5.4 Fluxo de dados

```
┌──────────────────────────────────────────────────────────────┐
│ feature/* (Compose Screen ↔ ViewModel StateFlow)             │
└──────────────┬───────────────────────────────────────────────┘
               │ portas (Repository interface) — :core:domain
┌──────────────▼───────────────────────────────────────────────┐
│ :core:data — Repository impl                                  │
│  ├── local/       Room (DAOs + migrations + PendingOpDao)     │
│  ├── remote/      BrainOutApi (Retrofit) ← RemoteDataSource  │
│  └── preferences/ DataStore<Preferences>                     │
└──────────────┬───────────────────────────────────────────────┘
               │ HTTP/JSON (snake_case /v1/*, cliente-supplied UUID)
┌──────────────▼───────────────────────────────────────────────┐
│ Backend FastAPI (backend-stub/ no CI; VM Tailscale em prod)  │
└──────────────────────────────────────────────────────────────┘
```

Camadas adicionais:

- **Notificações (R8)**: `DeadlineNotificationScheduler` (porta em
  `:core:domain`) → `WorkManagerDeadlineScheduler` (impl em `:app`)
  → `DeadlineWorker` → `NotificationManagerCompat` no canal
  `brainout_deadlines`. Ação "Concluir" → `DeadlineReceiver` →
  `CompleteTaskWorker` → `ChangeTaskStatusUseCase(taskId, DONE)`
  (caminho via WorkManager, requisito do critério E3.6).
- **Sincronização (R5/R6)**: `enqueueInTx(op, write)` em
  `PendingOpDao` (escrita dupla atômica) → `pending_ops` →
  `SyncWorker` (periódico 15 min + OneTime KEEP) →
  `BrainOutSyncDispatcher` → `RemoteDataSource` → backend.
- **Conectividade (E3.4)**: `SyncConnectivityWatcher` (callback
  `NetworkCallback` em `BrainOutApplication.onCreate`) enfileira
  `SyncScheduler.requestImmediateSync()` em `onAvailable`.

Decisões registradas (AD) em `docs/ARQUITETURA.md` §8 e §9.2:
**AD-1** multi-módulo Gradle; **AD-2** stack; **AD-3** backend próprio
FastAPI (21/09/2026); **AD-4** last-writer-wins + fila offline;
**AD-5** criptografia com `security-crypto`; **AD-6** WorkManager
para lembretes, sem `SCHEDULE_EXACT_ALARM` (21/09/2026).

---

## 6. Persistência

### 6.1 Schema Room v4

Banco: `BrainOutDatabase` em
`core/data/src/main/kotlin/.../core/data/local/BrainOutDatabase.kt`,
`@Database(version = 4, exportSchema = true)`. Schemas versionados em
`core/data/schemas/pucgo.joaopedrogmsilva.brainout.core.data.local.BrainOutDatabase/{1..4}.json`.

| Tabela | Chave | Colunas principais | Origem |
|--------|-------|--------------------|--------|
| `users` | `id` (UUID) | `email` UNIQUE, `password_hash`, `role` (OWNER/MEMBER), `name`, `created_at` | E1.4/E1.5 |
| `projects` | `id` (UUID) | `owner_id`, `name`, `description`, `created_at`, `is_completed` | E2.1 (`MIGRATION_1_2`) |
| `tasks` | `id` (UUID) | `project_id` (FK CASCADE), `title`, `priority_code` 0..4, `status`, `assignee_id`, `due_date`, `created_at`, `completed_at` | E2.1 + E2.5 (`completed_at` em `MIGRATION_2_3`) |
| `tags` | `id` (UUID) | `owner_id`, `name`, `color` (`UNIQUE(owner_id, name)`) | E2.1 |
| `project_tags` | `(project_id, tag_id)` | FK CASCADE em ambos | E2.1 |
| `pending_ops` | `id` AUTOINCREMENT | `entity_type`, `entity_id`, `op_type`, `payload` (JSON), `created_at`, `attempts` | E3.3 (`MIGRATION_3_4`) |

Índices auxiliares: `idx_projects_owner_id`, `idx_tasks_project_id`,
`idx_tasks_status`, `idx_tags_owner_name` (UNIQUE),
`idx_project_tags_tag_id`, `idx_pending_ops_created_at`.

### 6.2 Migrações

Todas as três migrações são **não destrutivas** (nenhum `DROP`/
`DELETE`/recriação de tabela) e escritas à mão em
`core/data/src/main/kotlin/.../core/data/local/Migrations.kt`:

- **`MIGRATION_1_2`** (E2.1): `CREATE TABLE IF NOT EXISTS` para
  `projects`, `tasks`, `tags`, `project_tags` e seus índices. Usuários
  da v1 (apenas tabela `users`) preservados.
- **`MIGRATION_2_3`** (RN03, E2.5): `ALTER TABLE tasks ADD COLUMN
  completed_at INTEGER`. Tarefas legadas ficam com `completed_at IS
  NULL`; o domínio (`Task.init`) aceita esse caso.
- **`MIGRATION_3_4`** (E3.3): `CREATE TABLE IF NOT EXISTS
  pending_ops` + `idx_pending_ops_created_at`. Sem FK para preservar
  ops em entidades removidas em cascata.

Cobertura de teste das migrações em
`core/data/src/androidTest/.../local/MigrationTest.kt` e testes
Robolectric em `core/data/src/test/.../local/dao/`.

### 6.3 Fila de operações (`pending_ops`)

Entidade `PendingOpEntity` em
`core/data/.../local/entity/PendingOpEntity.kt` (enum
`SyncEntityType { PROJECT, TASK, TAG }`, enum `SyncOpType { CREATE,
UPDATE, DELETE }`). `PendingOpDao` em
`core/data/.../local/dao/PendingOpDao.kt` expõe:

- `nextBatch(limit)` — próximo lote em ordem de inserção (preserva a
  ordem global da fila).
- `count()` / `observeCount()` — contagem para a UI exibir
  "X alterações aguardando".
- `insert(op)` / `update(op)` / `deleteById(id)` /
  `deleteForEntity(...)` — escrita.
- `findById(id)` / `markAttempt(opId)` — incremento de tentativas em
  transação atômica (`@Transaction`).
- **`enqueueInTx(op, write)`** — helper de escrita dupla atômica:
  recebe a escrita local como lambda; se qualquer uma das duas
  falhar, Room reverte ambas. É o método invocado pelos repositórios
  para garantir consistência.

### 6.4 DataStore Preferences

Dois arquivos lógicos sob o mesmo `DataStore<Preferences>` singleton
(`auth_prefs.preferences_pb`, delegate
`Context.authDataStore` em
`core/data/.../session/SessionStore.kt`):

- **Sessão ativa**: chaves `user_id` e `notification_permission_asked`
  (mantidas por `SessionStore.saveUserId/clear` e
  `wasNotificationPermissionAsked/markNotificationPermissionAsked`).
- **Preferências de listagem** (E2.6): chaves namespaced por `userId`
  em `ListingPreferencesRepositoryImpl`
  (`listing_<userId>_search_query`,
  `listing_<userId>_selected_tag_id`,
  `listing_<userId>_sort_order`) — implementa
  `ListingPreferencesRepository` (`observe`, `setSearchQuery`,
  `setSelectedTagId`, `setSortOrder`).
  Enum `SortOrder` (`NameAsc`, `NameDesc`, `CreatedDesc`, `CreatedAsc`)
  com `toStorageKey()` e `fromStorageKey()` (fallback seguro para
  upgrades).

`SessionStore.clear()` apenas remove `user_id` e a flag de permissão
de notificação — preserva `listing_*` por usuário (próximo login
encontra o snapshot anterior). Bind Hilt em
`core/data/.../di/DataModule.kt::provideAuthDataStore`,
`provideSessionStore`, `provideListingPreferencesRepository`.

---

## 7. Sincronização

A sincronização é **offline-first** e last-writer-wins. Toda escrita
local produz uma linha em `pending_ops` na mesma transação Room da
escrita principal — ou a alteração e a fila são aplicadas juntas, ou
nada é aplicado.

### 7.1 Política de ID

O cliente gera **UUID** localmente e envia no `POST`/`PUT`; o
servidor respeita o ID recebido e nunca o substitui (contrato em
`backend-stub/server.py:POST/PUT`, cobertura em
`backend-stub/tests/test_contract.py:16 casos pytest`). Garantias:

- `PUT /v1/projects/{id}` e `PUT /v1/tasks/{id}` são **upserts
  idempotentes**: criam se ausentes, substituem se presentes, com
  `created_at` fixado na primeira inserção e preservado em updates.
- `DELETE` é intencionalmente **idempotente (204)** mesmo quando a
  entidade não existe — replay da fila não trava.
- IDs mal formados (não-UUID) → `400`; divergência body/path no
  `PUT` → `400` (`test_put_project_rejects_id_mismatch`,
  `test_put_project_rejects_non_uuid_path`).

### 7.2 Estratégia dual-write

`PendingOpDao.enqueueInTx(op, write)` em
`core/data/.../local/dao/PendingOpDao.kt` é o helper atômico. Os
repositórios de projeto/tarefa/tag o invocam para garantir que toda
escrita local produza uma `PendingOpEntity` correspondente. JSON
serializado em `SYNC_JSON` (`Json { encodeDefaults = true;
ignoreUnknownKeys = true }` em `PendingOpEntity.kt`) com payloads
específicos:

```kotlin
@Serializable data class ProjectSyncPayload(id, name, description)
@Serializable data class TaskSyncPayload(id, project_id, title, priority, done)
@Serializable data class TagSyncPayload(id, name, color)
```

`BrainOutSyncDispatcher` em
`core/data/.../sync/BrainOutSyncDispatcher.kt` traduz o resultado
HTTP em `SyncOutcome` (`Success`, `Retriable(reason)`,
`Permanent(httpCode, reason)`):

- **2xx** → `Success` (op removida da fila).
- **`IOException` / HTTP 5xx / timeout** → `Retriable` (op permanece
  na fila, `attempts++`, worker devolve `Result.retry()`).
- **HTTP 4xx / payload inválido** → `Permanent` (op descartada — o
  contrato foi violado; reenviar não mudaria o resultado; a alteração
  local permanece).

### 7.3 SyncWorker (drenagem)

`app/src/main/.../sync/SyncWorker.kt` é um `CoroutineWorker`
(`@HiltWorker`) que drena a fila em lotes de `BATCH_SIZE = 50` por
`PendingOpDao.nextBatch(BATCH_SIZE)`:

- Sucesso (`SyncOutcome.Success`) → `pendingOpDao.deleteById(op.id)`.
- Permanente (`SyncOutcome.Permanent`) → `pendingOpDao.deleteById(op.id)`
  com log estruturado.
- Retriable (`SyncOutcome.Retriable`) → `pendingOpDao.markAttempt(op.id)`
  e `return Result.retry()` — interrompe a drenagem preservando a
  ordem global (op UPDATE chegando antes do CREATE correspondente
  violaria a FK `project_id` no stub).

### 7.4 Agendamento

`app/src/main/.../sync/SyncScheduler.kt`:

- **Periódico**: `enqueueUniquePeriodicWork(SyncWorker.UNIQUE_PERIODIC_NAME,
  KEEP, PERIOD_MINUTES = 15)` com `NetworkType.CONNECTED` e
  `BackoffPolicy.EXPONENTIAL, BACKOFF_SECONDS = 30`.
- **OneTime** (`requestImmediateSync()`): `enqueueUniqueWork(UNIQUE_ONE_TIME_NAME,
  KEEP, ...)` — disparado por `SyncConnectivityWatcher.onAvailable` e
  após enfileiramentos locais que precisam drenar imediatamente.

`BrainOutApplication.onCreate()`
(`app/src/main/.../BrainOutApplication.kt`) chama
`syncScheduler.ensurePeriodicSync()` e
`syncConnectivityWatcher.start()`.

### 7.5 Reconciliação automática no retorno de rede

`SyncConnectivityWatcher` em
`app/src/main/.../sync/SyncConnectivityWatcher.kt` registra um
`ConnectivityManager.NetworkCallback` (capability
`NET_CAPABILITY_INTERNET`) e, em `onAvailable`, chama
`SyncScheduler.requestImmediateSync()`. A UI observa
`ConnectivityObserver` (`AndroidConnectivityObserver` em
`core/data/.../sync/ConnectivityObserver.kt`) e renderiza o banner
"No connection" + contagem de `pending_ops` (pluralizada pt/en)
quando offline. Detalhes em
`docs/SMOKE-TEST-CRUD.md` §11 (projeto `OfflineP1` aparece em
`GET /v1/projects` após desativar modo avião).

### 7.6 Resolução de conflitos

A estratégia documentada (AD-4) é **last-writer-wins** por timestamp
do cliente. Conflitos são registrados em log local para revisão;
sobreposição silenciosa não ocorre — o usuário vê banner de
"conflito resolvido" apenas se a estratégia exigir confirmação (não
aplicada nesta release). Cobertura de teste do contrato HTTP em
`core/data/src/test/.../remote/BrainOutApiTest.kt` e da fila em
`PendingOpDaoTest.kt` (10 casos) e `SyncWorkerTest.kt` (8 casos
Robolectric + MockWebServer).

---

## 8. Notificações

O recurso nativo (R8) é o **lembrete local de prazo** (marco E3.6):
quando uma `Task` possui `dueDate` futura, o app agenda uma
notificação para disparar **1 hora antes** do prazo (constante
`DeadlineNotificationScheduler.REMINDER_LEAD = Duration.ofHours(1)`),
com as ações "Concluir" e "Dispensar".

### 8.1 Escolha do mecanismo (AD-6)

Duas alternativas foram avaliadas em `docs/ARQUITETURA.md` §10.1:

| Alternativa | Prós | Contras |
|-------------|------|---------|
| `AlarmManager.setExactAndAllowWhileIdle` | Precisão de segundos | Exige `SCHEDULE_EXACT_ALARM`/`USE_EXACT_ALARM` (restrita no Android 13+, revogável, exige intenção do usuário); `setExactAndAllowWhileIdle` não dispara em Doze sem permissão especial |
| **WorkManager** (`OneTimeWorkRequest` + `setInitialDelay`) | Sem permissão adicional; sobrevive a reboot/process death; integrado com a estratégia de sincronização do E3.3; testável via `work-testing` | Janela de tolerância (disparo tipicamente dentro de poucos minutos do horário programado) |

A escolha foi **WorkManager**, pela consistência com E3.3 (fila de
sincronização no mesmo executor), ausência de permissões extras no
Android 13+ e janela aceitável para lembretes acadêmicos. Sem
`SCHEDULE_EXACT_ALARM` no manifest (`app/src/main/AndroidManifest.xml`)
— intencional.

### 8.2 Componentes

| Componente | Caminho | Papel |
|------------|---------|-------|
| `DeadlineNotificationScheduler` (porta) | `core/domain/.../notification/DeadlineNotificationScheduler.kt` | Interface: `schedule(taskId, triggerAt)` / `cancel(taskId)`; `REMINDER_LEAD = 1h` |
| `WorkManagerDeadlineScheduler` (impl) | `app/src/main/.../notifications/WorkManagerDeadlineScheduler.kt` | `OneTimeWorkRequest` por tarefa com `ExistingWorkPolicy.REPLACE`; cria canal `brainout_deadlines` (importance HIGH) em `ensureChannel()`; cancela via `WorkManager.cancelUniqueWork` |
| `DeadlineWorker` | `app/src/main/.../notifications/DeadlineWorker.kt` | `@HiltWorker` que recarrega a `Task` do Room (evita lembrete fantasma se concluída/excluída desde o agendamento), resolve o nome do projeto e publica a notificação com ações "Concluir"/"Dispensar" |
| `DeadlineReceiver` | `app/src/main/.../notifications/DeadlineReceiver.kt` | `BroadcastReceiver` não exportado (`android:exported="false"`); botão "Concluir" enfileira `CompleteTaskWorker` no WorkManager, cancela o trabalho pendente e a notificação; usa `goAsync()` + coroutine |
| `CompleteTaskWorker` | `app/src/main/.../notifications/CompleteTaskWorker.kt` | `@HiltWorker` que executa `ChangeTaskStatusUseCase(taskId, DONE)`; `IOException` → `Result.retry()`; `DomainException` (tarefa inexistente) → `Result.success()` (idempotente) |
| `NotificationPermissionStore` | `app/src/main/.../notifications/NotificationPermissionStore.kt` | Flag "permissão já pedida" em `SessionStore` (DataStore) para não insistir após negativa |

### 8.3 Permissão e canal

- **Permissão `POST_NOTIFICATIONS`** (Android 13+): declarada em
  `app/src/main/AndroidManifest.xml` e pedida via
  `rememberLauncherForActivityResult` em `MainActivity` na primeira
  entrada na Home; negativa registra a flag em
  `NotificationPermissionStore.wasAsked()` para não insistir.
- **Canal `brainout_deadlines`** (importance HIGH): criado por
  `WorkManagerDeadlineScheduler.ensureChannel(context)` invocado em
  `BrainOutApplication.onCreate()`. Nome e descrição em
  `app/src/main/res/values/strings.xml` + `values-en/strings.xml`
  (8 chaves `deadline_*` traduzidas em ambos).
- `BrainOutApplication` também implementa `Configuration.Provider`
  para que os `@HiltWorker` (`DeadlineWorker`, `CompleteTaskWorker`,
  `SyncWorker`) recebam dependências via construtor — o
  inicializador default do WorkManager foi desabilitado no manifest
  (`tools:node="remove"` em
  `androidx.work.WorkManagerInitializer`).

### 8.4 Reconciliação de lembretes

A criação, edição (`CreateTaskUseCase`/`UpdateTaskUseCase`) e
mudança de status (`ChangeTaskStatusUseCase`) reconciliam o lembrete
no domínio: tarefa ativa com prazo futuro → agenda para
`dueDate − 1h`; tarefa `DONE`, sem prazo ou prazo no passado →
cancela. Exclusão (`DeleteTaskUseCase`) também cancela. O
reconciliar vive em `:core:domain`; o Android fica isolado na
implementação da porta (preservando R12).

---

## 9. Integração externa — BrasilAPI (E3.5)

O serviço externo consumido (R7) é a **BrasilAPI**
(`https://brasilapi.com.br/api/feriados/v1/{year}`), uma API pública
sem autenticação e sem limite duro de uso. Cada ano é uma chamada
independente; o domínio só precisa de `{date, name, type}`.

### 9.1 Componentes

| Componente | Caminho | Papel |
|------------|---------|-------|
| `HolidayApi` | `core/data/.../remote/HolidayApi.kt` | Interface Retrofit com `@GET("api/feriados/v1/{year}")` |
| `HolidayDto` | `core/data/.../remote/HolidayDto.kt` | DTO `date/name/type` (snake_case via kotlinx-serialization) |
| `HolidayRemoteDataSource` | `core/data/.../remote/HolidayRemoteDataSource.kt` | Cache em memória `Map<Int, List<Holiday>>` por ano + mutex para chamadas concorrentes; `404` → lista vazia; demais erros propagam |
| `HolidayRepositoryImpl` | `core/data/.../repository/HolidayRepositoryImpl.kt` | Bind para a porta `HolidayRepository` |
| `HolidayRepository` | `core/domain/.../repository/HolidayRepository.kt` | Porta de domínio: `suspend fun getHolidays(year)` |
| `Holiday` | `core/domain/.../model/Holiday.kt` | Modelo de domínio imutável (`LocalDate date`, `String name`, `String type`) |
| `CheckDeadlineUseCase` | `core/domain/.../usecase/CheckDeadlineUseCase.kt` | Janela `[prazo − 7d, prazo]`, inclusive; cruza dezembro/janeiro buscando os dois anos; filtra `type == "national"`; retorna `DeadlineInfo(isBusinessDay, nextHoliday)` |
| `DeadlineField` | `feature/projects/.../ui/projectdetail/DeadlineField.kt` | Componente Compose: botão de prazo, picker Material 3, dica inline de próximo feriado e aviso "dia não útil" |
| `provideHolidayRemoteDataSource` | `core/data/.../di/DataModule.kt` | `baseUrl = BuildConfig.HOLIDAYS_BASE_URL` (injetado por flavor) |

### 9.2 Fluxo de dados

1. Usuário escolhe prazo no `DatePicker` (Material 3).
2. `DeadlineField` dispara `LaunchedEffect(dueDate)` chamando
   `CheckDeadlineUseCase(deadline)` via ViewModel.
3. O caso de uso pede `HolidayRepository.getHolidays(year1[, year2])`.
4. A implementação delega para `HolidayRemoteDataSource.listHolidays`,
   que consulta o cache em memória (mutex); em cache miss, monta o
   `Retrofit` (uma vez por instância) e chama `HolidayApi`.
5. BrasilAPI devolve JSON → `HolidayDto` → `Holiday`.
6. O caso de uso filtra `type == "national"`, calcula `isBusinessDay`
   (não-sábado/domingo e ausente da lista de feriados) e o feriado
   mais próximo dentro da janela de 7 dias anteriores.
7. `DeadlineField` mostra a dica inline "⚠ Próximo feriado: …" ou
   o aviso "este prazo cai em dia não útil".
8. Falha de rede/5xx é capturada pelo `LaunchedEffect` (sem
   `CancellationException`) e renderiza o texto neutro
   "Aviso de feriado indisponível" — o usuário consegue salvar
   normalmente.

### 9.3 Configuração por flavor

`core/data/build.gradle.kts` declara
`buildConfigField("String", "HOLIDAYS_BASE_URL", ...)` para `dev` e
`prod` (default `https://brasilapi.com.br/`). A `DataModule` lê do
`BuildConfig` — não há URL hard-coded no app.

---

## 10. Segurança

### 10.1 HTTPS em produção

- **Flavor `dev`**: cleartext HTTP liberado via
  `android:usesCleartextTraffic="true"` no manifest, com URL
  `http://10.0.2.2:8000/` apontando para o `backend-stub` no host do
  emulador.
- **Flavor `prod`**: placeholder `https://TBD/` (decisão E3.1 —
  backend próprio FastAPI em VM caseira acessível via Tailscale,
  contrato HTTPS obrigatório; ver `docs/ARQUITETURA.md` §9).

### 10.2 Credenciais não versionadas

- `local.properties` e `keystore.properties` ficam fora do controle
  de versão (entradas `local.properties`, `local.properties.*`,
  `keystore.properties`, `*.jks`, `*.keystore`, `*.p12`, `*.pfx`,
  `secrets/`, `*.env` no `.gitignore`).
- O keystore do app é gerado localmente e fornecido ao CI como
  secret (4 secrets `BRAINOUT_*` — ver §13 e
  `docs/CI-CD.md`).
- `local.properties` registrado neste repositório contém apenas o
  `sdk.dir` do desenvolvedor (não-secret) e o override da URL do
  stub (`brainout.baseUrl.dev`); nenhum token, hash ou chave está
  versionado. Comando de verificação:
  ```bash
  git ls-files | grep -E '(\.jks|keystore|local\.properties|secrets/)' || echo "OK: nenhum arquivo sensível rastreado"
  ```

### 10.3 Assinatura de release via variáveis de ambiente

`app/build.gradle.kts` (linhas 19–33, 47–53) lê o keystore de quatro
variáveis: `BRAINOUT_KEYSTORE_PATH`, `BRAINOUT_KEYSTORE_PASSWORD`,
`BRAINOUT_KEY_ALIAS`, `BRAINOUT_KEY_PASSWORD`. Sem as quatro, a flag
`hasReleaseSigning = false` faz o build de release cair no caminho
não-assinado **sem quebrar** o CI comum. O
`.github/workflows/release-apk.yml` valida explicitamente as 4
variáveis antes de prosseguir e roda `jarsigner -verify` no `.aab`
publicado — qualquer falha aborta o upload.

### 10.4 Persistência de senhas

`CreateUserUseCase` em
`core/domain/.../usecase/CreateUserUseCase.kt` aplica hash + salt
antes de persistir. `PasswordHasherImpl` em
`core/data/.../security/PasswordHasherImpl.kt` deriva o pepper do
`PepperProvider` (default com `EncryptedSharedPreferences` +
`MasterKey.AES256_GCM`). Validação: `MIN_PASSWORD_LENGTH = 8`
(caracteres), `User.requireValidEmail` (regex canônica) e
`User.requireValidName` (1..120). Política de erro do login: TF-04
do `docs/ROTEIRO-TESTES.md` afirma que o app retorna "credenciais
inválidas" genérico — não distingue usuário inexistente de senha
errada — para não vazar enumeração de e-mails (privacidade, R2).

### 10.5 Permissões runtime

Justificadas e declaradas em `app/src/main/AndroidManifest.xml`:

| Permissão | Por quê | Quando pedir |
|-----------|---------|--------------|
| `POST_NOTIFICATIONS` | Notificações de prazo (R8 / E3.6) | Android 13+: primeira entrada na Home |
| `INTERNET` | Cliente HTTP Retrofit (R6) | Concedida na instalação (normal) |
| `ACCESS_NETWORK_STATE` | `ConnectivityObserver` (E3.4) + `NetworkType.CONNECTED` no WorkManager | Concedida na instalação (normal) |

`RECEIVE_BOOT_COMPLETED` foi declarado em PR #45 para reagendamento
pós-boot, mas foi removido na revisão E3.6 (WorkManager é persistido
pelo Android após reboot por padrão — não exige permissão adicional).

---

## 11. Acessibilidade

A pauta completa está em
[docs/ACESSIBILIDADE.md](ACESSIBILIDADE.md); este resumo cobre os
pontos exigidos por R11.

### 11.1 Contraste WCAG AA

Padrão adotado: **WCAG 2.1 nível AA**.

- **Texto normal**: ≥ 4.5:1 (critério 1.4.3).
- **Componente gráfico**: ≥ 3.0:1 (critério 1.4.11 — ícones
  funcionais, contornos, divisores).

Cobertura automatizada em
`core/ui/src/test/kotlin/.../core/ui/theme/ContrastRatioTest.kt`
(algoritmo sRGB + luminância relativa conforme W3C WCAG 2.1):

| Categoria | Pares testados | Limiar |
|-----------|----------------|--------|
| Texto (≥ 4.5:1) | 22 pares `on*`/superfície (`onPrimary`, `onSecondary`, `onTertiary`, `onError`, `onBackground`, `onSurface`, `onSurfaceVariant` + containers) | 4.5:1 |
| Gráfico (≥ 3.0:1) | 2 (`outline` em ambos os temas) | 3.0:1 |
| **Total** | **24 testes `@Test`** | — |

Ambos os temas (claro/escuro) são cobertos. Roda em JVM puro (sem
Robolectric) porque os tokens são `Color`/`Long`/`Int`.

### 11.2 Áreas de toque ≥ 48dp

`AssistChip` e `FilterChip` do Material 3 têm ~32dp de altura por
padrão (abaixo de WCAG 2.5.5 — Target Size, AAA). Os chips
interativos foram ajustados com
`Modifier.heightIn(min = 48.dp)`:

- `feature/projects/.../HomeScreen.kt` — `AssistChip` do badge de
  papel, `AssistChip` de tag (`TagChipView`), `FilterChip` da
  seleção de tags (`CreateProjectTagsFlow`).
- `feature/projects/.../ProjectDetailScreen.kt` — `AssistChip` de
  status (`StatusChip`), `AssistChip` de prioridade (`PriorityChip`).
- `feature/tasks/.../TasksScreen.kt` — `AssistChip` de prioridade,
  `AssistChip` de status.

`IconButton` do Material 3 já garante 48dp via
`minimumInteractiveComponentSize`.

### 11.3 contentDescription

- **Ícones funcionais** recebem `stringResource` com a ação descrita
  (`common_back`, `project_detail_delete_project`,
  `project_detail_task_menu_more`, etc.).
- **Ícones decorativos** recebem `contentDescription = null` explícito
  (ícones de `NavigationBarItem` com rótulo embaixo, ícones
  `Logout`/`ChevronRight` da `SettingsScreen`, ícone `Add` dentro do
  `ExtendedFloatingActionButton`).
- Único ícone funcional sem descrição antes do E4.4 era o `MoreVert`
  da `ProjectDetailScreen` — anotado em E4.4.

### 11.4 TalkBack

Sequências de leitura documentadas em `docs/ACESSIBILIDADE.md` §5
para Login, Home, Detalhe do projeto e Tarefas. A validação manual
com Accessibility Scanner + TalkBack no S20 FE (E5.3) permanece
pendente (seção "Achados manuais" em branco) — registrada como
limitação conhecida (ver §16).

---

## 12. Testes e cobertura

### 12.1 Frameworks

| Categoria | Ferramenta | Uso |
|-----------|------------|-----|
| Unit (JVM puro) | JUnit 4.13.2 + Truth 1.4.4 | Domínio (`core/domain/src/test/...`), Theme tokens |
| Unit (Robolectric) | Robolectric 4.15.1 + WorkManager Testing 2.9.1 + MockWebServer 4.12.0 | `:core:data` (Room in-memory, DAOs, repositories, RemoteDataSource com HTTP simulado, Dispatcher), `:app` (`SyncWorker`, `CompleteTaskWorker`, `WorkManagerDeadlineScheduler`, `NotificationPermissionStore`, `I18nStringsSmokeTest`, `BrainOutAppSmokeTest`) |
| Coroutines | `kotlinx-coroutines-test` 1.9.0 + Turbine 1.2.0 | Flows e ViewModels |
| Mocks | MockK 1.13.13 | ViewModels com fronteira assíncrona |
| Instrumented | `androidx.test.runner` + Room-testing 2.8.4 | `MigrationTest`, `UserDaoInstrumentedTest`, `LoginScreenTest` |
| Backend (pytest) | pytest + FastAPI `TestClient` | `backend-stub/tests/test_contract.py` (16 casos) |

### 12.2 Cobertura Kover (E4.7)

Plugin `org.jetbrains.kotlinx.kover` 0.9.9 aplicado em
`:core:domain` e `:core:data` (configurado em `build.gradle.kts`
raiz, subprojetos via `subprojects { ... apply(plugin =
"org.jetbrains.kotlinx.kover") }`). Bound mínimo de **60% de
cobertura de linhas** — falhar `koverVerify` quebra o job
`unit-tests` do CI.

Filtros documentados (classes geradas e infra de wiring fora do
denominador):

```
*_Impl, *_Impl$*, *_Factory, *_Factory$*, *_HiltModules,
dagger.hilt.*, hilt_aggregated_deps.*, *.di.*, *.BuildConfig,
*.PackageMarker, *.remote.*
```

#### 12.2.1 Cobertura atual por camada

| Camada | Cobertura de linhas | Origem do número |
|--------|---------------------|------------------|
| `:core:domain` | **87,9%** | ROADMAP E4.7 (atualizado após E2.7) |
| `:core:data` | **67,3%** | ROADMAP E4.7 (atualizado após E2.7) |

> ⚠️ **Números pendentes de refresh — não são a medição atual.**
> Os dois valores acima vêm do marco E4.7 e **não foram reexecutados
> desde então**. Existem três versões divergentes do mesmo número: esta
> tabela (87,9% / 67,3%), o apêndice
> ([docs/APENDICE-C-CONFORMIDADE.md:35-36](APENDICE-C-CONFORMIDADE.md),
> 84,9% / 84,1%) e o `kover report.xml` (90,0% / 83,6%). As três
> divergem porque o denominador do gate mudou depois (DEF-14 estreitou
> `*.remote.*` para `*Dto`, devolvendo `RemoteDataSource`, `BrainOutApi` e
> `HolidayRemoteDataSource` à medição) e porque a cadeia de correções de
> outubro (DEF-08..DEF-16, PR #143) ainda não foi consolidada.
> **Refresh deliberadamente fora desta revisão:** só um build reexecutado
> depois que a cadeia aterrissar pode arbitrar o número, e é isso que a
> revisão seguinte fará. Enquanto isso, não citar nenhum dos três como
> cobertura vigente. O gate de 60% (E4.7) segue satisfeito em qualquer
> das três leituras.

Relatório HTML publicado como artifact `coverage-report` (retenção
14 dias) no job `unit-tests` do `ci.yml`.

### 12.3 Catálogo de testes por módulo

Contagem de arquivos de teste (excluindo instrumented):

| Módulo | # arquivos `.kt` de teste |
|--------|----------------------------|
| `:app` | 6 |
| `:core:data` | 15 |
| `:core:domain` | 22 |
| `:core:ui` | 2 |
| `:feature:auth` | 2 |
| `:feature:projects` | 2 |
| `:feature:tasks` | 3 |
| `:feature:settings` | 2 |
| `backend-stub/tests` | 2 (16 pytest cases em `test_contract.py`) |
| **Total** | **56 unit + 3 androidTest + 16 pytest** |

> **Contagens de teste não verificadas nesta revisão.** A tabela
> acima é a medição do fechamento do E5.1 e **não** foi reexecutada aqui —
> nenhum `./gradlew test` foi rodado, por decisão explícita deste card
> (o build está sob a cadeia de correções em andamento no mesmo working
> tree). O apêndice registra 618 testes em 08/10/2026 e o
> `docs/DEFEITOS.md` cita 662 após a correção DEF-10; nenhuma das três
> leituras foi arbitrada aqui. **Nenhuma contagem de testes deve ser
> citada como medição atual** até o refresh pós-cadeia. O refresh
> esperado é maior que 56/3/16, porque DEF-08 fez a suíte
> instrumentada de `:core:data` (`MigrationTest`,
> `UserDaoInstrumentedTest`) passar a compilar, e DEF-13 habilitou
> `LoginScreenTest` no emulador — os três `androidTest` acima só
> passaram a rodar, não a existir.

### 12.4 Cobertura funcional por requisito

A correspondência entre casos e requisitos está em
[docs/ROTEIRO-TESTES.md](ROTEIRO-TESTES.md) §3. Resumo:

| Requisito | Marcos | Casos principais |
|-----------|--------|------------------|
| R1 — Telas | E1.3, E2.1, E2.2, E3.3 | TF-05, TF-06, TF-07, TF-08 |
| R2 — Auth e perfis | E1.6, E1.7 | TF-01, TF-02, TF-03, TF-04, TF-14 |
| R3 — Mudança de status | E2.2 | TF-12 (via `CompleteTaskWorker`) |
| R4 — Regras | E2.3, E2.4, E2.5 | TF-08 (RN01) |
| R5 — Sync/conectividade | E3.3, E3.4 | TF-09, TF-10 |
| R6 — Backend | E3.1, E3.2, E3.3 | TF-09, TF-10 (online) |
| R7 — Serviço externo | E3.5 | (BrasilAPI) `HolidayRepositoryTest` + `CheckDeadlineUseCaseTest` |
| R8 — Notificações | E3.6 | TF-11, TF-12 |
| R9 — Listagens | E2.6, E2.7 | TF-05, TF-13 + `HomeViewModelTest` (14 casos) + `DashboardViewModelTest` (10 casos) |
| R10 — Erros | E2.8 | TF-02, TF-04, TF-06, TF-08 |
| R14 — Release | E3.7 | `release-apk.yml` run 35599507321 (`v0.3.1-ciclo3`) |

### 12.5 Estado dos casos TF-01..TF-14

A coluna "Resultado observado" do roteiro permanece em branco
(`—`) até a sessão manual; casos dependentes de E3.3/E3.4 (TF-09,
TF-10) dependem da execução em rede real. Detalhes em
`docs/ROTEIRO-TESTES.md` §1 e §5.

---

## 13. CI/CD

Pipelines definidos em `.github/workflows/`. Detalhes operacionais
em [docs/CI-CD.md](CI-CD.md).

### 13.1 Workflows

| Workflow | Gatilho | Função |
|----------|---------|--------|
| `ci.yml` | Push em `main`, PR contra `main`, `workflow_dispatch` | 3 jobs: `static-analysis` (ktlint + detekt), `unit-tests` (JUnit + Compose + Kover + Android Lint), `backend-integration` (service container FastAPI + smoke HTTP + pytest) |
| `release-apk.yml` | Tag `v*` | `bundleRelease` assinado com 4 secrets `BRAINOUT_*`; validação dos secrets + `jarsigner -verify` antes do upload; artifact `brainout-release-aab-<tag>` (retenção 30 dias) |
| `codeql.yml` | Push em `main`, PR, semanal | CodeQL em Java + Kotlin |
| `pages.yml` | Push em `main` (docs) | Publica a documentação GitHub Pages (apenas docs/) |

Configurações auxiliares: `.github/dependabot.yml` (atualizações
semanais de actions/Gradle) e `.github/CODEOWNERS` (aprovação por
`@joao-pedro-gms`).

### 13.2 Jobs do `ci.yml` (3 sequenciais)

1. **`static-analysis`** — ktlint + detekt. Falha rápido.
   - `./gradlew ktlintCheck` (linha 63)
   - `./gradlew detekt` (linha 67)
   - Artifact `detekt-report` (14 dias).
2. **`unit-tests`** — depende de `static-analysis`. Roda
   `./gradlew testDevDebugUnitTest` (variante `Dev`, evita pular
   `:app` e `:core:data`), `./gradlew :core:domain:koverVerify
   :core:data:koverVerify`, `./gradlew
   :core:domain:koverHtmlReport :core:data:koverHtmlReport`,
   `./gradlew lintDevDebug` (gate `MissingTranslation` ativo).
   Artifacts `unit-test-results` (14 dias) e `coverage-report` (14
   dias).
3. **`backend-integration`** — depende de `static-analysis`. Constrói
   `backend-stub/Dockerfile`, sobe o container, executa smoke HTTP
   ponta-a-ponta contra `PUT/DELETE/tags` e roda
   `backend-stub/tests/test_contract.py` (16 casos pytest). Para
   instrumentação Android sem emulador confiável no CI free, o job
   roda `./gradlew :core:data:testDevDebugUnitTest
   :app:testDevDebugUnitTest` apontando para `BASE_URL=http://localhost:8000`.

### 13.3 Secrets necessários (`release-apk.yml`)

Configurar em **Settings → Secrets and variables → Actions → New
repository secret**. Exatamente 4 nomes:

| Secret | Conteúdo |
|--------|----------|
| `BRAINOUT_KEYSTORE_BASE64` | Conteúdo do `brainout-release.jks` codificado em base64 (uma linha) |
| `BRAINOUT_KEYSTORE_PASSWORD` | Senha do keystore |
| `BRAINOUT_KEY_ALIAS` | Alias da chave dentro do keystore |
| `BRAINOUT_KEY_PASSWORD` | Senha da chave (em PKCS12 do JDK 21, igual à senha do keystore) |

Geração do keystore local (PKCS12):

```bash
keytool -genkeypair -v \
  -keystore brainout-release.jks \
  -alias brainout \
  -keyalg RSA -keysize 2048 \
  -validity 10000
```

Codificação para secret:

```bash
base64 -w 0 brainout-release.jks > brainout-release.jks.b64
# copie o conteúdo para BRAINOUT_KEYSTORE_BASE64
```

### 13.4 Procedimento de corte de release

1. Atualizar `versionName` e `versionCode` em `app/build.gradle.kts`
   (atualmente `versionName = "0.1.0-alpha01"`, `versionCode = 1`).
2. Confirmar os 4 secrets cadastrados.
3. Tag e push:
   ```bash
   git tag -a vX.Y.Z -m "Release vX.Y.Z"
   git push origin vX.Y.Z
   ```
4. Acompanhar **Actions → Release AAB assinado**; baixar artifact
   `brainout-release-aab-vX.Y.Z` (retenção 30 dias).

### 13.5 Comandos locais equivalentes

```bash
# Análise estática (mesma dupla do CI)
./gradlew ktlintCheck detekt

# Testes unitários
./gradlew testDebugUnitTest
# (cobre :app, :core:data, :core:domain via wire-up do build.gradle.kts raiz)

# Android Lint
./gradlew :app:lintDevDebug

# Cobertura mínima 60%
./gradlew :core:domain:koverVerify :core:data:koverVerify

# Relatório HTML de cobertura
./gradlew koverMergedHtmlReport

# Subir o backend stub (R6) e apontar o app
cd backend-stub
docker build -t brainout-stub .
docker run -d --name brainout-stub -p 8000:8000 brainout-stub
echo "brainout.baseUrl.dev=http://10.0.2.2:8000" >> ../local.properties
# (10.0.2.2 é o host a partir do emulador padrão do Android Studio.
#  `BASE_URL` como env var tem precedência sobre esta chave e é o que o CI usa.)
```

### 13.6 Branch protection recomendada

`Settings → Branches → main`:

- ✅ Require a pull request before merging
- ✅ Require approvals: **1** (ou 2 se revisão adicional)
- ✅ Dismiss stale pull request approvals when new commits are pushed
- ✅ Require status checks to pass: `Static analysis (ktlint + detekt)` e `Unit tests`
- ✅ Require conversation resolution before merging
- ✅ Require linear history
- ✅ Include administrators

---

## 14. Instalação e execução

### 14.1 Pré-requisitos

| Ferramenta | Versão mínima | Observação |
|------------|---------------|------------|
| JDK | 21 (Temurin no CI/release; JBR do Android Studio localmente) — piso do Gradle 9.7.1/AGP 9.4.1 é 17, mas todo o fluxo é verificado em 21; bytecode permanece Java 17 | `org.gradle.java.installations.auto-detect=true` + `auto-download=false` em `gradle.properties` |
| Android SDK | `compileSdk = 37` | Instalar via Android Studio ou `sdkmanager` |
| Android Studio | Hedgehog (2023.1.1)+ | Emulador, editor, SDK Manager |
| Emulador ou dispositivo | API 24+ | Emulador API 37 (imagem `system-images;android-37;google_apis;x86_64`) ou dispositivo físico com depuração USB |
| Python 3 + pip | 3.10+ | Apenas para o backend stub local |
| Docker | opcional | Para rodar o `backend-stub` em container |

### 14.2 Clone e configuração local

```bash
git clone https://github.com/joao-pedro-gms/BrainOut.git
cd BrainOut
cp local.properties.example local.properties
# edite `sdk.dir` para o caminho do Android SDK
#   (ex.: /home/<user>/Android/Sdk)
```

Conteúdo esperado de `local.properties` (este repositório contém um
`local.properties` real com apenas `sdk.dir` e `BASE_URL` para o
desenvolvedor; nenhum segredo está versionado):

```properties
sdk.dir=/home/<user>/Android/Sdk
BASE_URL=http://10.0.2.2:8000
brainout.baseUrl.dev=http://10.0.2.2:8000/
APP_ENV=dev
```

### 14.3 Build por flavor

O projeto tem dois flavors (`dev`, `prod`) na dimensão
`environment`. Cada flavor injeta `BuildConfig.BASE_URL` e
`BuildConfig.HOLIDAYS_BASE_URL`.

```bash
# Variante dev (CI + dia a dia)
./gradlew :app:assembleDevDebug
# APK: app/build/outputs/apk/dev/debug/app-dev-debug.apk

# Variante prod
./gradlew :app:assembleProdDebug
# APK: app/build/outputs/apk/prod/debug/app-prod-debug.apk

# Todas as variantes de uma vez
./gradlew assembleDebug
```

Para release assinado local, exportar as 4 variáveis `BRAINOUT_*` e
rodar `./gradlew :app:bundleRelease`. Sem as variáveis, o build sai
não-assinado de propósito (não quebra o CI comum).

### 14.4 Testes

```bash
# Testes unitários em todos os módulos (inclui :core:domain via wire-up do build.gradle.kts raiz)
./gradlew testDebugUnitTest
# Relatórios: <modulo>/build/reports/tests/testDebugUnitTest/

# Android Lint (gate MissingTranslation em :app)
./gradlew :app:lintDevDebug

# Instrumented (requer emulador/dispositivo conectado)
./gradlew connectedDevDebugAndroidTest
```

### 14.5 Backend stub

```bash
# Opção A — Python direto
cd backend-stub
python -m pip install --no-cache-dir -r requirements.txt
python -m uvicorn server:app --host 0.0.0.0 --port 8000

# Opção B — Docker (usado pelo CI)
docker build -t brainout-stub ./backend-stub
docker run -d --name brainout-stub -p 8000:8000 brainout-stub

# Contrato via suíte pytest
python -m pytest backend-stub/tests/ -v   # 16 casos
```

No emulador padrão, o host é alcançado em `10.0.2.2`. Em
dispositivo físico, use o IP LAN (`ip addr`/`hostname -I`) e rode o
stub com `--host 0.0.0.0`.

### 14.6 Emulador + execução do app

```bash
# Criar AVD uma vez (cmdline-tools instalado)
sdkmanager "system-images;android-37;google_apis;x86_64"
avdmanager create avd -n pixel8 -k "system-images;android-37;google_apis;x86_64" -d pixel_8

# Subir emulador e instalar
$ANDROID_HOME/emulator/emulator -avd pixel8 &
adb install app/build/outputs/apk/dev/debug/app-dev-debug.apk
```

No Android Studio: **Build → Select Build Variant → `devDebug`** e
**Run ▶**.

### 14.7 Troubleshooting (resumo)

| Sintoma | Causa provável | Ação |
|---------|----------------|------|
| `Cannot find a Java installation matching languageVersion=17` | Só o JBR do Android Studio instalado | `export JAVA_HOME=/opt/android-studio/jbr` antes de `./gradlew` |
| `error: unresolved reference` em classes `*_Impl`/`Hilt_*` | KSP não rodou (cache sujo após bump Kotlin/AGP) | `./gradlew clean` + rebuild; conferir `gradle/libs.versions.toml` (AGP 9 exige KGP ≥ 2.2.10) |
| `MissingTranslation` falha o lint | Chave em `values/` sem par em `values-en/` | Traduzir a chave (E4.6 — gate ativo em `:app`) |
| `OutOfMemoryError` | Heap insuficiente | `org.gradle.jvmargs=-Xmx4g` já em `gradle.properties`; `./gradlew --stop` |
| `Connection refused` no emulador | Stub não está de pé | Verificar `docker ps`/uvicorn; emulador alcança host em `10.0.2.2`, não `localhost` |
| `release-apk.yml` falha em `Decode keystore` | Secrets ausentes ou base64 corrompido | Cadastrar 4 secrets; recodificar com `base64 -w 0` |
| `signReleaseBundle` falha: `Given final block not properly padded` | PKCS12 com senhas divergentes em `-storepass` e `-keypass` | Gerar keystore com a MESMA senha para store e key (JDK 21 PKCS12) |
| Workflow não dispara no PR | Branch protection / permissões | **Settings → Actions → General → Allow actions** |

A tabela completa está em `docs/CI-CD.md` (seção Resolução de
problemas) e em `README.md` (seção Troubleshooting).

---

## 15. Credenciais por perfil

Esta seção lista os **perfis de demonstração** previstos para a N2.
O BrainOut **não versiona hashes de senha** nem User seeds no banco
local — contas de demonstração são criadas na tela de Cadastro. Os
valores abaixo são **placeholders descritivos**, não credenciais
válidas (não inventamos hashes).

### 15.1 Política de papéis

`UserRole` em
`core/domain/.../model/UserRole.kt`:

| Papel | Permissões | Caso de uso |
|-------|------------|-------------|
| **`OWNER`** | `CREATE_PROJECT`, `EDIT_PROJECT`, `DELETE_PROJECT`, `CREATE_TASK`, `EDIT_TASK`, `DELETE_TASK`, `INVITE_MEMBER` | Criador do projeto; papel padrão atribuído em `CreateUserUseCase` (`User.create` default = `OWNER`) |
| **`MEMBER`** | `CREATE_TASK`, `EDIT_TASK` | Visualiza projetos em que é `assignee_id`; não administra |

A diferenciação é visível na Home: o botão "novo projeto" só é
habilitado para `OWNER` (gate em `HomeViewModel` via
`CanPerformActionUseCase`). Detalhes em
`docs/USABILIDADE.md` (tarefas-chave T1/T2/T3) e em
[docs/APENDICE-C-CONFORMIDADE.md](APENDICE-C-CONFORMIDADE.md) R2.

### 15.2 Conta de demonstração Owner

> **Placeholder** — não há User seed no banco. Para criar a conta,
> abra o app e use a tela **Cadastrar** (RegisterScreen, em
> `feature/auth/.../ui/register/RegisterScreen.kt`) com os valores
> abaixo. A senha precisa ter pelo menos 8 caracteres
> (`MIN_PASSWORD_LENGTH` em `CreateUserUseCase.kt`).

| Campo | Valor (placeholder) | Como é gerado |
|-------|----------------------|----------------|
| Nome | `Maria Owner` | Formulário de cadastro |
| E-mail | `owner@brainout.test` | Formulário de cadastro |
| Senha | `senha-123` (≥ 8 chars) | Formulário de cadastro |
| Papel | `OWNER` (default no cadastro) | `User.create(role = UserRole.OWNER)` |

Referência cruzada: TF-01 do `docs/ROTEIRO-TESTES.md` (cadastro
Owner com dados válidos — redireciona para a Home com perfil
`OWNER`).

### 15.3 Conta de demonstração Member

> **Placeholder** — idem. Como `MEMBER` não é exposto no fluxo de
> cadastro (papel default é `OWNER`), esta conta só é criável
> diretamente em teste automatizado ou via mutação direta do banco.
> Para a sessão de usabilidade (E4.2), a colega ADS que assumir o
> papel `MEMBER` pode entrar com credenciais pré-criadas pelo autor
> antes da sessão.

| Campo | Valor (placeholder) | Como é gerado |
|-------|----------------------|----------------|
| Nome | `João Member` | Inserção manual ou seed de teste |
| E-mail | `member@brainout.test` | Inserção manual ou seed de teste |
| Senha | `outra-456` (≥ 8 chars) | Inserção manual ou seed de teste |
| Papel | `MEMBER` | `User.create(role = UserRole.MEMBER)` |

> **Observação sobre hashing.** O hashing da senha é feito por
> `PasswordHasherImpl.hash(rawPassword)` +
> `PepperProvider.getOrCreatePepper()` (EncryptedSharedPreferences +
> `MasterKey.AES256_GCM`). Como o pepper é gerado por
> `SecureRandom` na primeira execução e **persistido criptografado
> no dispositivo do usuário**, hashes não são portáveis entre
> dispositivos. Por isso a N2 não publica hashes pré-calculados —
> qualquer credencial "válida" neste documento seria falsa.

### 15.4 Conta do backend stub

O `backend-stub` (FastAPI em `backend-stub/server.py`) **não tem
autenticação** — ele confia no `id` cliente-supplied do app e não
emite tokens. Para acoplar uma camada de auth real, a evolução do
stub para o backend definitivo é o item fora-de-escopo da N2.

### 15.5 Procedimento recomendado para o avaliador

1. Instalar a APK `devDebug` ou um `.aab` assinado da tag `v*`.
2. Abrir o app; o `Splash` consulta o `SessionStore`; sem sessão,
   navega para a tela **Login**.
3. Em **Login**, tocar em **Cadastrar**.
4. Preencher `Nome`, `E-mail`, `Senha` (≥ 8 chars) e `Confirmação`
   conforme TF-01; tocar em **Registrar**. O app persiste o usuário
   com papel `OWNER` e navega para a **Home**.
5. Para experimentar a diferenciação de papéis, criar uma segunda
   conta pelo mesmo fluxo e, em seguida, atualizar o `role` da
   segunda conta para `MEMBER` via ferramenta de inspeção do Room
   (ex.: `adb shell run-as pucgo.joaopedrogmsilva.brainout.debug
   sqlite3 databases/brainout.db "UPDATE users SET role='MEMBER'
   WHERE email='member@brainout.test'"`). Esse passo é opcional e
   só é útil para avaliar a matriz de permissões.

---

## 16. Limitações conhecidas

Esta seção reúne limitações explícitas do estado atual do
repositório. Origem: registro de defeitos
[docs/DEFEITOS.md](DEFEITOS.md) (DEF-02..07), auditoria técnica
[docs/AUDITORIA-2026-09-24.md](AUDITORIA-2026-09-24.md) e bloqueios
documentados em [docs/ROADMAP.md](ROADMAP.md).

### 16.1 Defeitos com tratamento definido (E4.3)

| ID | Sev. | Origem | Resumo | Status |
|----|------|--------|--------|--------|
| DEF-02 | Crítico | CI run 35598981118 | `signReleaseBundle`: `BRAINOUT_KEY_PASSWORD` difere da senha que abre a chave dentro do PKCS12 (PKCS12 usa a senha do keystore para a chave). | **Não corrigido em código** — contorno documentado em `docs/CI-CD.md` (PR #47): gerar o keystore com a MESMA senha em `-storepass` e `-keypass` e cadastrar os dois secrets com esse valor. Sem correção de código a fazer — a falha só se repete se o operador gerar o keystore com senhas divergentes. |
| DEF-06 | Crítico | QA manual (i18n) | 8 chaves `deadline_*` (E3.6) presentes em `app/src/main/res/values/strings.xml` (PT) sem par em `values-en/strings.xml`. Com locale `en` ativa, notificações aparecem em PT. A regra `lint { abortOnError = true; error += "MissingTranslation" }` em `:app` (PR #49) não pegou porque o CI roda `ktlint + detekt` (não `lintDebug`). | **CORRIGIDO** — commit `d171d25` (PR #57, mergeado em `main` em 21/09/2026), conforme `docs/DEFEITOS.md:129`. As 8 chaves foram traduzidas e a regra foi endurecida no CI (`lintDevDebug`), que hoje barra a regressão. Reverificado em 08/10/2026 (`docs/DEFEITOS.md`). **Não há mais pendência de confirmação em E4.8**: o E4.8 já congelou e a correção está em `main`. |
| DEF-07 | Menor | QA manual (holidays) | Em `main`, o módulo de feriados (E3.5) ainda não havia recebido polimento (assinatura de testes, `@Suppress("NewApi")` para `java.time.*` em `minSdk 24`, supressões justificadas de detekt para `TooManyFunctions`/`LongParameterList`/`LongMethod`, e traduções EN). detekt e `lintDebug` falhavam em `:core:data` e `:feature:projects`. | **CORRIGIDO** — PR #59 (`feat(holidays): integrar consulta de feriados e prazos (E3.5)`), mergeado em `main` em 21/09/2026 como commit `bb95109`, conforme `docs/DEFEITOS.md:130`. **O PR #51 (`feat/external-holiday-api`, commit `e828544`), citado em versões anteriores deste relatório, foi FECHADO SEM MERGE** e portanto nunca corrigiu o defeito: ele não está em `main` e não deve ser lido como evidência de correção. A evidência válida é o PR #59 / `bb95109`. |

Defeitos já corrigidos (não bloqueiam a N2): **DEF-01** (PR #43 —
keystore em path absoluto `$RUNNER_TEMP`), **DEF-03** (PR #48 —
registro do run-log), **DEF-04** (commit `f528ff1` — wireframe
low-fi), **DEF-05** (PR #47 — flags `-keyalg`/`-keysize` no
keytool).

### 16.2 Achados da auditoria técnica (24/09/2026)

A revisão consolidada em `docs/AUDITORIA-2026-09-24.md` lista
achados P0/P1/P2. Os que afetam o produto entregue na N2 estão
resumidos a seguir (status de merge no momento da redação):

| ID | Sev. | Local | Status na N2 |
|----|------|-------|---------------|
| P0-1 | Crítico | `BrainOutNavHost.kt` — `signOut()` em escopo cancelável junto à composição | **Resolvido** — o `signOut()` roda em `lifecycleOwner.lifecycleScope.launch`, não em `rememberCoroutineScope` (`app/src/main/kotlin/pucgo/joaopedrogmsilva/brainout/navigation/BrainOutNavHost.kt:181`). A escrita no DataStore conclui antes de navegar e o escopo sobrevive à saída de `Settings`, evitando sessão órfã na próxima abertura. |
| P0-2 | Crítico | `CompleteTaskWorker.kt` — `catch (Exception)` retray tarefas deletadas | **Resolvido** — o worker captura `DomainException`, não `Exception` cru (`app/src/main/kotlin/pucgo/joaopedrogmsilva/brainout/notifications/CompleteTaskWorker.kt:52`), e termina idempotentemente com `Result.success()` para que o WorkManager não reprocesse uma violação permanente. Coberto por `CompleteTaskWorkerTest`. |
| P0-3 | Crítico | `PepperProvider.kt` — pepper derivado de `masterKey.toString()` (string pública) | **Resolvido** — o pepper é sorteado com `SecureRandom` (`core/data/.../security/PepperProvider.kt:64-66`, 16 bytes em `:77`) e persistido em `EncryptedSharedPreferences` (`:49`), protegido por `MasterKey` AES256-GCM. Não há mais `masterKey.toString()` no caminho de derivação; o pepper é determinístico por instalação. |
| P0-4 | Crítico | `BrainOutSyncDispatcher` + `TagSyncPayload` — DELETE de tag sem id cliente-supplied | **Resolvido** — `TagSyncPayload` carrega `id` cliente-supplied com `@SerialName("id")` (`core/data/.../local/entity/PendingOpEntity.kt:158-161`), paridade com projetos e tasks no contrato `/v1/*` (UUID do cliente, PUT/POST upsert idempotente). |
| P0-5 | Crítico | `ci.yml` — tarefas não-flavor (`testDebugUnitTest`/`lintDebug`) | **Resolvido** — CI migrado para `testDevDebugUnitTest`/`lintDevDebug` (linhas 110, 125, 222 do `ci.yml`) |
| P0-6 | Crítico | `release-apk.yml:52-56` — validação parcial dos secrets | **Resolvido** — workflow valida os 4 secrets + `jarsigner -verify` (release-apk.yml) |
| P0-7 | Menor | `TagNotFoundException` extends RuntimeException | **Resolvido** — `TagNotFoundException` estende `DomainException`, a raiz única do pacote `error/` (`core/domain/.../error/TagNotFoundException.kt:11-13`), então `catch (DomainException)` captura também essa falha. Hierarquia fixada por `DomainExceptionHierarchyTest` e `TagNotFoundExceptionTest` em `core/domain/src/test/.../error/`. |
| P0-8 | Crítico | `ChangeTaskStatusUseCase.kt` — `kotlin.error` lança `IllegalStateException` cru | **Resolvido** — não há mais `kotlin.error` no arquivo (`core/domain/.../usecase/ChangeTaskStatusUseCase.kt`); a transição inválida é recusada pela hierarquia de domínio, com `InvalidStateTransitionException` sob `DomainException`. |

> Verificação: os oito P0 foram reconferidos **um a um no código** em
> 2026-10-09 (`grep`/`read` sobre `main`, commit `54f1d79`). Os P0-5 e
> P0-6 já constavam como resolvidos na redação anterior; os outros seis
> passaram de "Pendente de correção" para "Resolvido" com a evidência
> `file:line` da coluna acima. Nenhum deles depende de uma execução de
> build: são todos verificáveis por leitura de fonte.

P1/P2 (refatoração, decomposição de telas grandes, supressões de
detekt) **não bloqueiam** a N2 e estão listados na auditoria como
backlog pós-N2.

### 16.3 Smoke headless (sem dispositivo físico)

Limitações documentadas em
[docs/ROADMAP.md](ROADMAP.md) — seção "Riscos e mitigações":

- **E3.6 — disparo real de notificação**: o emulador headless do CI
  não permite `adb shell date` (requer root em build de produção),
  então o smoke ponta-a-ponta do `DeadlineWorker` não foi exercitado.
  Cobertura via `WorkManagerDeadlineSchedulerTest` (verifica
  `WorkInfo` com `DEADLINE_TAG`) e `CompleteTaskWorkerTest`. Disparo
  real registrado em TF-12 do `docs/ROTEIRO-TESTES.md` para o
  dispositivo físico do E5.3.
- **E3.5 — BrasilAPI em emulador headless**: a rede interna do
  laboratório é inacessível para `https://brasilapi.com.br/`. A UI
  degrada para `holidays unavailable` (coberto por
  `HolidayRepositoryTest`). Dica de próximo feriado exercitada em
  rede real no dispositivo do E5.3 / TF-09.
- **E3.4 — banner offline**: exercitado em emulador API 35 com
  `cmd connectivity airplane-mode enable/disable`; cobertura dupla
  via `ConnectivityObserverTest` (Robolectric shadow) e casos
  E3.4 em `HomeViewModelTest` (17 casos) e
  `ProjectDetailViewModelTest` (14 casos).

### 16.4 Correções posteriores ao fechamento do E5.1

Esta subseção foi acrescentada em 2026-10-09 para registrar o que
aconteceu no repositório **depois** da redação original deste relatório.
Nada aqui é alterado pelo status de R1–R14 da §4; são correções e
reforços de qualidade. Detalhe completo em
[docs/DEFEITOS.md](DEFEITOS.md).

**Auditoria de 08/10/2026 — DEF-08..DEF-14** (gate de qualidade e
ambiente, todos **corrigidos**):

| ID | Sev. | Achado | Correção |
|----|------|--------|----------|
| DEF-08 | Crítico | A suíte instrumentada de `:core:data` **não compilava**: `MigrationTest` e `UserDaoInstrumentedTest` importavam Truth e `runTest`, declarados só em `testImplementation`. As 3 migrations do Room (`MIGRATION_1_2`, `MIGRATION_2_3`, `MIGRATION_3_4`) não eram exercitadas em lugar nenhum. | `core/data/build.gradle.kts:181-182` (`androidTestImplementation`); `MigrationTest` passa a cobrir 2→3 e 3→4 em SQLite real. |
| DEF-09 | Crítico | O plugin ktlint era aplicado **só na raiz**: `ktlintCheck` inspecionava 2 arquivos de build e **zero** dos arquivos `.kt` versionados. O gate de estilo do CI era verde por construção. | `build.gradle.kts:112-129` (`subprojects { apply(plugin = "...ktlint") }` com `android = true`); agora há relatório nos 9 projetos. |
| DEF-10 | Menor | Com o ktlint alcançando os 9 projetos, apareceram violações cosméticas em 17 arquivos. | `ktlintFormat` nos 9 subprojetos (commit `b59318e`) + 9 violações não auto-corrigíveis (`b6db6e0`): **770 → 0**. `ignoreFailures` voltou para `false` (`631a4ba`) — o gate volta a bloquear. |
| DEF-11 | Menor | `.gitignore` não cobria `.venv`, e o `README.md:148` manda criar `backend-stub/.venv` — um `git add` de rotina versionaria o virtualenv inteiro. | `.gitignore:66-73` cobre `.venv/`, `venv/`, `env/` e `backend-stub/.venv/`. |
| DEF-12 | Menor | `backend-stub/requirements.txt` fixava `pydantic==2.9.2` (→ `pydantic-core 2.23.4`), sem wheel para Python 3.14; a instalação caía em build via Rust e falhava porque o `PyO3` só declara suporte até 3.13. | Pins relaxados para faixas (`fastapi>=0.118.3,<1.0`, `pydantic>=2.12,<3.0`, …); instala por wheel em 3.12, 3.13 e 3.14. |
| DEF-13 | Crítico | `LoginScreenTest` compilava mas **os 3 testes falhavam em runtime** no emulador API 37: `NoSuchMethodException: InputManager.getInstance`, porque o **Espresso 3.6.1** usava um método removido da plataforma. | `espresso` 3.6.1 → **3.7.0** (`gradle/libs.versions.toml:51`), que troca `InputManager.getInstance` por `Context.getSystemService`. Verificado no emulador: 3 testes, 0 falhas. |
| DEF-14 | Menor | O gate do Kover excluía `*.remote.*`, tirando **806 linhas** de produção do denominador em `:core:data` — inclusive `RemoteDataSource` (253 linhas), que **tem** teste. A métrica estava inflada. | Padrão estreitado para `*Dto`/`*Dto$*`; `RemoteDataSource`, `BrainOutApi` e `HolidayRemoteDataSource` voltaram ao denominador. `*.di.*` mantido (wiring, sem lógica). |

**PR #143** (`fix(auditoria): 4 bugs confirmados + paridade documental`,
commit `54f1d79`, 09/10/2026) — quatro defeitos reais de produto,
todos **corrigidos**:

- **Ordenação da Home era no-op.** `SortOrder.toStorageKey()` emitia
  `nameasc`/`namedesc`/`createddesc`/`createdasc`, enquanto o `CASE WHEN`
  de `ProjectDao.searchProjects` compara `name_asc`, `name_desc`,
  `created_asc`, `created_desc`. **Nenhum dos quatro casava**: os quatro
  `CASE` devolviam NULL e o SQLite entregava as linhas na ordem natural
  do scan — a ordenação escolhida pelo usuário nunca teve efeito. Agora
  `toStorageKey()` emite a grafia canônica em `snake_case`;
  `fromStorageKey` continua aceitando a forma concatenada legada para
  preferências já gravadas. Contrato fixado por `SortOrderStorageKeyTest`
  (`core/domain/.../repository/`) e por `ProjectDaoSearchSortOrderTest`,
  que roda a query real contra um banco Room em memória. O bug escapou
  porque `HomeViewModelTest` faz mock de `observeSearch`.
- **Bypass de RN02 fechado** (prioridade imutável em tarefa concluída era
  burlável por um overload) e **hierarquia de exceções unificada** —
  `BusinessRuleException` passou a estender `DomainException`, então
  `catch (DomainException)` também captura violações de regra de negócio.
  Contrato fixado por `DomainExceptionHierarchyTest`.
- **DEF-15** (`TaskRepositoryImpl.update` gravava tarefa inexistente e
  enfileirava a op, porque o `@Update` do Room é no-op silencioso quando a
  linha não existe) — corrigido com guarda de existência que lança
  `TaskNotFoundException` **antes** do `enqueueInTx`.
- **DEF-16** (`completeAndCascade` retornava no-op para tarefas já em
  `DONE` sem reidratar `projects.is_completed`, deixando projetos
  concluídos antes da v3 permanentemente ativos com zero tarefas ativas) —
  corrigido por `repairCompletedProject()`, reconta e marca de forma
  idempotente e sem gerar op pendente.

Ambos os DEF-15/16 são cobertos por `TaskSaveAuditTest`
(`core/data/src/test/.../local/dao/TaskSaveAuditTest.kt`), auditoria de
gravação que prova que a fila `pending_ops` **não** fica órfã.

**Identidade Neo (tema ativo).** A redesign de tokens e arte entrou pelo
NB-04/NB-05 e está **ativa por padrão**: `BrainOutTheme` delega para
`BrainOutNeoTheme`, com `NeoColor.kt`, `NeoTokens.kt`, `NeoTypography.kt`
em `core/ui/src/main/kotlin/.../theme/`, 5 fontes locais em
`core/ui/src/main/res/font/` e 20 drawables `neo_*` em `res/drawable/`.
O contraste WCAG AA tem suíte própria (`NeoContrastRatioTest`, 5 casos,
paleta Neo ativa) ao lado da legada (`ContrastRatioTest`, 24 casos). Os
totais de bytes e as regras da arte são assinados em teste contra o disco
(PR #135).

### 16.5 Pendências de validação manual (E5.3)

- **E5.3 — Teste em 2 dispositivos físicos**: o checklist 5.1–5.5 do
  `docs/DISPOSITIVOS.md` permanece em branco até a sessão presencial
  com o dispositivo secundário emprestado. O primário (Samsung
  Galaxy S20 FE 5G, Android 13, API 33) já tem a ficha preenchida.
  **É esta pendência que mantém R14 em Parcial** na §4 — sem ela, o
  requisito não pode ser marcado como atendido.
- **E4.2 — Sessões de usabilidade P1–P5**: a Parte 2 de
  [docs/USABILIDADE.md](USABILIDADE.md) (relatório de achados +
  pontuação SUS) permanece em branco. Janela de execução: 17/11 a
  19/11/2026. Os casos TF-01..TF-14 dependem dessa sessão para
  preenchimento da coluna "Resultado observado".
- **E4.4 — Accessibility Scanner / TalkBack**: seção "Achados
  manuais" de [docs/ACESSIBILIDADE.md](ACESSIBILIDADE.md) ainda em
  branco. Plano: rodar no S20 FE no E5.3.

### 16.6 Backlog pós-N2 (backlog documentado)

- Decomposição de `HomeScreen.kt` (1403 LoC), `ProjectDetailScreen.kt`
  (1027 LoC), `DashboardScreen.kt` (593 LoC) — extrair `*TopBar`,
  `*BottomBar`, `*Fab`, `*OfflineBanner`, `*States`, `*FilterRow` em
  arquivos de ≤ 200 LoC (auditoria P2).
- Mover `resolveAuthMessage`/`resolveHomeErrorMessage`/
  `resolveProjectDetailMessage`/`resolveCreateProjectNameError`
  para `ui/common/Messages.kt`.
- Endurecer `lintDevDebug` em `:core:data` e `:feature:tasks`
  (`abortOnError = false` atualmente esconde `MissingTranslation` e
  `NewApi`).
- Expandir `AuthRoutesTest`/`ProjectsRoutesTest`/`SettingsRoutesTest`
  (coberturas fracas).
- Cobertura Kover: incluir repositórios
  (`TaskRepositoryImpl`/`ProjectRepositoryImpl`/`TagRepositoryImpl`)
  com testes do dual-write e cascade (sem cobertura atual; alvo de
  ≥ 60% permanece satisfeito por outras classes).
- Adicionar tag `v1.0.0-rc` no congelamento E4.8 (27/11/2026).

---

## 17. Referências

### 17.1 Documentos do projeto (neste repositório)

- **Documento norteador**: `Documentos/Documento Norteador Projeto Integrador ADS 2026-2.pdf` (PDF na raiz do repositório).
- [`README.md`](../README.md) — pré-requisitos, build, execução, troubleshooting.
- [`docs/ROADMAP.md`](ROADMAP.md) — roadmap de implementação (E1.1–E5.5), mapeamento R1–R14, riscos e mitigações.
- [`docs/ARQUITETURA.md`](ARQUITETURA.md) — definição arquitetural (decisão E3.1, AD-1..6, camadas, sincronização, notificações, integração externa).
- [`docs/CI-CD.md`](CI-CD.md) — operação de pipelines, secrets, troubleshooting.
- [`docs/APENDICE-C-CONFORMIDADE.md`](APENDICE-C-CONFORMIDADE.md) — lista de verificação de conformidade técnica.
- [`docs/ACESSIBILIDADE.md`](ACESSIBILIDADE.md) — pauta AA, 24 testes WCAG, achados manuais.
- [`docs/DEFEITOS.md`](DEFEITOS.md) — registro e classificação de defeitos (DEF-01..07, política E4.3).
- [`docs/USABILIDADE.md`](USABILIDADE.md) — plano e relatório de sessões com 5 colegas ADS (E4.2).
- [`docs/DISPOSITIVOS.md`](DISPOSITIVOS.md) — mapeamento de dispositivos físicos (E5.3, S20 FE + secundário emprestado).
- [`docs/ROTEIRO-TESTES.md`](ROTEIRO-TESTES.md) — 14 casos funcionais (TF-01..TF-14, E4.1).
- [`docs/SMOKE-TEST-CRUD.md`](SMOKE-TEST-CRUD.md) — smoke de persistência, offline→online, banner.
- [`docs/AUDITORIA-2026-09-24.md`](AUDITORIA-2026-09-24.md) — revisão técnica (P0/P1/P2) antes da N2.
- [`docs/CONTRIBUTING.md`](CONTRIBUTING.md) — convenções de contribuição.
- [`docs/ATAS/checkpoint2.md`](ATAS/checkpoint2.md) — ata do Checkpoint 2 (E3.1).

### 17.2 Workflows GitHub Actions

- `.github/workflows/ci.yml` — `static-analysis` + `unit-tests` + `backend-integration`.
- `.github/workflows/release-apk.yml` — bundleRelease assinado com 4 secrets `BRAINOUT_*`.
- `.github/workflows/codeql.yml` — análise CodeQL Java/Kotlin.
- `.github/workflows/pages.yml` — GitHub Pages para docs.
- `.github/dependabot.yml` — atualizações semanais.
- `.github/CODEOWNERS` — aprovação por `@joao-pedro-gms`.

### 17.3 Padrões e normas citados

- WCAG 2.1 (W3C Recommendation) — [https://www.w3.org/TR/WCAG21/](https://www.w3.org/TR/WCAG21/) — §1.4.3 (Contrast Minimum), §1.4.11 (Non-text Contrast), §2.5.5 (Target Size), §1.1.1 (Non-text Content).
- Material 3 Accessibility — [https://m3.material.io/foundations/accessible-design/accessibility-basics](https://m3.material.io/foundations/accessible-design/accessibility-basics).
- System Usability Scale (SUS), Brooke (1996) — citação em `docs/USABILIDADE.md` §1.7.5.
- BrasilAPI — [https://brasilapi.com.br/](https://brasilapi.com.br/) — feriados nacionais (E3.5).
- Bundletool — [https://github.com/google/bundletool](https://github.com/google/bundletool) — geração de APKs a partir de `.aab` no E5.3.
- AGP 9 — built-in Kotlin — [developer.android.com/build/migrate-to-built-in-kotlin](https://developer.android.com/build/migrate-to-built-in-kotlin).

---

## 18. Anexo: árvore de módulos

Saída do comando `find . -maxdepth 3 -type d -not -path '*/build*'
-not -path '*/.git*' -not -path '*/.gradle*' -not -path
'*/.kotlin*' -not -path '*/.omo*' -not -path '*/node_modules*'
-not -path '*/__pycache__*' -not -path '*/.pytest_cache*' -not -path
'*/.benchmarks*' | sort`, executado em 24/09/2026:

```
.
Documentos
app
app/src
app/src/main
app/src/test
backend-stub
backend-stub/tests
config
config/detekt
core
core/data
core/data/schemas
core/data/src
core/domain
core/domain/src
core/ui
core/ui/src
docs
docs/ATAS
docs/qa
docs/wireframes
feature
feature/auth
feature/auth/src
feature/projects
feature/projects/src
feature/settings
feature/settings/src
feature/tasks
feature/tasks/src
gradle
gradle/wrapper
```

Os diretórios `core/data/schemas/pucgo.joaopedrogmsilva.brainout.core.data.local.BrainOutDatabase/{1,2,3,4}.json` armazenam o
schema Room exportado por versão (referência na auditoria
`docs/AUDITORIA-2026-09-24.md`). Os diretórios `docs/ATAS`,
`docs/qa` e `docs/wireframes` contêm, respectivamente, a ata do
Checkpoint 2, os run-logs de release (`run-log-t_333ab600.md` para
`v0.3.1-ciclo3`) e os wireframes lo-fi.

---

João Pedro G M Silva - PUC Goiás ADS - 20251012000740


