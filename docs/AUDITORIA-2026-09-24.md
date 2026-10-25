# BrainOut — Revisão técnica e plano de melhorias

**Data:** 2026-09-24T13:49Z · **Commit:** 5ce89d9 · **Branch:** main
**Escopo:** 185 arquivos `.kt/.py/.yml`, 9 módulos Gradle + backend-stub + CI.
Esta página é a síntese priorizada em P0/P1/P2 das não-conformidades e melhorias.

> **⚠️ Estado em 08/10/2026.** Esta página foi escrita em 24/09/2026 e
> **o diagnóstico do arquivo continua válido**, mas o *status* de
> execução mudou: **os 8 P0 (P0-1 a P0-8) já foram corrigidos e estão
> em `main`**. A tabela abaixo de P0-7 a P0-8 foi reescrita para
> refletir o estado real; a seção *Correções aplicadas* e a nova
> § [Reverificação 2026-10-08](#reverificacao-2026-10-08) registram
> a evidência (`arquivo:linha`) de cada correção. Onde um item de P1/P2
> mudou de estado, ele está anotado no ponto em que aparece.

> **⚠️ Estado em 23/10/2026.** A rodada de revisão de código fechou os
> itens de P1/P2 que ainda dependiam só de decisão de execução, e o
> *ledger* passou a ser a fonte da verdade do que está aberto:
> [`docs/DEFEITOS.md`](DEFEITOS.md) (**71 defeitos corrigidos**; restam
> **DEF-24** (parcial — os empty states), **DEF-74** e **DEF-78**
> (parcialmente corrigidos) e **DEF-75**, **DEF-76** e **DEF-77**
> (abertos)). Desde
> 08/10/2026, entre outras correções: o hash de senha saiu da thread
> principal (DEF-71), a identidade da notificação de prazo deixou de
> colidir (DEF-54), a Home parou de mostrar um usuário fake (DEF-72), os
> diálogos passaram a ficar acima do teclado (DEF-46) e a barra inferior
> virou abas de verdade, com o `when (currentTab)` morto removido. Os
> alvos de decomposição de arquivos (1403/1027/593/583 linhas) hoje
> estão todos abaixo de 450 linhas.

> **⚠️ Estado em 24/10/2026.** A rodada seguinte atacou a **camada de
> teste**, que a de 23/10 não alcançara: cinco PRs (#328 a #332) tiraram
> da duplicação o que ainda estava duplicado — o `inMemoryDatabaseBuilder`
> do Room (7 cópias), as fábricas de entidade (3), o
> `FakeLocalTransaction` (3), a fábrica de worker (6) e o `FakeProjectDao`
> (2 de ~80 linhas) — e fecharam a lacuna do teste de paridade dos
> tokens, que prometia ler o contrato e repetia os valores. Uma varredura
> de código morto sobre as 1 452 declarações da produção não achou
> **nada**: o único símbolo sem uso (`NeoSizes.touchGap`) era um token do
> design system sem cobertura — e agora tem.
>
> A rodada fechou com mais três PRs (#334 a #336): a decomposição do
> formulário de cadastro levou a **última** supressão de `LongMethod` do
> projeto embora (122 → 52 linhas, o corpo passou a listar as seções do
> formulário), o `TasksRoutesTest` fechou o gap de cobertura das rotas que
> a `:feature:tasks` expõe — **sem** a dependência `navigation-testing`,
> que se achava necessária, porque o `NavGraph` é publicamente iterável —
> e as nove supressões que restam em produção passaram a ter a
> justificativa escrita no lugar.
>
> E com mais cinco (#337 a #341): os **documentos de entrega** foram
> reconciliados com o código (o Apêndice C, o guia de CI/CD, o roteiro de
> testes de QA, o plano do redesign e o guia de acessibilidade — todos
> mandavam rodar comandos ou apontar para arquivos que não existem mais),
> o **DEF-79** e o **DEF-80** passaram a registrar o drift que sobrou
> (o relatório técnico da N2 e o roteiro de testes, que descrevem a
> arquitetura com rede removida), e o esqueleto da tela de login e de
> cadastro — o `Scaffold` escrito duas vezes, linha por linha — virou um
> só, o que **zerou de novo a duplicação na produção**.
>
> E com mais seis (#342 a #349): o `FakeTagDao` saiu da lista do que
> ficaria de fora (o `ProjectRepositoryImpl` usa só `findById`, então a
> versão completa do fake serve às duas suítes); a **varredura de
> literais** — strings repetidas entre arquivos, fora de comentário —
> encontrou o tipo de duplicação que a varredura de blocos não
> alcança: o mesmo conhecimento escrito em **duas camadas** (as 17
> chaves do JSON de backup, a categoria do feriado, o nome do argumento
> da rota), sempre com **modo de falha silencioso** e **sem teste que
> pegasse**; e o **DEF-81** registrou a última duplicação da camada de
> teste, que é *entre módulos* e só sai com uma variante `testFixtures`
> em `:core:domain` — mudança de topologia do build, que é decisão do
> responsável.
>
> **O que está aberto hoje** (o *ledger* é a fonte da verdade):
> **DEF-24** (contraste dos empty states — decisão de design),
> **DEF-74** (a semana do Painel não se renova com a tela aberta),
> **DEF-75** (o pepper no `EncryptedSharedPreferences` depreciado),
> **DEF-76** (a sombra rígida da identidade não é adotada),
> **DEF-77** (o diálogo "Editar projeto" e o `DeleteTagUseCase` sem tela),
> **DEF-78**, **DEF-79** e **DEF-80** (a autoavaliação e os documentos de
> entrega da N2 ainda descrevem a arquitetura com rede removida) e
> **DEF-81** (a duplicação entre módulos, que exige `testFixtures`).
> Todos os nove são **decisão do responsável** — de produto, de identidade
> ou acadêmica — e nenhum é corrigível por conta própria sem mexer nessa
> decisão.

> **⚠️ Base pré-transição local.** Esta página foi escrita contra a base que
> ainda tinha camada de rede, flavors e fila `pending_ops`. A direção aprovada
> em 16/10/2026 ([TRANSICAO-LOCAL](TRANSICAO-LOCAL.md)) removeu esse código no
> #218: o diretório `backend-stub/` foi apagado por inteiro e o job
> `backend-integration` não existe mais. Todo item desta página que cita o
> stub, os flavors, `BuildConfig.BASE_URL` ou a fila descreve **código que não
> existe mais** — vale como histórico do diagnóstico, não como backlog. O
> produto é local; a leitura do estado atual está no *ledger*
> ([DEFEITOS](DEFEITOS.md)).

---

## P0 — bugs críticos — **TODOS CORRIGIDOS (verificado em 08/10/2026)**

Status conferido um a um contra o código em `main`
(`316ed41`), não apenas por confiança na seção *Correções aplicadas*.

| # | Local | Problema | Status + evidência em `main` |
|---|---|---|---|
| **P0-1** | `app/.../navigation/BrainOutNavHost.kt:163-177` | `signOut()` em `scope = rememberCoroutineScope()` cancela quando Settings deixa composição → DataStore write interrompido → sessão persiste → próximo cold-start ainda loga. | ✅ **RESOLVIDO** — `BrainOutNavHost.kt:180` obtém `val lifecycleOwner = LocalLifecycleOwner.current`; `:189-192` faz `lifecycleOwner.lifecycleScope.launch { activeUserProvider.signOut(); navigate(...) { popUpTo(0) { inclusive = true } } }`. Import `androidx.lifecycle.lifecycleScope` em `:20`. Não há mais `rememberCoroutineScope` no arquivo. |
| **P0-2** | `app/.../notifications/CompleteTaskWorker.kt:35-48` | `catch (Exception) → Result.retry()` quebra idempotência em task deletada: `IllegalArgumentException` re-retraya para sempre. Contradiz KDoc e nome do teste. | ✅ **RESOLVIDO** — `CompleteTaskWorker.kt:50-62`: caminho feliz `Result.success()`; `catch (e: DomainException)` → `Result.success()` (`:51-56`, comentário "violação permanente — idempotente"); `catch (e: IOException)` → `Result.retry()` (`:57-61`). Import `DomainException` em `:12`. |
| **P0-3** | `core/data/.../security/PepperProvider.kt:48-50` | Pepper derivado de `masterKey.toString()` — SHA-256 de string pública idêntica em todo dispositivo. | ✅ **RESOLVIDO** — `PepperProvider.kt:48-55` cria `EncryptedSharedPreferences` com `MasterKey` AES256_GCM; `:57-59` devolve o valor já persistido; `:60-68` gera `ByteArray(PEPPER_LENGTH_BYTES)` com `SecureRandom().nextBytes()` **uma vez por instalação** e grava em Base64. Não há mais `masterKey.toString()` nem `identityHashCode` no arquivo. |
| **P0-4** | `core/data/.../sync/BrainOutSyncDispatcher.kt:101-107` + `TagSyncPayload` + `TagCreateDto` | DELETE de tag nunca chega ao servidor: cliente envia só `{name, color}`; `DELETE /v1/tags/{localUUID}` → 404 → tag orfã. | ✅ **RESOLVIDO** — `PendingOpEntity.kt:155-160`: `TagSyncPayload` tem `@SerialName("id") val id: String`; `RemoteDtos.kt:124-130`: `TagCreateDto(id: String? = null, ...)`; `RemoteDataSource.kt:226-228` `createTag(name, color, id = null)` repassa `id` ao DTO; `BrainOutSyncDispatcher.kt:111` TAG CREATE usa `id = payload.id` e `:113` DELETE usa `remote.deleteTag(op.entityId)`. |
| **P0-5** | `ci.yml:107,122,217` (`testDebugUnitTest`, `lintDebug`, `connectedDebugAndroidTest`) | Tarefas não-flavor não resolvem em `:app`/`:core:data` → unit tests do `:app` e gate de `MissingTranslation` silenciosamente pulados. | ✅ **RESOLVIDO** — `ci.yml:110` `./gradlew testDevDebugUnitTest`; `:154` `./gradlew lintDevDebug`; `:282` `./gradlew :core:data:testDevDebugUnitTest :app:testDevDebugUnitTest`. Passo `connectedDebugAndroidTest` **removido** do job `backend-integration` (sem emulador + flavor-mismatch). Passos explícitos adicionais: `:118-128` as 4 features (FIX-02) e `:133-135` `:core:ui:testDebugUnitTest` (NB-05). `continue-on-error` não aparece mais em `ci.yml`. |
| **P0-6** | `release-apk.yml:52-56` + `app/build.gradle.kts:24-27,106-110` | Só `BRAINOUT_KEYSTORE_BASE64` é validado → se os 3 secrets de senha faltarem, `.aab` não-assinado sobe como release verde. | ✅ **RESOLVIDO** — `release-apk.yml:47-66` passo *Validate signing secrets (P0-6)* itera `BASE64 STORE_PWD ALIAS KEY_PWD` e faz `exit 1` se qualquer uma vier vazia; `:83-104` passo *Verify .aab signature (P0-6)* roda `jarsigner -verify` sobre o `.aab` gerado e falha o job antes do upload (`:105` `uses: actions/upload-artifact@v4`). |
| **P0-7** | `core/domain/.../error/TagNotFoundException.kt:4` | Estende `RuntimeException`, não `DomainException`. | ✅ **RESOLVIDO** — arquivo reescrito: `class TagNotFoundException(tagId: String) : DomainException("Tag não encontrada: $tagId")`, com KDoc declarando que a extends de `DomainException` permite captura agregada. |
| **P0-8** | `core/domain/.../usecase/ChangeTaskStatusUseCase.kt:59` | `kotlin.error("Tarefa não encontrada: $taskId")` lança `IllegalStateException` cru, fora do mapeamento de domínio. | ✅ **RESOLVIDO** — `ChangeTaskStatusUseCase.kt:66`: `repository.findById(taskId) ?: throw TaskNotFoundException(taskId)`. `TaskNotFoundException` é subclasse de `DomainException`. |

---

## P1 — correções adjacentes / qualidade alta

### App module (`brainOut :app`)

- ✅ **RESOLVIDO (PR #251)** — `MainActivity.kt:87-89` + AGENTS: permission request prompt roda em **toda composição**, não só na Home como documentado. O pedido da permissão saiu da raiz da Activity e passou para o destino Home (DEF-36).
- ✅ **RESOLVIDO** — `MainActivity.kt:152-161` `withContext(Dispatchers.IO) { currentActiveUser }` sem `runCatching` → spinner eterno em DataStore/Room falho no cold-start. A leitura da sessão agora é envolvida em `runCatching`: se falhar, o app segue para o Splash (que pede o login dentro dele) em vez de deixar `startDestination` nulo e a tela presa no `LoadingIndicator`.
- ✅ **RESOLVIDO** — `BrainOutAppSmokeTest.kt:32-52` — `actual == expected` tautologia (sets idênticos). O lado esperado virou literal de propósito: com os dois lados construídos pelas mesmas constantes, o teste nunca falharia e um erro de digitação numa rota passaria batido.
- `DeadlineWorker.kt:104` + `NotificationDismissal.kt:22` — `task.id.hashCode()` como notification ID: colide, pode ser negativo; clamp em `Int.MAX_VALUE`/`0x7FFF_FFFF` ou map estável.
- ⚪️ **OBSOLETO** — `SyncConnectivityWatcher.kt:55-60` — `registerNetworkCallback` dispara `onAvailable` na primeira rede disponível; sync duplica com o `ensurePeriodicSync` 15-min. Pular o primeiro. A camada de sync foi removida no #212.
- ✅ **RESOLVIDO (PR #270)** — `DeadlineReceiver.kt:27-30` + `DeadlineWorker.kt:33-35` — KDoc + manifest mencionam ação "Dispensar" que não é adicionada ao `addAction`. As referências foram removidas: a notificação só tem o botão "Concluir", e o toque no corpo abre o app e dispensa (`setAutoCancel(true)`).
- ✅ **RESOLVIDO** — `notifications/NotificationPermissionStore.kt` + `DeadlineReceiver` carecem de teste Robolectric (goAsync + cancel + dismiss). O store tem `NotificationPermissionRequestTest` (3 casos) e o receiver ganhou `DeadlineReceiverTest` (PR #270, 5 casos: enfileira, cancela, dispensa, ação alternativa e intent sem id).

### core/domain

- ⚪️ **OBSOLETO** — `CanPerformActionUseCase.kt:19-27` — sem `@Inject constructor` e `invoke` não é `suspend`. Quebra template AGENTS duas vezes. O use case foi removido no PR #226 por não ter caller de produção; o gate de papel entrou pelo `ActiveUserPort` + `requirePermission` (PR #264).
- ✅ **RESOLVIDO (PR #269)** — `Task.transitionTo` + `CreateTaskUseCase.scheduleReminder` + `ChangeTaskStatusUseCase.reconcileReminder` + `UpdateTaskUseCase.reconcileReminder` usam `Instant.now()` direto (não injetável → RN03 não-testável; `TaskTest` **não afirma** populating/clearing em transições). O carimbo é injetável (`Task.transitionTo(target, now)`, como o `now` de `Task.create`), os use cases de lembrete passam pela regra única `ReminderReconciler` (que não carimba nada) e o `TaskTest` afirma o carimbo e o apagamento.
- ⚪️ **OBSOLETO** — `TaskPriority.fromCodeOrThrow` é alias morto de `fromCode`. Remover ou documentar e estender. O alias não existe mais no código.
- ⚪️ **OBSOLETO** — `TaskPriority.VALID_CODES` + `LOW_CODE..CRITICAL_CODE` são dead constants. `VALID_CODES` está em uso (`TaskRepositoryImpl` e `DashboardViewModel` normalizam a contagem por ele) e as constantes `LOW_CODE..CRITICAL_CODE` não existem mais.
- ✅ **RESOLVIDO** — `TaskRepository.kt` KDoc vaza "Room" / "invalidação do Room" / "@Transaction" para o domínio. A porta documenta o **contrato** ("na mesma transação", "mudança relevante nos dados"); o mesmo vale para `ChangeTaskStatusUseCase`, `DeleteProjectUseCase`, `DeleteTagUseCase`, `CreateTagUseCase` e `CreateTaskUseCase`. O KDoc do `completeAndCascade` ainda prometia `IllegalArgumentException` quando a tarefa não existe — a implementação lança `TaskNotFoundException`; o texto foi corrigido.
- `SortOrder.fromStorageKey` tem dois branches com o mesmo fallback (`CreatedDesc`).
- ✅ **RESOLVIDO** — `UpdateTaskUseCase.kt:50-53` prossegue silenciosamente quando `findById` retorna null → RN02 não roda. O repositório é quem falha: `TaskRepositoryImpl.update` lança `TaskNotFoundException` quando a tarefa não existe, então o "silêncio" nunca chega a happen — o caso de uso propaga a falha de domínio.
- 4 use cases **sem teste** (gap conhecido, AGENTS): `CreateTagUseCase`, `UpdateProjectUseCase`, `DeleteProjectUseCase`, `DeleteTagUseCase`. Cada um é 1-line delegate mas zerar cobertura derruba o piso Kover.
- ✅ **RESOLVIDO (PR #269)** — `TaskPriorityRulesTest.update integral` duplica cenário `change priority from DONE is blocked` e nem exercita `copy`. O cenário duplicado saiu; o atalho `copy` que o modelo não bloqueia passou a ser coberto no `UpdateTaskUseCaseTest`, junto do caso de uso que o barra.

### core/data

- ❌ **NÃO É REDUNDÂNCIA** — `DataModule.@Provides` para classes já `@Inject @Singleton`: `UserRepositoryImpl`, `ProjectRepositoryImpl`, `TaskRepositoryImpl`, `TagRepositoryImpl`, `HolidayRepositoryImpl`, `PasswordHasherImpl`. Cortar. O `@Provides` **é** a ligação da interface com a impl (`UserRepository` → `UserRepositoryImpl`); o construtor `@Inject` da impl é o que a constrói. Tirar o `@Provides` quebra a resolução da interface — a forma idiomática seria `@Binds` num módulo abstrato, o que é mudança de estilo, não remoção de redundância.
- ⚪️ **OBSOLETO** — `BrainOutDatabase.kt:70` `const val NAME = DATABASE_NAME` é alias morto. O alias não existe mais; só `DATABASE_NAME` sobreviveu.
- ✅ **RESOLVIDO (PR #288)** — Kover exclusion `*.remote.*` em `build.gradle.kts:87` mascarava `RemoteDataSource` (error prop) e `HolidayRemoteDataSource` (cache+mutex) — não contava pra 60%. Com a camada de rede removida no #212, os DTOs sumiram: as exclusões `*Dto`/`*Dto$*` e o comentário de nove linhas que as descrevia saíram do `build.gradle.kts` (não havia mais o que excluir) e o `AGENTS.md` deixou de citar `*.remote.*`.
- ⚪️ **OBSOLETO** — `RemoteDataSource.kt:48-53 vs :56-175` — `ping()` engole Exception; outros relançam. Inconsistente. `RemoteDataSource` não existe mais.
- ⚪️ **OBSOLETO** — `RemoteDataSource.kt:31-33` — `HttpLoggingInterceptor.Level.BASIC` sempre on, inclusive release. Loga URL em prod. A camada de rede saiu no #212.
- ⚪️ **OBSOLETO** — `RemoteDataSource.kt:50-60` — `catch (Exception)` engole `CancellationException`. BrainOutSyncDispatcher já corrigiu; alinhar. A camada de rede saiu no #212.
- ✅ **RESOLVIDO** — `ProjectRepositoryImpl.create/update` + `TaskRepositoryImpl.delete` — `@Transaction` aninhado (`enqueueInTx`→`replaceProjectTags`/`cascade*`) depende de SQLite savepoints. Sem teste de roll-back. A fila `enqueueInTx` saiu com a camada de rede; o que restou são as cascatas, e elas têm teste (`TaskRepositoryImplTest` e `ProjectRepositoryImplTest` cobrem `cascadeCompleteTask`, `cascadeReopenTask` e `cascadeDeleteTask`).
- ✅ **RESOLVIDO** — `ProjectRepositoryImpl.kt:136-138` — `require(tag.ownerId == ownerId)` lança `IllegalArgumentException` cru; criar `TagOwnershipException`. A exceção de domínio `TagOwnershipException` existe e é lançada pelo repositório.
- ⚪️ **OBSOLETO** — `UserDao.deleteAll()` + `count()` mortos em prod (`@VisibleForTesting` ou remover). Os dois métodos não existem mais no DAO.
- ⚪️ **OBSOLETO** — `TaskRepositoryImpl.delete` enfileira DELETE op mesmo quando task já deletada por cascade. A fila de operações saiu com a camada de rede no #212.
- ⚪️ **OBSOLETO** — `RemoteDtos.kt:101-109` + `RemoteDataSource.kt:80-88` — `ProjectCreateDto.id: String? = null`; kotlinx-serialization omite null do PUT body → stub rejeita com 400 (path/body mismatch). Id obrigatório. Os DTOs remotos saíram no #212.
- ⚪️ **OBSOLETO** — `Documented updated_at / last-writer-wins` ausente do data layer e stub. Zero colunas `updated_at` em entidades/payloads/stub. PUT é blind upsert. Resolver antes de multi-device. O modelo de multi-dispositivo ficou fora do escopo do produto local; o `PUT` idempotente era do backend removido.
- ⚪️ **OBSOLETO** — `ConnectivityObserver.kt:73,83` — `connectivityManager.activeNetwork` deprecated API 29+. A observação de conectividade saiu com a camada de sync no #212.
- ✅ **RESOLVIDO** — `PasswordHasherImpl.verify` ignora `parsed.iterations` (campo morto). Latente. O `verify` hoje lê as iterações do próprio hash armazenado (`alg$iter$salt$hash`) e valida a faixa — um hash gravado com iterações diferentes verifica corretamente.
- ⚪️ **OBSOLETO** — `HttpLoggingInterceptor` em release (mesmo finding acima). A camada de rede saiu no #212.

### core/ui

- ✅ **RESOLVIDO** — `ThemeSelectionTest.kt:86-120` é tautológico (não invoca `BrainOutTheme` nem `isSystemInDarkTheme()`). O teste passou a exercitar a função de escolha (`resolveNeoThemeSpec(darkTheme = …)`) — a costura que o `BrainOutTheme` usa — com a configuração de night mode forcada nos dois sentidos.
- ✅ **RESOLVIDO** — `ContrastRatioTest` cobre subset apenas: faltam `outline`/`surfaceVariant`, `onSurface`/`surfaceVariant`, error/background, inverseSurface. Hoje são **29 pares** declarados num único lugar (`NeoContrastRatioTest`), com um teste que fixa a contagem (29, sendo 23 com mínimo 4.5 e 6 com 3.0) para que um "conserto" por remoção seja percebido. O `EmptyStateContrastTest` fixa o outro lado do invariante: o `outline` puro passa de 3:1 e o `surfaceVariant` **não** alcança (é preenchimento, não fronteira — ver DEF-24).
- ⚪️ **OBSOLETO** — Dynamic color path (`Theme.kt:114-116`) sem teste e sem garantia WCAG AA. O caminho de cor dinâmica saiu: a identidade Neo é fixa (`dynamicColor = false` por padrão) e quem valida o contraste é o `NeoContrastRatioTest`.
- 🟡 **PARCIAL** — `Shape.kt` contrato violado por 23 hardcoded `RoundedCornerShape(...)` em `feature/*` — sem enforcement. Restam **9** (eram 23): o PR #263 adotou o token Neo em todos os casos de valor idêntico, e o que sobrou é raio de 12dp, que não tem token equivalente (decisão de design, registrada no `AGENTS.md`).

### feature/auth

- ✅ **RESOLVIDO** — `AuthViewModel.kt:100-108, 188-194` — `emailError`/`passwordError` **e** `errorMessage` setados com a mesma string → banner + `supportingText` mostram a mesma mensagem em duplicidade. Os erros de campo não vão mais para `errorMessage`: o banner ficou reservado para os erros de domínio que não são de campo.
- ✅ **RESOLVIDO** — `AuthViewModel.kt:51` `MutableSharedFlow()` default (replay=0, buffer=0, SUSPEND) → `NavigateHome` emitido durante rotation pode perder-se. O evento passou a `Channel<AuthEvent>(BUFFERED)` + `receiveAsFlow()`: o `NavigateHome` não se perde entre composições nem em config change.
- ✅ **RESOLVIDO** — `AuthViewModel.kt:109, 128, 170, 196` emite `AuthEvent.FocusField` + `AuthField` enum — dead code, não consumido; UI usa `LaunchedEffect(uiState.*Error)`. O enum e o evento saíram; o foco é state-driven.
- ✅ **RESOLVIDO** — `AuthUiState.MIN_PASSWORD_LENGTH = 8` enforçado só no VM, não em `CreateUserUseCase`/`AuthenticateUserUseCase` (apenas `isNotBlank()`). Parity gap. A constante local foi removida e a do domínio passou a ser a autoridade única: os dois casos de uso exigem `length >= MIN_PASSWORD_LENGTH`.
- ✅ **RESOLVIDO (DEF-49)** — `LoginScreen.kt:246-270` `resolveAuthMessage` `else -> message` vaza domain PT raw em locale EN (`InvalidModelException` no catch `DomainException`). O ViewModel entrega o **tipo** (`AuthErrorType`) e a UI escolhe o texto pt/en: nenhuma mensagem técnica do domínio chega à tela.
- ✅ **RESOLVIDO** — `LoginScreen.kt:3-4` comentário referencia `SessionViewModel` em `:feature:auth` que **não existe**. O comentário saiu.
- ✅ **RESOLVIDO** — `AuthViewModel.reset()` morto (nunca invocado). O método saiu.
- ✅ **RESOLVIDO** — `LoginScreen.kt:78` `onLoginSubmit` (mesmo em RegisterScreen) deveria ser `onLoginSuccess` — nome mente. O callback só dispara no evento `AuthEvent.NavigateHome`, ou seja, depois do login bem-sucedido — o próprio KDoc dizia "após login/cadastro bem-sucedido". Renomeado para `onLoginSuccess` na tela, no NavHost e no teste instrumentado; o irmão `onRegisterSubmit` foi renomeado para `onRegisterSuccess` pelo mesmo motivo. (O `RegisterScreen` não tinha esse problema de nome no KDoc, mas tinha o callback homônimo.)
- `feature/auth/res` contém 4 strings `home_*` **duplicadas** em `feature/projects/res` — drift risk → eventual `Duplicate resources`. ✅ **RESOLVIDO** — `feature/auth/src/main/res/values/strings.xml` tem hoje **43 chaves, todas com prefixo `auth_`/`login_`/`register_`/`splash_` e zero `home_*`**; as 4 cópias (`home_role_member_dialog_title/body/ack`, `home_fab_disabled_owner`) foram removidas.
- ✅ **RESOLVIDO (PR #280)** — Sem `RegisterScreenTest` + `SplashScreenTest`. O `SplashScreenTest` existe e o `RegisterScreenTest` (Robolectric, 5 casos) cobre a tela de cadastro inteira: o erro no campo, o foco voltando ao primeiro campo inválido, senhas diferentes barrando o envio, o cadastro inválido sem navegar e o cadastro válido persistindo a sessão e pedindo a navegação.

### feature/projects

- ✅ **RESOLVIDO** — `HomeViewModel.buildHomeUiState` linhas 380-385 — `tags = inputs.tagChips` (todas tags do owner) atribuído a **cada** ProjectCard → cards mostram tags erradas. DAO tem `observeTagsFor`; falta port `TagRepository.observeForProject`. A port `TagRepository.observeByProjectIds(ids): Flow<Map<String, List<Tag>>>` existe e cada card lista só as tags do seu projeto.
- ✅ **RESOLVIDO (DEF-45)** — `HomeViewModel.createProject/updateProject/deleteProject/createTag` (`HomeViewModel.kt:419,433,445,459`) gravam em `_errorMessage` que **não é observado** por `HomeScreen` (UI só lê `uiState.errorMessage` de Room). CRUD falha silenciosamente. O canal de erro passou a ser a única fonte e a tela combina `actionError ?: listState.errorMessage` — a falha de CRUD chega ao banner.
- ✅ **RESOLVIDO** — `DeleteProjectDialog` usa `R.string.project_detail_description` (descrição do app) como corpo de confirmação. Texto errado. O diálogo usa `project_detail_delete_confirm_title`/`_body` (pt+en).
- ✅ **RESOLVIDO** — `NewTaskDialog.onConfirm = { viewModel.addTask(...); showCreateDialog = false }` — dialog fecha antes da validação; erro perde-se. O `onSuccess` do caso de uso é que fecha o diálogo: a validação acontece antes e o erro fica na tela.
- ✅ **RESOLVIDO** — `AddTagDialog` passa color raw; `Tag.requireValidColor` lança, catch em `_errorMessage` (não exibido). O erro de cor inválida agora chega à tela (o canal de erro é observado) — e o diálogo já envia uma cor válida.
- ✅ **RESOLVIDO** — `DeadlineField.kt:47` hardcoded `ofPattern("dd/MM/yyyy")`; `R.string.project_detail_new_task_due_date_format` (MM/dd/yyyy em EN) existe mas nunca referenciado. EN vê formato pt. O padrão vem do recurso localizado, então o `en` mostra `MM/dd/yyyy`.
- ✅ **RESOLVIDO (PR #272)** — `R.string.home_fab_disabled_label`, `project_detail_back`, `project_detail_new_task_due_date_picker_title`, `project_detail_new_task_due_date_format`, `project_detail_task_priority_locked_info` são chaves mortas. Duas (`project_detail_new_task_due_date_picker_title` e `project_detail_task_priority_locked_info`) já não existiam em nenhum `strings.xml`; uma (`project_detail_new_task_due_date_format`) foi resgatada e é consumida por `DeadlineField.kt`. As duas últimas cópias mortas viviam em `:app` (`home_fab_disabled_label` e `project_detail_back`, zero referências) e foram removidas junto com as demais strings duplicadas do `:app`.
- ✅ **RESOLVIDO (DEF-22/DEF-23)** — `HomeScreen.kt:843, 921` ícones errados (`Add` como checkmark, `Sort` como leading de search). O marcador de "selecionado" é o check (com `stateDescription` para o TalkBack) e o ícone de ordenação é o de sort.
- ✅ **RESOLVIDO** — `ProjectDetailBody` usa `verticalScroll` + `forEach` em vez de `LazyColumn` — jank com muitas tasks. A lista virou `LazyColumn` com chave por `task.id`.
- ✅ **RESOLVIDO** — `HomeScreen.kt` + `ProjectDetailScreen.kt` Pré-visualizações dependem de `hiltViewModel()` default, não compilam no preview sem Hilt. As previews apontam para os corpos (`HomeProjectsContent`, `ProjectDetailBody`), que recebem estado de exemplo; o motivo está escrito no ponto.
- ✅ **RESOLVIDO (DEF-72, PR #261)** — `DefaultHomeUser` hardcode "João Pedro"/"JP" flashing em Loading/SignedOut. A `HomeTopBar` aceita `user: HomeUser?` — `null` significa "sem sessão": sem avatar, sem badge e a saudação genérica.
- ✅ **RESOLVIDO (DEF-25)** — `renameTask` em `ProjectDetailScreen.kt:314` tem `@Suppress("unused")` — fio morto. A decisão foi **ligar**, não remover: o parâmetro ganhou tipo `(Task) -> Unit` e passou a ser acionado por um `RenameTaskDialog` novo.

### feature/tasks

- ✅ **RESOLVIDO** — `clearError()` quebrado em ambos VMs. Testes verificam só `viewModel.errorMessage.value`, nunca `uiState.errorMessage` → banner nunca dismiss. O `uiState` combina o canal de erro e o `clearError()` zera o banner.
- `feature/tasks` é **read-only** — zero CRUD no escopo R7-R8 (sem `transitionTo`, `changePriority`, `dueDate` display).
- ✅ **RESOLVIDO** — `PriorityBarsCanvas` raw `cornerRadius = CornerRadius(8f, 8f)` em pixels, density-dependent. O raio passou a vir do token `NeoRadii.chip` (4dp), que escala com a tela e vale exatamente os mesmos 8px na densidade xhdpi.
- ✅ **RESOLVIDO** — `PriorityChart` contrato frágil: `barColors[index]` + `counts.size` divisor sem `require`. Empty list → NaN. Rótulo e cor agora são lidos com guarda (`zip` e `getOrElse`) e o divisor tem piso 1; dois testes novos cobrem a lista vazia e a lista maior que o número de níveis.
- ✅ **RESOLVIDO** — `TasksViewModel.toRow` rebuilds `associateBy` per task (O(n*m)). Hoist. O mapa é montado uma vez por lista.
- ✅ **RESOLVIDO** — Nested `flatMapLatest` re-subscribes projects flow on every task emission → use `combine`. A assinatura do Room acontece quando os **inputs** mudam (preferências, texto debounceado, tags e filtro), não a cada emissão de tarefa.
- 🟡 **PARCIAL (DEF-74)** — `DashboardViewModel.currentWeekStartMillis()` avaliado por subscription; `Instant.now()` no `Clock` injectable → week-rollover stale + untestable. O instante passou a vir de um `java.time.Clock` injetado (`provideClock` no `DataModule`), o que torna o cálculo determinístico e testável, e a semana é recalculada quando a tela é reaberta (o `WhileSubscribed(5_000)` solta o fluxo antigo). O que continua de pé: uma tela **aberta** atravessando a virada de segunda-feira mostra a semana antiga — um fluxo periódico resolveria, mas quebra o `advanceUntilIdle` dos testes.
- ℹ️ **POR DECISÃO** — UTC Monday hardcoded (en expects Sunday). A semana UTC é a regra: a contagem é a mesma entre sessões e dispositivos, independentemente do fuso. O motivo está no KDoc do cálculo.
- ✅ **RESOLVIDO (DEF-22)** — `AssistChip(onClick = {})` decorativo em PriorityChip/StatusChip — ainda focusável, TalkBack engana. O `NeoInfoChip` não tem parâmetro `onClick`: quem precisa de chip acionável usa `FilterChip` (seleção) ou um botão.
- ✅ **RESOLVIDO (DEF-26)** — `TasksErrorBanner(message: String)` + `DashboardErrorBanner` suprime `message` (`@Suppress("UNUSED_PARAMETER")`) — sempre mostra a string canônica. O banner de tarefas mostra a mensagem; o do Painel segue a regra canônica de propósito.
- ✅ **RESOLVIDO** — `retryToken` público em ambos VMs. O token vive no `TasksLoadState` (classe interna) e o ViewModel expõe só `retry()`.
- ℹ️ **POR DECISÃO** — `tasksGraph()` + `dashboardGraph()` split em `TasksRoutes.kt` — fácil esquecer dashboard. A feature é a dona das rotas que usa (é a única que expõe extensões de grafo) e o `AGENTS.md` registra a regra.
- ✅ **RESOLVIDO** — `TasksViewModel.NO_PROJECT_LABEL = "(sem projeto)"` hardcoded PT em VM companion. A constante não existe mais.
- ✅ **RESOLVIDO** — `TasksViewModel.ERROR_LOAD_FAILED` / `DashboardViewModel.ERROR_LOAD_FAILED` hardcoded PT (UI usa `R.string.tasks_error_load_failed`). As constantes não existem mais.
- ✅ **RESOLVIDO** — Pré-visualizações sem `BrainOutTheme { Surface { ... } }`. As três previews do módulo de tarefas embrulham o conteúdo em `Surface(color = MaterialTheme.colorScheme.background)` — o mesmo fundo que a `Scaffold` da tela produz em produção. Antes, o preview pintava o fundo do IDE, e não o do tema.
- ✅ **RESOLVIDO** — `TasksTestTags.LOADING/EMPTY/LIST` dead (composables não recebem `testTag`). As três chaves passaram a ser aplicadas em `TasksContent` (a lista, o indicador de carregamento e o empty state), e três testes novos as exercitam; no dashboard as equivalentes (`DashboardTestTags.LOADING` e `EMPTY`) já eram aplicadas por `DashboardLoading` e `DashboardEmptyState`.
- ✅ **RESOLVIDO** — Sem `TasksScreenTest` + `TasksRoutesTest`. O `TasksScreenTest` existe; o `TasksRoutesTest` (novo, PR #335) monta o grafo de verdade e cobre as rotas que a feature expõe (`tasksGraph()` e `dashboardGraph()`).
- ✅ **RESOLVIDO** — `abortOnError = false` em `feature/tasks/build.gradle.kts:57` esconde mais que só `Instant.parse` — fix com `@SuppressLint` ou baseline, restaurar gate `MissingTranslation`. O `Instant#parse` do preview deixou de ser problema quando o módulo passou a usar `coreLibraryDesugaring` (DEF-63); o lint do módulo roda com zero erros e o gate voltou a ser rígido, com `MissingTranslation` promovido a erro.

### feature/settings

- ✅ **RESOLVIDO (PR #272)** — `app/src/main/res/values*/strings.xml:58-66` **duplica** as 9 chaves `settings_*` já em `feature/settings/res` — dual source of truth, eventual `Duplicate resources`. Deletar de `:app`. As 9 chaves saíram do `:app` (que foi de 55 para 8 strings) junto com as outras 38 cópias de feature que lá estavam.
- ⚪️ **OBSOLETO** — `SettingsRoutes.SettingsPattern` constante morta. NavHost usa `BrainOutRoutes.Settings`. Remover ou tornar `private`. A constante não existe mais.
- ❌ **NÃO É VERDADE** — Build.gradle.kts tem `lifecycle-viewmodel-compose`, `hilt-android`, `hilt-navigation-compose`, KSP mas **não tem** `SettingsViewModel` — todas dead deps. O `BackupViewModel` (PR #214) usa `hiltViewModel()` (de `hilt-lifecycle-viewmodel-compose`, desde o PR #296) e é um `@HiltViewModel` (Hilt + ksp): as dependências existem porque são usadas. O que não existe é um `SettingsViewModel` — e ele não é necessário: só o backup tem estado próprio.
- ✅ **RESOLVIDO** — `SettingsScreen.kt:174-178` `@Preview` sem `BrainOutTheme { Surface { ... } }`. A preview renderiza a tela inteira, e a tela pinta o próprio fundo (`Scaffold(containerColor = MaterialTheme.colorScheme.background)`) — o preview mostra o fundo Neo. O `Surface` vem de dentro da tela, e não do preview.
- ✅ **RESOLVIDO (DEF-44, PR #252)** — `Profile`/`Notifications`/`Theme` cliques são no-op silenciosos (BrainOutNavHost L179-183). No Snackbar/Toast — broken UX. As linhas de placeholder agora respondem "Em breve" num snackbar (`SettingsRowKind.Placeholder`).
- ✅ **RESOLVIDO** — `SettingsStructure.kt:4-6` comentário envelhece mal ("não precisa de ViewModel próprio"). Reavaliar em E2.6+. O comentário foi corrigido: só as linhas de backup têm estado próprio (o `BackupViewModel`).
- ℹ️ **POR DECISÃO** — `FEATURE_SETTINGS_PACKAGE` constante não referenciada fora do próprio arquivo. É a convenção de marcador de módulo (o `AGENTS.md` registra que ela existe nos 8 módulos e o Kover a exclui).

### backend-stub

> ⚪️ **REMOVIDO** — o diretório `backend-stub/` foi apagado por inteiro no #218
> (commit `05acf32`), junto com a camada de rede, a fila `pending_ops` e o job
> `backend-integration` que o exercitava. Os achados abaixo descrevem arquivos
> que não existem mais: valem como histórico do contrato `/v1/*`, não como
> backlog. A seção "Correções aplicadas (2026-09-24)" deste arquivo, que
> menciona o stub, é um registro datado de antes da remoção.

- `smoke_e2e.py:18-23` — quando `want_status is None`, `return 200, json.loads(out)` hardcoda 200 → asserts `code == 200` são **tautológicos**. Toda POST/PUT/GET (lines 35, 41, 48, 68, 71, 78, 89) aceita 4xx/5xx silencioso.
- `server.py:96-110` — `TaskIn`/`TaskUpsert.project_id: str` sem UUID validator → 404 (project not found) em vez de **400** exigido por §5.1.
- `server.py:65-72 vs 61-62` — `_normalize_id` (body) aceita hex-32 + canonical; `_require_uuid` (path) só canonical. PUT com id hex-32 no path → 400 mesmo UUID lógico.
- `server.py:142-145` — `datetime.utcnow()` deprecated Python 3.12.
- `server.py:174-197, 290-310` — POST idempotente ignora payload divergente (devolve existing). Drift silencioso. 409 ou update quando payload differs.
- `server.py:78-125` — `Project`/`Task`/`Tag` response models nunca bound (`response_model=` ausente). OpenAPI drift vs RemoteDtos.
- Endpoints sem teste: `GET /v1/projects/{id}`, `POST /v1/tasks`, `GET /v1/tasks/{id}`, validation 422 boundaries (color/name/title max_length), PUT body id mismatch em tasks.

### CI / Gradle / docs

- `ci.yml:123, 218` `continue-on-error: true` esconde falhas reais (lint, instrumented). ✅ **RESOLVIDO** — nenhuma ocorrência de `continue-on-error` restou em `ci.yml`.
- `ci.yml:148-217` `backend-integration` sem emulator + sem `setup-java` → `connectedDebugAndroidTest` sempre morre, masked by `continue-on-error`. ✅ **PARCIALMENTE RESOLVIDO** — o job agora provisiona JDK 21 (`ci.yml:188-191`) e trocou o teste instrumentado por `testDebugUnitTest` (`:124`) mais as suítes explícitas de `:feature:*` e `:core:ui` (`:130-138`); **permanece** a suíte instrumentada fora do CI (sem emulador no runner) — ver DEF-13 em `docs/DEFEITOS.md`. (A auditoria citava `testDevDebugUnitTest`, task que não existe desde a saída dos flavors no #212.)
- **Versões de actions**: `upload-artifact` estava em `@v7` (inválido) em 24/09; ✅ **corrigido — todos os 5 usos de `actions/upload-artifact` estão em `@v4`** (`ci.yml:71,158,170,286`, `release-apk.yml:105`). Restam `@v6`/`@v5` **válidos** em `pages.yml`: `actions/configure-pages@v6:39`, `actions/upload-pages-artifact@v5:79`, `actions/deploy-pages@v5:94` — são actions do Pages com majors próprios, não inválidas. Os demais usos são `@v4` (`checkout`, `setup-java`, `cache`, `github/codeql-action/*`).
- `codeql.yml:41` `build-mode: none` para Java/Kotlin (compiladas) → análise vazia. ✅ **PARCIALMENTE RESOLVIDO** — `build-mode` agora é `autobuild` (comentário explicativo no arquivo); `continue-on-error: true` **permanece** no job (linha 26), então a análise continua não-bloqueante.
- ✅ **RESOLVIDO** — JDK drift: CI 17 vs release 21 vs backend-integration sem JDK; cache key única compartilhada entre JDKs (cache pollution). Todos os workflows (`ci.yml`, `release-apk.yml`, `release-apk-debug.yml`) provisionam **JDK 21**.
- ✅ **RESOLVIDO** — `core/data/build.gradle.kts:118-121` `abortOnError = false` esconde `MissingTranslation`/NewApi do data layer. O módulo usa `abortOnError = true` desde o DEF-63 (desugaring); `MissingTranslation` não é promovido porque o módulo não tem string de interface.
- ✅ **RESOLVIDO** — `feature/tasks/build.gradle.kts:57` `abortOnError = false` esconde lint da feature. O gate voltou a ser rígido na mesma linha em que o desugaring (DEF-63) eliminou o `NewApi` do preview.
- ✅ **RESOLVIDO** — a exclusão `*.remote.*` do Kover mascarava código de produção (`RemoteDataSource` e o mutex do `HolidayRemoteDataSource`). Com a camada de rede removida no #212, os DTOs sumiram: as exclusões `*Dto`/`*Dto$*` e o comentário que as descrevia saíram do `build.gradle.kts` — não havia mais o que excluir.
- ℹ️ **POR DECISÃO** — `gradle.projectsEvaluated` hook frágil (string matching, late evaluation). O hook amarra o `:core:domain:test` a todo `test*UnitTest` Android (sem ele, a CI não roda os testes do domínio quando chamada por um módulo Android). Manter.
- ⚪️ **OBSOLETO** — Common Android block (compileSdk/minSdk/javaVersion/jvmTarget/buildTypes) duplicado ×7 módulos. Detekt block duplicado ×8. `missingDimensionStrategy` ×4. `testOptions.unitTests.isIncludeAndroidResources` duplicado em `:app`. A duplicação de flavor (`missingDimensionStrategy`) sumiu com os flavors (#212); a de `testOptions` foi desfeita — o `:app` declara o bloco uma vez.
- ⚪️ **OBSOLETO** — `local.properties` leitura duplicada (`:app:32-38` + `:core/data:19-24`). Nenhuma leitura manual sobrou: o AGP resolve o SDK por conta própria.
- ⚪️ **OBSOLETO** — `:app:139-143 E 165-169` `testOptions` block declarado duas vezes — segundo silenciosamente override. O `:app` declara `testOptions` uma vez.
- ✅ **RESOLVIDO** — `composeUiTest = "1.7.5"` em `libs.versions.toml` era alias morto, não referenciado. Removido: as libs de teste de Compose são gerenciadas pelo BOM.
- ⚪️ **OBSOLETO** — `README.md:17` link para `./docs/RELATORIO-TECNICO.md` quebrado. O link está correto e o arquivo existe.
- ⚪️ **OBSOLETO** — `ARQUITETURA.md:99,100,103` (stack table stale), `:243` (path `network/`), `:193-197` (AD-1/2/4/5 "a definir"). O documento foi reenquadrado em 16/10/2026 como **registro da arquitetura legada** (removida no #212), com o ponteiro para `TRANSICAO-LOCAL.md` no topo — as linhas citadas descrevem a arquitetura legada de propósito, não a atual.
- `ROADMAP.md:623` + `CONTRIBUTING.md:106` placeholder "GitHub Projects (link a adicionar)".
- ✅ **RESOLVIDO** — `CONTRIBUTING.md:63` exemplo `Kotlin 2.0.21` stale. O exemplo e o texto ao lado citam a versão do catálogo: Kotlin 2.4.21, AGP 9.4.1 e Gradle 9.8.1.
- ✅ **RESOLVIDO** — `keystore.properties.example` era stale: o build ignora o arquivo e a assinatura vem das variáveis `BRAINOUT_KEYSTORE_PATH`/`PASSWORD`, `BRAINOUT_KEY_ALIAS`/`PASSWORD` (o próprio exemplo dizia "para builds de release via GitHub Actions" — mas o arquivo não funciona nem local). O exemplo foi removido: ele prometia um caminho que não existe.

### Kover 60% gate em risco

P0+P1 uncovered LoC ≈ 550 :core:data + 60 :core:domain + ~300 feature. Sem testes pra `TaskRepositoryImpl`/`ProjectRepositoryImpl`/`TagRepositoryImpl`/`BrainOutSyncDispatcher`/`PepperProvider`, porta do gate cai bem abaixo de 60%.

---

## P2 — refactor técnico, decompor quando conveniente

- ✅ **RESOLVIDO** — **Decompose:** `HomeScreen.kt (1403)`, `ProjectDetailScreen.kt (1027)`, `DashboardScreen.kt (593)`, `HomeViewModel.kt (583)`, `ProjectDetailScreen.kt (1027)`. Extrair `*TopBar`, `*BottomBar`, `*Fab`, `*OfflineBanner`, `*States`, `*FilterRow`, `*SearchFilter`, `*EmptyStates`, `*Card`, `*Dialogs`, `*TestTags` em arquivos de ≤200 LoC. A Home foi de 1403 para **176 LoC** (`HomeCreateProjectDialog` 289, `HomeListingPipeline` 277, `HomeViewModel` 216, `HomeFilterRow` 216, `HomeProjectsContent` 213, `HomeActions` 166, `HomeSearchBar` 133); o maior arquivo do projeto tem 447 linhas (`ProjectDetailViewModel`, 268 de código).
- ⚪️ **OBSOLETO** — **Mover `resolveAuthMessage`/`resolveHomeErrorMessage`/`resolveProjectDetailMessage`/`resolveCreateProjectNameError`** para um `ui/common/Messages.kt` (atualmente espalhados por feature). A refatoração que substituiu os `resolve*` por **tipos de erro** por feature (`AuthErrorType`, `HomeErrorType`, `ProjectDetailError`) deixou de precisar de um arquivo comum: um `ui/common` teria de conhecer os tipos de erro de cada feature — o oposto do desacoplamento. `resolveAuthMessage` e `resolveHomeErrorMessage` não existem mais.
- ⚪️ **OBSOLETO** — **`SettingsRoutesTest`, `ProjectsRoutesTest`, `AuthRoutesTest`** — todos fracos. Expandir para validar estabilidade cross-module ou deletar. Nenhum dos três existe no código.
- ✅ **RESOLVIDO (DEF-71, PR #258)** — **`AuthViewModel.submitLogin` etc** — adicionar `runCatching { withContext(Dispatchers.IO) { ... } }` (CPU-bound hash roda em Main hoje). O KDF saiu da thread principal: `PasswordHasher.hash` é `suspend` e a implementação (`PasswordHasherImpl`) roda o PBKDF2 em `Dispatchers.IO`, com o dispatcher injetável para o teste.
- ⚪️ **OBSOLETO** — **`RemoteDtos.id`** — explícito required em tasks/projects (R6). Os DTOs remoto saíram com a camada de rede no #212.
- ⚪️ **OBSOLETO** — **Refatorar `core/data` + `core/domain` ParaConstantsHelper** — extrair `CHECK_DEADLINE_PRE_WINDOW_DAYS`, `REMINDER_OFFSET_HOURS` para shared constants. As duas constantes não existem mais: o `UseCaseConstants` do domínio tem hoje só `MAX_ACTIVE_TASKS_PER_PROJECT`, e o `core/domain/AGENTS.md` foi corrigido (ele ainda citava `REMINDER_OFFSET_HOURS`).
- ✅ **RESOLVIDO** — **Composition cleanup:** `HomeScreen.kt:1088` `AssistChipDefaults.assistChipBorder(enabled = true, ...)` default redundante — o `assistChipBorder` não existe mais no código.
- ✅ **RESOLVIDO (PRs #304, #306, #307, #316, #318)** — **As receitas visuais estavam escritas dentro das telas.** Uma varredura de blocos duplicados (janelas de 5 linhas normalizadas, entre arquivos distintos) achou **11** blocos realmente repetidos, todos com a mesma raiz: a receita do design system vivia nas features, e não no `:core:ui`. Cada uma virou um componente ou um modifier com nome:

  | PR | O que estava copiado | Para onde foi |
  |---|---|---|
  | #304 | A cor da barra superior (4 telas) | `neoTopAppBarColors` |
  | #306 | O cartão (3 telas) | `NeoCard` |
  | #307 | A estrutura do `AlertDialog` (5 diálogos) e o botão "Fechar" (5) | `ProjectDialog` |
  | #316 | O botão de voltar (2 features) e a string `common_back` | `NeoBackButton` + `neo_back` |
  | #318 | A receita da página (6 telas, 3 módulos) | `neoPageGutter` |

  A string `common_back` era a **única** string de interface duplicada entre módulos do repositório inteiro — e ela ia para o `:core:ui` de qualquer jeito, que já é dono das strings compartilhadas da identidade. A varredura refeita depois da rodada (#325) devolve **zero** blocos duplicados: o último — o cartão de linhas de Configurações — virou `SettingsCard`.
- ✅ **RESOLVIDO** — **`@file:Suppress("TooManyFunctions", "MagicNumber", "LongMethod")`** suprime sinais de bloat (corrigir decompondo, não suprimindo). A supressão em bloco saiu, o `MagicNumber` deixou de ser suprimido e **não há mais nenhuma supressão de `LongMethod` no projeto** — a última, a do formulário de cadastro, saiu no #334, quando a função foi decomposta em `RegisterIdentityFields`, `RegisterPasswordFields` e `RegisterActions` (122 → 52 linhas). Restam **nove supressões pontuais em produção, cada uma com a justificativa escrita no próprio lugar**: `LongParameterList` × 2 (os construtores do Hilt da Home e do detalhe de projeto), `MagicNumber` × 3 (as constantes do PBKDF2 e as do calendário e do algoritmo de Easter), `TooManyFunctions` × 1 (o `BackupDao`, uma função por operação, sem lógica), `ReturnCount` × 1 (o `parseHash`, cujas guard clauses validam cada campo do hash no ponto em que ele é extraído) e `NewApi` × 2 (o `java.time` do `NewTaskDialog`, coberto pelo `coreLibraryDesugaring`). As do `HomeListingPipeline` saíram no #295 (dependências e sinais agrupados, etapas extraídas), a do `HomeCreateProjectDialog` no #299 (nove parâmetros viraram `CreateProjectFields` + `CreateProjectCallbacks`) e a do `PasswordField` no #300 (`PasswordFieldLabels` + `PasswordFieldTags` viraram `PasswordFieldConfig`). As três que restavam como `@file:Suppress` — `HomeCreateProjectDialog`, `HomeProjectsContent` e `NewTaskDialog` — desceram para a função, com a justificativa no lugar. E as de `LoginScreen` e `HomeCreateProjectDialog` saíram por completo no #310: a rodada encolheu as duas funções abaixo do limiar, e a regra voltou a valer. A rodada seguinte levou as três restantes do `:feature:projects`: o orquestrador do detalhe de projeto (#312), a lista da Home (#313) e o menu de prioridade do diálogo de nova tarefa (#314) — cada uma decomposta em partes nomeadas, com a regra `LongMethod` de volta a valer. **As duas `LongParameterList` que ficam são construtores do Hilt** (oito dependências cada: quatro repositórios e quatro casos de uso na Home; um repositório, seis casos de uso e o `SavedStateHandle` no detalhe) — a regra conta parâmetros de funções chamadas à mão, e um construtor de DI é chamado pelo framework. A `LongParameterList` tem peso 1 na `config/detekt/detekt.yml` justamente porque a regra não serve às assinaturas de UI, que são agrupadas por outro caminho.
- ✅ **RESOLVIDO (PR #267)** — **`SnapshotStateList<String>`** leak em `CreateProjectDialogBody:1225` — tipar `List<String>` + callbacks. A assinatura agora recebe `List<String>` e o estado da seleção volta por callback, sem expor o `SnapshotStateList`.
- ✅ **RESOLVIDO (DEF-72, PR #261)** — **`HomeUser.kt:23-26`** `DefaultHomeUser("João Pedro"/"JP")` flashing em Loading/SignedOut. Trocar por shimmer/genérico. A `HomeTopBar` passou a aceitar `user: HomeUser?` — `null` significa "sem sessão": sem avatar, sem badge e a saudação genérica.
- ✅ **RESOLVIDO (PR #296)** — **`androidx.hilt.navigation.compose.hiltViewModel` depreciado** — a função mudou de pacote para `androidx.hilt.lifecycle.viewmodel.compose`. Os sete imports foram trocados e o artefato `androidx.hilt:hilt-navigation-compose` saiu do catálogo: depois da troca, nada mais importava `androidx.hilt.navigation.*`. No `:app` a dependência saiu inteira — ele usava a biblioteca sem nada dela.
- ✅ **RESOLVIDO (PR #297)** — **`rememberSaveable(key = ...)` depreciado** — a chave manual vira a chave do registro de estado **sem** o hash de posição, o que permite que dois `rememberSaveable` com a mesma string, em telas diferentes, divida o mesmo slot. Os dois diálogos de edição do detalhe do projeto (prioridade e renomear) usavam chaves manuais; cada um ganhou a sua própria função (`rememberPriorityDialogTaskId`, `rememberRenameTaskId`) e o escopo posicional do runtime passou a fazer a separação. A justificativa antiga no código — "sem a chave os dois compartilhariam o mesmo slot" — estava errada: verificado por sabotagem, os slots ficam separados mesmo com as duas chamadas na mesma função.
- ✅ **RESOLVIDO (PR #299)** — **`@file:Suppress("LongParameterList", "LongMethod")` no `HomeCreateProjectDialog.kt`** — supressão de bloco, o que a própria auditoria apontou ("corrigir decompondo, não suprimindo"). `CreateProjectDialogBody` tinha nove parâmetros, um a mais que o limiar: agora são três — os valores em `CreateProjectFields`, as ações em `CreateProjectCallbacks`, o mesmo desenho de estado/ação que a Home já usa. A supressão que ficou virou pontual, com a justificativa no lugar. O `HomeProjectsContent.kt`, que suprimia o arquivo inteiro para uma função só, desceu a supressão para a função.
- ✅ **RESOLVIDO (PR #300)** — **`@Suppress("LongParameterList")` no `PasswordField`** — o campo de senha do Login (reusado no Cadastro) tinha oito parâmetros, um a mais que o limiar, e recebia dois objetos (`PasswordFieldLabels` e `PasswordFieldTags`). Os dois viraram um (`PasswordFieldConfig`), que é o desenho que o `EmailField` já usava: com sete parâmetros, a supressão saiu. O `NewTaskDialog.kt`, com `@file:Suppress("LongMethod")` para uma função só, também desceu a supressão para a função.
- ✅ **RESOLVIDO (PR #301)** — **`createComposeRule` depreciada** — as 22 suítes Compose (21 em `src/test`, uma instrumentada) migraram para `androidx.compose.ui.test.junit4.v2.createComposeRule`, que usa `StandardTestDispatcher` em vez de `UnconfinedTestDispatcher`. Nenhuma precisou de sincronização explícita: a suíte completa passa sem alteração. O `createAndroidComposeRule` (5 usos) não está depreciado e ficou como estava.

---

> **⚠️ Estado em 23/10/2026.** A campanha de revisão fechou os últimos
> gaps de teste do marco E3.6 e limpou recursos duplicados: o
> `DeadlineReceiver` ganhou suíte própria (PR #270), o `PepperProvider`
> ganhou suíte instrumentada (PR #271), o `TaskTest` passou a exigir o
> carimbo de conclusão (PR #269), o `DeadlineWorker` ganhou o teste do
> caminho de publicação (PR #273) e o `:app` deixou de guardar cópia
> das strings das features (PR #272). A tabela de gaps abaixo foi
> reconciliada item a item contra o código — inclusive onde a nota
> anterior dava um item por fechado e ele não estava.

---

## Gaps de teste (priorizado)

> **Estado em 23/10/2026.** Reconciliado item a item contra o código
> (campanha #226–#279). **Fechados:** os use cases de
> `CreateTag`/`UpdateProject`/`DeleteProject`/`DeleteTask` (P0), o
> `DeadlineWorker` no caminho de publicação (P0, PR #273), o
> `DeadlineReceiver` como receiver isolado (P0, PR #270), o
> `PepperProvider` (P0, PR #271), os repositórios de `:core:data` (P0),
> as migrations 2→3 e 3→4 em SQLite real (P1), o `ActiveUserProvider`
> (P1, PR #268), o `TaskTest` com `completedAt` (P1, PR #269) e o
> cenário duplicado do `TaskPriorityRulesTest` (P1, PR #269), o
> `AuthViewModel` no limite do nome (P1, PR #278), o `DeadlineField`
> (P1), o `SessionStore` (P2), as agregações do dashboard (P2) e a
> `SplashScreen` (P2). **Parciais:** a
> `HomeScreen`/`ProjectDetailScreen` (há suíte de componente, não da
> tela inteira), a `RegisterScreen` (coberta pelo caso de IME), a
> `TasksScreen` (coberta pelo `TasksScreenTest` + o `TasksRoutesTest`) e a
> `SettingsScreen` (não há
> ViewModel de settings — o backup tem o seu). **Obsoletos:** o
> `BrainOutSyncDispatcher` e o `ConnectivityObserver` — a camada de sync
> foi removida no #212.

| Severidade | Módulo | Arquivo | Situação |
|---|---|---|---|
| P0 | core/data | `repository/{Task,Project,Tag}RepositoryImpl.kt` | ✅ **RESOLVIDO** — `TaskRepositoryImplTest`, `ProjectRepositoryImplTest` e `TagRepositoryImplTest` cobrem dual-write, cascade e validação. A parte de *sync enqueue* deixou de existir com a remoção da camada de rede (#212). |
| P0 | core/data | `sync/BrainOutSyncDispatcher.kt` | ⚪️ **OBSOLETO** — a classe foi removida com a camada de sync no #212; não há mais o que testar. |
| P0 | core/data | `security/PepperProvider.kt` | ✅ **RESOLVIDO** — PR #271: suíte instrumentada (`PepperProviderInstrumentedTest`) prova os 16 bytes, a estabilidade entre chamadas e a regeneração após apagar o armazenamento (a regressão do B1). |
| P0 | core/domain | `usecase/{CreateTag,UpdateProject,DeleteProject,DeleteTag}UseCaseTest.kt` | ✅ **RESOLVIDO** — os quatro testes existem e o piso de 60% do Kover passa. |
| P0 | app | `notifications/DeadlineReceiver.kt` | ✅ **RESOLVIDO** — PR #270: `DeadlineReceiverTest` roda o receiver de verdade (Robolectric + grafo do Hilt) e prova o enfileiramento do `CompleteTaskWorker`, o cancelamento do lembrete e a dispensa da notificação. |
| P0 | app | `notifications/DeadlineWorker.kt` | ✅ **RESOLVIDO** — o `DeadlineWorkerTest` cobre o desfecho (falha terminal vs. repetição, DEF-32) e o `DeadlineWorkerPublishTest` (PR #273) cobre o *publish path*: título, projeto, ação "Concluir", identidade (tag, id) e gate de `POST_NOTIFICATIONS`. |
| P1 | core/data | `local/Migrations.kt` 2→3 e 3→4 | ✅ **RESOLVIDO** — `MigrationTest` exercita `MIGRATION_2_3` e `MIGRATION_3_4` em SQLite real, além da cadeia 1→5 e do reparo do banco legado. |
| P1 | core/data | `session/ActiveUserProvider.kt` | ✅ **RESOLVIDO** — PR #268: `ActiveUserProviderTest` cobre a limpeza da sessão órfã e o `flatMapLatest`. |
| P1 | core/data | `sync/ConnectivityObserver.kt` | ⚪️ **OBSOLETO** — removido com a camada de sync no #212. |
| P1 | core/domain | `TaskTest` | ✅ **RESOLVIDO** — PR #269: `Task.transitionTo` aceita o instante por parâmetro (como o `now` de `Task.create`) e o teste afirma o carimbo e o apagamento. |
| P1 | core/domain | `TaskPriorityRulesTest.update integral` | ✅ **RESOLVIDO** — PR #269: o cenário duplicado foi removido; o atalho `copy` que o modelo não bloqueia é coberto no `UpdateTaskUseCaseTest`, junto do caso de uso que o barra. |
| P1 | feature/settings | `ui/SettingsScreen.kt` | 🟡 **PARCIAL** — `SettingsScreenTest` existe; `SettingsViewModelTest` não tem objeto, porque a tela de Configurações não tem ViewModel (o backup tem o seu, `BackupViewModel`). |
| P1 | feature/projects | `ui/home/HomeScreen.kt` + `ui/projectdetail/ProjectDetailScreen.kt` | ✅ **RESOLVIDO** — as duas telas ganharam suíte de tela inteira em Robolectric (rodam no CI, que não tem emulador): `HomeScreenTest` (5 casos: estado vazio, cards, toque no card, FAB e carregamento) e `ProjectDetailScreenTest` (5 casos: card vazio, tarefas na lista, FAB, carregamento e a transição vinda do menu da tarefa). Havia ainda suíte de componente (`HomeTopBarTest`, `HomeFilterRowTest`, `HomeCreateProjectDialogTest`, `ProjectDetailErrorBannerTest`, `NewTaskDialogTest`). |
| P1 | feature/projects | `DeadlineField.kt` | ✅ **RESOLVIDO** — `DeadlineFieldTest` cobre o round-trip `pickerDateToDeadline`/`deadlineToPickerDate`. |
| P1 | feature/auth | `ui/register/RegisterScreen.kt` | ✅ **RESOLVIDO** — `RegisterScreenTest` (Robolectric, 5 casos): o erro de campo aparece no campo (semântica `Error`), o foco volta para o primeiro campo inválido, senhas diferentes barram o envio, o cadastro inválido não navega e o cadastro válido persiste a sessão e pede a navegação. |
| P1 | feature/auth | `viewmodel/AuthViewModelTest.kt` | ✅ **RESOLVIDO** — o nome vazio, o erro de domínio e o erro inesperado estão cobertos, e o `validateName` ganhou os dois casos que faltavam: nome acima de `User.MAX_NAME_LENGTH` (erro `LongName`) e nome exatamente no limite (aceito). |
| P1 | feature/tasks | `ui/TasksScreen.kt` | ✅ **RESOLVIDO** — `TasksScreenTest` cobre a tela e o `TasksRoutesTest` (novo) cobre as rotas que a feature expõe (`tasksGraph()` e `dashboardGraph()`), montando o grafo de verdade. |
| P2 | core/data | `session/SessionStore.kt` | ✅ **RESOLVIDO** — `SessionStoreTest` cobre round-trip e `clear`. |
| P2 | core/data | `local/dao/DashboardAggregates.kt` | ✅ **RESOLVIDO** — `TaskRepositoryImplTest` prova o preenchimento de `observeCountByPriority` de 0 a 4. |
| P2 | core/domain | `TaskCompletionStats` / `TaskPriorityCount` / `SortOrder` | ✅ **RESOLVIDO** — `DashboardViewModelTest` exercita as duas agregações e `SortOrderStorageKeyTest` cobre o `SortOrder`. |
| P2 | feature/auth | `ui/splash/SplashScreen.kt` | ✅ **RESOLVIDO** — `SplashScreenTest` existe. |

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

### P2 — corrigidos (lote posterior)

- **P2 (auth)** `AuthViewModel.kt` — `MutableSharedFlow` (default replay=0, buffer=0) → `Channel<AuthEvent>(BUFFERED)` + `receiveAsFlow()`. `NavigateHome` não se perde entre composições ou config change. `AuthEvent.FocusField` + `AuthField` enum removidos (dead code; foco já é state-driven).
- **P2 (projects)** `HomeViewModel.kt` — tags-por-projeto (B2). Port `TagRepository.observeByProjectIds(ids): Flow<Map<String, List<Tag>>>` adicionado; `TagRepositoryImpl` via `ProjectDao.observeTagsFor` + `combine`. `HomeViewModel.buildHomeUiState` recebe `tagsByProject` e cada `ProjectCardItem` lista apenas as tags do projeto (antes mostrava todas as tags do owner).
- **P2 (projects)** `HomeScreen.kt` — `actionError by viewModel.errorMessage.collectAsStateWithLifecycle()` adicionado; passa `actionError ?: listState.errorMessage` ao `HomeProjectsContent` (erros de CRUD antes caíam em `_errorMessage` privado).
- **P2 (projects)** `ProjectDetailScreen.kt` — lista de tasks via `LazyColumn` (keyed por `task.id`) com `contentPadding(bottom = 80.dp)` para o FAB. Elimina o `verticalScroll + forEach` que renderizava todas as linhas de uma vez.
- **P2 (projects)** `ProjectDetailScreen.kt` — `DeleteProjectDialog` usa `project_detail_delete_confirm_title/body` (pt+en) em vez do título da home page.
- **P2 (projects)** `DeadlineField.kt` — `DateTimeFormatter.ofPattern` agora lê `project_detail_new_task_due_date_format` do `strings.xml` (pt: `dd/MM/yyyy`, en: `MM/dd/yyyy`).
- **P2 (tasks)** `TasksViewModel.kt` + `DashboardViewModel.kt` — `uiState` combina com `_errorMessage` via `combine`. `clearError()` zera o banner de erro (antes só zerava o flow interno e o banner persistia).
- **P2 (settings)** `SettingsScreen.kt` — preview usa `BrainOutTheme { Surface { … } }`. `SettingsRow` ganha `testTag("settings_option_<type>")`. `SettingsScreenTest.kt` Robolectric cobre renderização (título, 3 seções, 4 opções) e dispatch do `SettingsActionType` por clique.
- **P2 (core/data)** `PepperProvider.kt` — verifica que `PepperProvider.kt:54` usou `System.identityHashCode` em vez do alias da `MasterKey` (que é público/idêntico em todo dispositivo). Corrigido para `EncryptedSharedPreferences` com `SecureRandom` (16 bytes).
- **P2 (core/data)** `RemoteDataSource.kt` — `HttpLoggingInterceptor.Level.BASIC` agora só é instalado quando `loggingEnabled = true`. `DataModule` passa `BuildConfig.DEBUG`, então release não loga URL no logcat.
- **P2 (core/data)** `DomainException.kt` — adicionado `TagOwnershipException`. `ProjectRepositoryImpl.validateTagOwnership` lança essa exceção (em vez de `require IllegalArgumentException`) quando a tag existe mas pertence a outro owner.
- **P2 (core/data)** `TaskRepositoryImplTest.kt` + `ProjectRepositoryImplTest.kt` + `TagRepositoryImplTest.kt` — testes diretos cobrindo escrita dual (Room + `pending_ops`), cascatas RN03, validação de propriedade de tags, `TaskNotFoundException` (P0-2) e payload de sync com `id` cliente-supplied (R6/P0-4). Fecham o maior gap de Kover 60% do `:core:data`.
- **P2 (CI)** `codeql.yml` — `build-mode: none` → `autobuild` (linguagens compiladas precisam de toolchain detectada; sem isso a análise Java/Kotlin fica vazia).
- **P2 (CI)** `backend-stub/server.py` — `datetime.utcnow()` (deprecated 3.12) → `datetime.now(timezone.utc)`.
- **P2 (CI)** `backend-stub/tests/smoke_e2e.py` — `curl()` retorna HTTP code real (via `-w "%{http_code}"`) em vez de hardcodar `200` quando `want_status is None`. Asserts de status agora falham honestamente.
- **P2 (CI)** `:app` `build.gradle.kts` + `libs.versions.toml` — adicionado `errorprone-annotations 2.28.0`. Necessário porque o `build-mode: autobuild` do CodeQL aciona R8 minify do :app, que precisa de `com.google.errorprone.annotations.CanIgnoreReturnValue` (referenciada em runtime por Tink/EncryptedSharedPreferences via PepperProvider do :core:data). Sem o dep, `:app:minifyDevReleaseWithR8` quebrava o build do CodeQL.
- **P2 (core/data)** `RemoteDataSource.kt` — `catch (e: CancellationException) { throw e }` antes de cada um dos 13 `catch (e: Exception)`. Antes, o catch genérico engolia `CancellationException` quando uma coroutine era cancelada (ex.: viewModelScope cancelado em logout, SyncWorker cancelado), fazendo o `BrainOutSyncDispatcher` interpretar como falha retentável em vez de cancelamento normal.
- **P2 (feature/projects)** `feature/projects/src/main/res/values*/strings.xml` — removidas 4 chaves mortas (`home_fab_disabled_label`, `project_detail_back`, `project_detail_new_task_due_date_picker_title`, `project_detail_task_priority_locked_info`) que não eram referenciadas pelo código (grep em .kt confirma zero usos).
- **P2 (feature/auth)** `feature/auth/src/main/res/values*/strings.xml` — removidas 4 chaves `home_*` (`home_role_member_dialog_title/body/ack`, `home_fab_disabled_owner`) que eram cópias mortas das chaves correspondentes em `feature/projects/res` (usadas pelo `HomeFab.kt` via `feature.projects.R`). Sem isso, risco de drift entre as duas cópias.
- **P2 (feature/projects)** `ProjectDetailScreen.kt` — decompose de 971 LoC em orchestrator (186 LoC) + 9 arquivos extraídos: `ProjectDetailTopBar`, `ProjectDetailOfflineBanner`, `ProjectDetailStates` (loading/error/empty + resolveMessage), `ProjectDetailBody` (Column + LazyColumn de tasks), `TaskRow` (row + chips + menu + StatusChip + PriorityChip + extensões `TaskPriority.labelRes`, `TaskStatus.{allowedTransitions,menuTag}`), `NewTaskDialog`, `DeleteProjectDialog`, `ChangeTaskPriorityDialog`, `ProjectDetailTestTags`. API pública preservada; detekt + ktlintCheck verde.
- **P2 (feature/tasks)** `DashboardScreen.kt` — decompose de 593 LoC em orchestrator (164 LoC) + 6 arquivos: `DashboardLoading`, `DashboardErrorBanner`, `DashboardEmptyState`, `DashboardProjectStateCard`, `DashboardPriorityChart` (com `PriorityBarsCanvas`, `PriorityBarsLegend`, `priorityShortLabel` e constantes `BAR_TO_SLOT_RATIO`/`BAR_CORNER_RADIUS_PX` que resolvem o `MagicNumber`), `DashboardCompletionRateCards`. API pública preservada; detekt + ktlintCheck verde.
- **P2 (feature/projects)** `HomeViewModel.kt` — decompose de 595 LoC em orchestrator (197 LoC) + 4 arquivos: `HomeUiModels` (HomeUserState/HomeSyncState/HomeUiState/ProjectCardItem/TagChip), `HomeMappers` (toHomeRole/toHomeUserState/toChip/toHomeErrorMessage/toHomeActionErrorMessage/computeInitials), `HomeListingPipeline` (ListingSnapshot/ListingInputs/debouncedSearchInput/listingPrefsFlow/tagChipsForOwnerFlow/homeUiStateFlow/buildHomeUiState), `HomeActions` (runCreateProject/runUpdateProject/runDeleteProject/runCreateTag/runOnSearchQueryChange/runOnTagFilterChange/runOnSortOrderChange). Os actions passam a delegar para funções top-level que recebem dependências + error sink como params. API pública (construtor, flows, actions, companion) preservada; detekt + ktlintCheck verde.

### Não corrigidos (P2+ / fora de escopo deste lote)

> Snapshot **de 24/09/2026**. Itens marcados com ✅ foram
> corrigidos depois e estão verificados no código (ver §
> [Reverificação 2026-10-08](#reverificacao-2026-10-08)); os demais
> foram reconferidos em 08/10/2026 e continuam abertos.

- `feature/projects/res/values*/strings.xml` ainda duplica 9 chaves `settings_*` em `feature/settings/res/` (não tocou — é feature/cross). ⚠️ **AINDA VÁLIDO** — mas a duplicação real é entre `app/src/main/res/values*/strings.xml` e `feature/settings/res`, não em `feature/projects`.
- `feature/auth/res` contém 4 chaves `home_*` duplicadas. ✅ **RESOLVIDO** — zero `home_*` restantes em `feature/auth/res`.
- Auth: `resolveAuthMessage` em composable; `else -> message` vaza PT em EN.
- Auth: `LoginScreen.kt:3-4` comentário referencia `SessionViewModel` que não existe.
- `feature/tasks` — read-only (zero CRUD), `PriorityBarsCanvas` raw px, UTC Monday hardcoded.
- `feature/settings` — sem VM (estado de Settings é todo stateless no escopo do E1.3); sem decompose estrutural.
- `core/ui`: `ThemeSelectionTest` tautológico; `ContrastRatioTest` com subset de pares; dynamic color path sem teste.
- `core/ui`: 23 hardcoded `RoundedCornerShape` em `feature/*` violando contrato de `Shape.kt`.
- `core/data`: `ProjectCreateDto.id: String? = null` em PUT pode omitir body → 400 stub.
- `core/data`: TAG UPDATE rejeitado por design (aceitável, alinhado a R6).
- `core/data`: `last-writer-wins via updated_at` ausente end-to-end (precisa migration v5 + stub compare + dispatch).
- `core/data`: `ProjectRepositoryImpl` — exceções de domínio tipadas com teste, mas "bypass" via `Task.copy` não travado por teste (E14). ✅ **RESOLVIDO em 08/10** — `ChangeTaskStatusUseCase.invoke(Task)` agora recarrega o registro persistido e rejeita RN02 com `TaskPriorityChangeForbiddenException`.
- `core/data`: `HomeScreenTest`/`ProjectDetailScreenTest` Compose ausentes (testTag infra pronta).
- `backend-stub`: `TaskUpsert.project_id` sem UUID validator (404 em vez de 400); `_normalize_id` aceita hex-32 mas `_require_uuid` não (assimetria path/body).
- CI: backend-integration sem `setup-java`. ✅ **RESOLVIDO** — `ci.yml:188-191` provisiona Temurin 21.
- docs: README link para RELATORIO-TECNICO.md quebrado; ARQUITETURA stack table stale.

Próxima rodada: workflow scripts + ADR directory + feature/tasks CRUD (ref. §Workflows).

### Verificação executada

> Snapshot **de 24/09/2026**, quando o ambiente local não tinha Android
> SDK. Substituído pela execução real de 08/10/2026 registrada em
> § [Reverificação 2026-10-08](#reverificacao-2026-10-08) — os testes
> unitários de `:core:data`, `:app` e `:feature:*` rodam e estão verdes
> (618 testes), e `pytest` roda (16 casos).

- `:core:domain` — `gradle :core:domain:test` BUILD SUCCESSFUL (118 testes passaram após correções).
- `:core:domain` — `:core:domain:detekt` + `ktlintCheck` BUILD SUCCESSFUL.
- `:core:data` / `:app` / `:feature:*` — não compila localmente (ambiente sem Android SDK); verificável em CI após merge.
- `backend-stub/server.py` + `tests/test_contract.py` — `python3 -m py_compile` OK em ambos.
- `pytest` — não executável (venv sem wheels de pydantic no ambiente); sintaxe OK.

---

<a id="reverificacao-2026-10-08"></a>

## Reverificação 2026-10-08

Esta seção substitui, para fins de rastreabilidade, a § *Verificação
executada* acima — que descrevia um ambiente **sem Android SDK**, onde
`:core:data`, `:app` e `:feature:*` não compilavam e o `pytest` não
rodava. Em **08/10/2026** a auditoria foi refeita com **execução real**:
suíte unitária completa, build release com R8 e emulador API 37.

### Como verificar (comandos)

```bash
# 618 testes unitários, 0 falhas
./gradlew testDevDebugUnitTest :feature:projects:testDebugUnitTest \
  :feature:tasks:testDebugUnitTest :feature:auth:testDebugUnitTest \
  :feature:settings:testDebugUnitTest :core:ui:testDebugUnitTest

# cobertura: o gate de 60% passa
./gradlew :core:domain:koverVerify :core:data:koverVerify

# contagem a partir dos XML de resultado
find . -path '*build/test-results/**/*.xml' | xargs grep -ho 'tests="[0-9]*"'
```

**Testes unitários: 561 → 618 (+57), 0 falhas.** Distribuição medida
nos `build/test-results/`:

| Módulo | Testes |
|---|---|
| `:app` | 24 |
| `:core:data` | 302 |
| `:core:domain` | 133 |
| `:core:ui` | 88 |
| `:feature:auth` | 15 |
| `:feature:projects` | 35 |
| `:feature:settings` | 3 |
| `:feature:tasks` | 18 |
| **Total** | **618** |

**Cobertura (Kover 0.9.9, gate de 60% verde):** `:core:domain`
84,9% linha / 77,4% branch / 73,9% classe; `:core:data` 84,1%
linha / 73% branch / 80,7% classe — lido de
`core/{domain,data}/build/reports/kover/html/index.html`.

**Build release com R8** concluído. **Emulador API 37** usado para
executar a suíte instrumentada (ver DEF-08/DEF-13 em
`docs/DEFEITOS.md`).

### Os 8 bugs corrigidos em 08/10/2026

Todos em `main` (`316ed41`, 129 commits) ou na árvore de trabalho
desta sessão. Bugs de produto, não estilo.

| # | Bug | Arquivo |
|---|---|---|
| 1 | **`ChangeTaskStatusUseCase` não revalidava RN02 no overload `invoke(Task)`.** O `grep` mostrava zero chamadores de produção, mas, sendo a porta pública de atualização integral, um `Task.copy` burlava o bloqueio de prioridade em tarefa concluída. O overload agora recarrega o registro persistido e rejeita com `TaskPriorityChangeForbiddenException`. | `core/domain/.../usecase/ChangeTaskStatusUseCase.kt:105-107` |
| 2 | **`BusinessRuleException` e `DomainException` eram raízes irmãs.** Nenhum `catch (DomainException)` capturava uma violação de RN — no `CompleteTaskWorker`, isso significava `ProjectTaskLimitReachedException` e `TaskPriorityChangeForbiddenException` escapando da captura idempotente e chegando a uma falha de worker. `BusinessRuleException` passou a estender `DomainException` (que já é `IllegalArgumentException`, preservando os contratos testados). | `core/domain/.../error/BusinessRuleException.kt:27-29` |
| 3 | **Ordenação da Home quebrada — `SortOrder.toStorageKey()` emitia grafia errada.** Emitia `name.lowercase()` (`nameasc`), mas o `ORDER BY` dinâmico do `ProjectDao` compara `'name_asc'`, `'name_desc'`, `'created_asc'`, `'created_desc'`: nenhuma casa, todos os `CASE` devolvem `NULL` e o SQLite devolve as linhas na ordem natural do scan — a ordenação escolhida pelo usuário era silenciosamente ignorada. `toStorageKey()` passou a emitir as chaves canônicas. | `core/domain/.../repository/ListingPreferencesRepository.kt:59-65` (contraparte SQL: `core/data/.../local/dao/ProjectDao.kt:86-90`) |
| 4 | **`PasswordHasherImpl.verify` ignorava `parsed.iterations`.** O custo do KDF vinha da constante, não do hash persistido: subir `PBKDF2_ITERATIONS` (ex.: 120 000 → 600 000, conforme OWASP) tornaria todo hash antigo verificável apenas contra o custo novo, quebrando silenciosamente todas as contas existentes. Agora deriva do hash e valida a faixa na leitura. | `core/data/.../security/PasswordHasherImpl.kt:81`, `:102`, `:153` (`MIN=1` / `MAX=2_000_000` em `:179,185`) |
| 5 | **Suíte instrumentada de `:core:data` não compilava** — faltavam `truth` e `kotlinx-coroutines-test` em `androidTestImplementation`; ~40 erros `Unresolved reference`, e nenhuma migration do Room era testada em lugar nenhum. | `core/data/build.gradle.kts:181-182` |
| 6 | **`MigrationTest` só cobria 1→2** (e nem compilava). Agora cobre 2→3 e 3→4, em SQLite real — `completed_at` e a fila `pending_ops` passam a ser validadas por `runMigrationsAndValidate`. | `core/data/src/androidTest/.../local/MigrationTest.kt` (`MIGRATION_2_3` em `:105-112`, `MIGRATION_3_4` em `:146-173`) |
| 7 | **Gate de estilo do ktlint não inspecionava nada.** O plugin era aplicado só na raiz, embora o `build.gradle.kts:16` afirmasse o contrário: `ktlintCheck` cobria 2 arquivos de build e **zero dos 221 `.kt` versionados** (confirmado em disco — só havia `build/reports/ktlint/` na raiz). Aplicado agora nos 9 projetos; as ~180 violações cosméticas que apareceram ficam com `ignoreFailures = true` até uma reformatação dedicada (**DEF-09**/**DEF-10**). | `build.gradle.kts:112-129` |
| 8 | **`UserDaoInstrumentedTest` auto-pulava sem Android runtime** (`assumeTrue`), masking a ausência de cobertura em vez de falhar alto. A instrumentação agora falha explicitamente quando não há runtime. | `core/data/src/androidTest/.../local/dao/UserDaoInstrumentedTest.kt` |

### Testes que acompanham as correções

`ProjectDaoSearchSortOrderTest` (9 casos, novo), `DomainExceptionHierarchyTest` (5, novo), `SortOrderStorageKeyTest` (7, novo), `NeoThemeActivationTest` (14, novo), `PasswordHasherImplTest` (12, ampliado), `ChangeTaskStatusUseCaseTest` (ampliado).

### Pendências que permanecem (não corrigidas nesta sessão)

Registradas em `docs/DEFEITOS.md` com severidade, origem e plano:

- **DEF-10** — ~180 violações cosméticas do ktlint; `ignoreFailures = true` até a reformatação dedicada.
- **DEF-11** — `.gitignore` não cobre `.venv`, e o README manda criar `backend-stub/.venv`. **Fora do escopo**: nenhum arquivo além destes dois documentos foi editado.
- **DEF-12** — `requirements.txt` não instala em Python 3.14 (`pydantic-core 2.23.4` sem wheel; PyO3 máx. 3.13). Os 16 testes do stub rodam.
- **DEF-13** — **aberto**: `LoginScreenTest` compila mas os 3 testes falham em runtime no emulador API 37 — `NoSuchMethodException: android.hardware.input.InputManager.getInstance` no `Espresso.onIdle`; Espresso 3.6.1 é incompatível com `compileSdk 37`.
- **DEF-14** — o gate do Kover exclui `*.remote.*` e `*.di.*` do denominador: **806 das 3 944 linhas** de produção de `:core:data` (20,4%), incluindo `RemoteDataSource` (253 linhas, 26 `catch`) e `HolidayRemoteDataSource`. Influi a métrica; não foi corrigido.

---

## Revisão da camada de teste (24/10/2026)

A rodada de 23/10 cuidou da produção. Esta cuidou da **camada de teste**,
que ainda tinha duplicação do mesmo tipo — e a mesma regra valendo: quando
duas cópias fixavam o **mesmo** conhecimento, elas viraram uma fonte só.

| PR | O que foi unificado | Cópias |
|---|---|---|
| #328 | `inMemoryDatabase()` — o `inMemoryDatabaseBuilder` do Room | 7 → 1 |
| #328 | `sampleProject` / `sampleTask` / `sampleTag` (entidades) | 3 → 1 |
| #329 | `FakeLocalTransaction` | 3 → 1 |
| #329 | `sampleProject` / `sampleTag` / `sampleTask` (domínio) | 2 → 1 |
| #330 | `deadlineWorkerFactory` / `completeTaskWorkerFactory` | 6 → 1 |
| #331 | `FakeProjectDao` (13 métodos da interface) | 2 → 1 |
| #343 | `FakeTagDao` (o completo — o `ProjectRepositoryImpl` usa só `findById`) | 2 → 1 |
| #332 | `NeoTokenParityTest` passa a ler o contrato | 8 grupos |

**Nenhum dado de teste mudou.** As sete cópias do banco usavam o mesmo
contexto (`ApplicationProvider`) e o mesmo `allowMainThreadQueries`; as
fábricas de entidade já usavam exatamente os mesmos valores padrão
(`"Projeto $id"`, `"Tarefa $id"`, `"#FF0000"`, `2026-01-01T00:00:00Z`). A
única mudança de valor foi o nome padrão da tag de domínio (`"Tag"` →
`"Urgente"`), numa suíte em que **nenhuma asserção lê o nome da tag**.

O `FakeProjectDao` foi o caso mais caro e o mais instructive: são dois
fakes de ~80 linhas para uma interface de 13 métodos, e as diferenças
entre eles (as cascatas agiam em um e não no outro) viviam no meio do
código repetido. Agora há um fake só, com as três operações de cascata
`open` — o `TaskRepositoryImplTest` as sobrescreve para registrarem a
chamada, o `ProjectRepositoryImplTest` usa a implementação padrão.

O `FakeTagDao` estava na lista do que ficaria de fora, e saiu de lá
depois: as duas versões tinham comportamentos diferentes — a de
`TagRepositoryImplTest` reproduz o índice UNIQUE `(owner_id, name)` e
monta `observeForProjects` a partir das associações — e a versão
completa recusaria a segunda tag de mesmo owner em `create substitui
associacoes de tags atomicamente`. Só que o `ProjectRepositoryImpl`
usa **apenas** `findById` do DAO de tags (as tags são gravadas direto
no mapa pelo teste, nunca por `insert`), então a versão completa serve
às duas suítes — e o caminho que parecia blocking nunca chegou a ser
exercitado por ela. O fake único saiu no #343.

### O teste de paridade dos tokens prometia uma coisa e fazia outra

O `NeoTokenParityTest` diz no próprio KDoc que *"nada aqui replica
constantes: os valores esperados vêm do arquivo"* — mas só as cores, os
papéis e o espaçamento cumpriam. Raios, bordas, tamanhos e breakpoints
repetiam os valores no código do teste, e as sombras repetiam os quatro
níveis inteiros.

Duas consequências:

- **Mudar o contrato não reprovava nada.** O teste comparava a cópia
  particular dele com o Kotlin, e os dois continuavam de acordo. A
  paridade só existia no papel.
- **`touchGap` não era verificado por lado nenhum** — declarado no
  `tokens.json` e no `NeoSizes`, ausente da lista do teste e sem uso em
  produção. Era o único símbolo da produção que a varredura de código
  morto achou, e não era código morto: era um token sem cobertura.

Depois da rodada (#332), cada grupo é lido do JSON e comparado com o
conjunto de tokens do Kotlin pelo nome, e a prova de que o teste morde é
por sabotagem: `"card": 8` → `"card": 12` reprova `raios declarados
batem com os tokens`, e remover `touchGap` do contrato reprova `tamanhos
declarados batem com os tokens`. Antes as duas passavam.

### O que ficou de fora, e por quê

- **O `setMain`/`resetMain` dos testes de ViewModel** — o boilerplate do
  dispatcher aparece em 8 suítes de 4 módulos. É glue de framework, e
  unificá-lo exigiria um `testFixtures` compartilhado entre módulos — uma
  mudança de topologia do build, não uma limpeza. Registrado como
  **DEF-81**.
- **O `FakeDeadlineNotificationScheduler`** — duas cópias idênticas, uma
  em `:core:domain/src/test/` e outra dentro da suíte de backup de
  `:core:data`. Código de teste não é publicado entre módulos, então a
  correção é a mesma do item acima — **DEF-81**.
- **O `seed` das suítes de backup** — os valores (`USER_ID`, `CREATED_AT`,
  `DUE_DATE`) são dado de cenário, não boilerplate.
- **A `EmptyTasksCard` do detalhe do projeto** — é a **sexta** cópia da
  receita de "estado vazio": o `:core:ui` é o dono dela (`NeoEmptyState`),
  que já atende as quatro telas de lista e painel, mas a `EmptyTasksCard`
  reimplementa o esqueleto à mão. Unificar elimina a cópia, mas **decide a
  aparência**: o mesmo estado vazio aparece hoje com raio 8 dp no detalhe
  do projeto e 16 dp nas outras quatro telas. Escolher qual é o certo é
  decisão de design — **DEF-82**.

### Varreduras de código morto

Duas varreduras sobre a produção (1 452 declarações, 1 012 nomes
distintos, uso contado incluindo chamadas de método) não acharam
**nenhum** código morto. Os cinco candidatos iniciais eram falsos
positivos — `bindDeadlineScheduler` (`@Binds`), `NotificationModule`
(`@Module`), `fromEpochMillis` e `toEpochMillis` (`@TypeConverter`) são
chamados pelo framework — e o sexto, `NeoSizes.touchGap`, era o token
sem cobertura descrito acima.

### Verificação

675 testes unitários, 0 falhas, 0 ignorados; `ktlintCheck`, `detekt` e os
gates de Kover de `:core:domain` e `:core:data` verdes.

---

## Documentos de entrega e o resto da rodada (24/10/2026)

A camada de teste era o alvo da primeira metade. A segunda pegou os
**documentos de entrega** — que mandavam o leitor (ou o agente de IA) rodar
comandos e procurar arquivos que não existem mais desde a transição local.

| PR | Documento | O que estava errado |
|---|---|---|
| #337 | `docs/APENDICE-C-CONFORMIDADE.md` | Registrava uma "pêndice" sobre R14 que já não valia: o `RELATORIO-TECNICO.md` **também** adota "Parcial". As duas referências de linha estavam deslocadas. |
| #338 | `docs/CI-CD.md` | As seções **operacionais** (comandos locais e troubleshooting) estavam dentro do bloco histórico e mandavam rodar `testDevDebugUnitTest`, `lintDevDebug`, `connectedDevDebugAndroidTest` e o `backend-stub/` — tarefas que não existem. O guia apontava para o issue #208 como "a reconciliar", e o issue está **fechado**. |
| #339 | `docs/ACESSIBILIDADE.md`, `docs/ROTEIRO-TESTES.md` | `installDevDebug` não existe; e TF-09/TF-10 eram descritos como *pendentes* quando os marcos E3.3/E3.4 foram **cancelados** com a camada de rede. |
| #340 | `docs/plans/2026-10-01-redesign-neobrutalista.md` | O plano — que é explicitamente um plano *para um agente de IA executar* — mandava rodar sete tarefas com flavor. |
| #341 | `feature/auth` | O `Scaffold` com a barra superior estava escrito duas vezes, linha por linha. Virou `AuthScreenScaffold` em `AuthForm.kt`, e os quatro `@OptIn(ExperimentalMaterial3Api::class)` que ficaram sem motivo saíram. |

### O que sobrou para o responsável

O **DEF-79** (o relatório técnico da N2 descreve a arquitetura com rede —
a §10 afirma que `INTERNET` e `ACCESS_NETWORK_STATE` são concedidas, e o
manifesto as remove) e o **DEF-80** (TF-09/TF-10 e o mapeamento de R5/R6
no roteiro de QA) ficaram **registrados com o escopo técnico mapeado**,
como o DEF-78. São alterações de documento **acadêmico** da entrega da N2:
a campanha corrige o que é fato e deixa a decisão — reescrever ou
reescopar — com o responsável.

### A produção voltou a zero duplicação

Depois do #341, a varredura de blocos (janelas de 8 linhas normalizadas,
entre arquivos distintos) sobre os 186 arquivos de produção **não acha
nenhum** bloco duplicado. A varredura de código morto continua sem
achado, e as nove supressões de produção têm todas a justificativa no
lugar.

---

## Constantes e chaves em duas camadas (24/10/2026)

A varredura de **literais** (strings repetidas em arquivos distintos,
fora de comentário) encontrou um tipo de duplicação que a varredura de
blocos não alcança: o mesmo **conhecimento** escrito em duas camadas
que não se enxergam.

| PR | O que estava duplicado | Modo de falha |
|---|---|---|
| #346 | As 17 chaves do JSON do payload de backup, escritas como literal no `encode*` **e** no `decode*` (34 repetições) | Uma chave grafada diferente nos dois lados perdia o dado **em silêncio** — o backup saía "correto" e a restauração devolvia o campo vazio |
| #347 | A categoria `"national"` do feriado, literal em `:core:data` (produtor), em `:core:domain` (consumidor) e em dois módulos de teste | Se o produtor gravasse diferente do consumidor, o filtro não casaria com nada e a dica de "próximo feriado" sumiria **sem erro** — e nenhum teste pegaria, porque o teste do caso de uso monta o próprio feriado |
| #349 | O nome do argumento da rota do detalhe (`"projectId"`), declarado em `BrainOutRoutes` (`:app`) e em `ProjectDetailViewModel` (`:feature:projects`) | A tela do detalhe quebraria em execução, no `checkNotNull` do ViewModel — e nenhum teste pegaria, porque os dois lados do teste eram autorreferentes |

Nos três casos, **os testes existentes não cobriam a divergência**:
ou comparavam a constante com ela mesma, ou montavam o dado com a
constante do próprio lado. A correção foi dar **uma fonte só** — no
domínio quando a regra é de domínio (`Holiday.TYPE_NATIONAL`), na
camada mais baixa quando é contrato entre módulos
(`ProjectDetailViewModel.PROJECT_ID_ARG`, que o `:app` passa a
honrar).

### O que ficou de fora, e por quê

- **As chaves de ordenação** (`name_asc`, `name_desc`, `created_desc`,
  `created_asc`) — o domínio emite (`SortOrder.toStorageKey()`) e o
  `ProjectDao` compara em SQL. A duplicação é **deliberada e
  documentada** (no `AGENTS.md` e no KDoc do DAO) e está guardada: o
  `ProjectDaoSearchSortOrderTest` roda a query real contra um banco
  Room em memória alimentando-a com `toStorageKey()`, e um caso fixa as
  quatro grafias. Interpolar as constantes no SQL trocaria a
  legibilidade do `CASE WHEN` por uma garantia que o teste já dá.
- **Os nomes de coluna do Room** (`created_at`, `owner_id`,
  `project_id`) — cada entidade declara as suas; uma constante
  compartilhada acopla as entidades entre si sem ganho.
- **A regra "não pode ser vazio"** dos modelos de domínio — o
  `require(x.isNotBlank()) { "x não pode ser vazio" }` inline é a
  forma mais clara da regra, e a mensagem é por entidade.

### Verificação

675 testes unitários, 0 falhas, 0 ignorados; `ktlintCheck`, `detekt` e
os gates de Kover de `:core:domain` e `:core:data` verdes.

---

## O mesmo tratamento de erro em duas cópias (25/10/2026)

A varredura de **blocos duplicados** já dava a produção por limpa (janela
de 8 linhas), mas o critério dela é o bloco de código — e o
`AuthViewModel` tinha uma duplicação que ela não alcançava: a
**estrutura** do tratamento de erro de `submitLogin` e de
`submitRegister` era a mesma, escrita duas vezes.

| O que estava duplicado | Modo de falha |
|---|---|
| Três dos quatro blocos `catch` idênticos nos dois fluxos (`CancellationException` → propaga, `DomainException` → `DomainRejected`, `Exception` → `Unexpected`), mais o preâmbulo (guarda de `isLoading`, validação, marcação de loading) | Um erro **novo** no tratamento — uma exceção de domínio que devesse virar erro de campo, um desfecho intermediário — seria corrigido em um dos dois fluxos e esquecido no outro. O usuário veria "credenciais inválidas" no login e "falha inesperada" no cadastro **para a mesma causa** |

### A correção

`launchSubmission(operation, fieldError, block)` centraliza o
tratamento: cancelamento propaga, erro de domínio vira
`AuthErrorType.DomainRejected` e falha inesperada vira
`AuthErrorType.Unexpected`. O que de fato muda entre os dois fluxos —
**qual** erro de domínio pertence a um campo — virou parâmetro: o
login mapeia `InvalidCredentialsException` para o campo de senha, o
cadastro mapeia `DuplicateEmailException` para o campo de e-mail, e
qualquer outro erro de domínio cai no banner.

O mapeamento é **file-private** (`AuthField`, `AuthFieldError` e a
extensão `AuthUiState.withFieldError`): nada de novo vazou para a API
do módulo.

### O que mais saiu no mesmo movimento

As seis funções de validação (`validateLoginFields`,
`validateRegisterFields`, `validateEmailFormat`,
`validatePasswordLength`, `validateName`,
`validatePasswordConfirmation`) e o `FieldErrors` foram para
`AuthFieldValidation.kt`. São regras **de campo**, sem efeito
colateral — não tocam o estado nem disparam evento; o ViewModel
orquestra a submissão. Efeito colateral útil: a classe `AuthViewModel`
saiu de **14 para 9 funções**, longe do `TooManyFunctions` do detekt
(`thresholdInClasses: 15`).

### A lacuna que a refatoração expôs

O caminho de erro **genérico** (`DomainRejected` / `Unexpected`) só
era exercitado pelo **cadastro** — o login tinha caso de credencial
inválida, mas não de erro de domínio nem de falha inesperada. Com os
dois fluxos passando pelo mesmo helper, essa assimetria deixou de
fazer sentido: os dois casos foram acrescentados ao
`AuthViewModelTest` (16 → 18 casos).

### Verificação

682 testes unitários, 0 falhas, 0 ignorados; `ktlintCheck`, `detekt` e
os gates de Kover de `:core:domain` e `:core:data` verdes. Os casos
existentes do `AuthViewModel` passaram **sem alteração** — a
refatoração mudou a forma, não o contrato: credencial inválida
continua no campo da senha, e-mail duplicado continua no campo do
e-mail, e o cancelamento continua propagando.

---

## A mesma regra de senha reescrita em quatro lugares (25/10/2026)

A varredura de **literais** repetidos achou uma string que aparece em
dois arquivos do domínio: `"A senha do backup deve ter pelo menos
$MIN_PASSWORD_LENGTH caracteres"`. O número já tinha uma fonte só
(`MIN_PASSWORD_LENGTH`), mas a **guarda** — o `require` com a
mensagem — estava reescrita em **quatro** casos de uso:

| Onde | Forma |
|---|---|
| `CreateUserUseCase` | `require(rawPassword.length >= MIN_PASSWORD_LENGTH) { "Senha deve ter pelo menos … " }` |
| `AuthenticateUserUseCase` | idem, com o mesmo texto |
| `ExportBackupUseCase` | `require(passphrase.length >= MIN_PASSWORD_LENGTH) { "A senha do backup deve ter pelo menos … " }` |
| `RestoreBackupUseCase` | idem, com o mesmo texto |

O modo de falha é concreto: **a senha do backup é a senha da
conta restaurada**. Se o export e o restore divergirem — um aceita
uma senha que o outro recusa — o app grava um arquivo que ele mesmo
não consegue restaurar, e o usuário só descobre no momento de
restaurar, com o backup já feito.

### A correção

`PasswordPolicy.kt` é o dono da regra: a constante `MIN_PASSWORD_LENGTH`
(movida de `CreateUserUseCase.kt`, **mesmo pacote** — nenhum import
mudou) e a guarda `requireValidPassword(password, subject)`. Os quatro
casos de uso agora chamam a guarda; o `subject` é o que distingue a
senha de conta da senha de backup, e o texto sai de um lugar só.

### A guarda é provada por sabotagem

O teste (`PasswordPolicyTest`) compara os casos de uso **entre si** —
comparar a constante com ela mesma não prova nada. Ele foi validado
por duas sabotagens:

- **Reintroduzir um `require` inline** no `AuthenticateUserUseCase`
  com outro texto → `a mensagem de senha curta e a mesma em cada par
  de casos de uso` **falha**.
- **Mudar o limite** do `RestoreBackupUseCase` para `>` → `a senha no
  limite e aceita pelos quatro casos de uso` **falha**.

O texto da mensagem é fixado por literal ("pelo menos 8
caracteres"), então o número também tem contrato escrito.

### Verificação

687 testes unitários, 0 falhas, 0 ignorados (3 novos no
`PasswordPolicyTest`); `ktlintCheck`, `detekt` e os gates de Kover de
`:core:domain` e `:core:data` verdes. O **texto** das mensagens e o
**limite** não mudaram — só passaram a ter uma fonte só.

---

## O mesmo vínculo ViewModel↔tela em duas telas (25/10/2026)

A varredura de duplicação por **linhas de código** (o script anterior
contava linhas de fechamento como conteúdo, o que produzia falso
positivo) apontou o par `LoginScreen` ↔ `RegisterScreen` com **10
blocos** de 6 linhas iguais. O achado real é um bloco de 9 linhas,
byte a byte igual nas duas telas:

```kotlin
val uiState by viewModel.state.collectAsStateWithLifecycle()

LaunchedEffect(viewModel) {
    viewModel.events.collect { event ->
        if (event is AuthEvent.NavigateHome) {
            on…Success()          // ← a única diferença: o nome do callback
        }
    }
}
```

O modo de falha: o vínculo entre o ViewModel e a tela — coletar o
estado e consumir o evento de navegação — é **uma** decisão, escrita
duas vezes. Um evento novo (ex.: "sessão expirada, volte ao login")
seria tratado em uma tela e esquecido na outra; e a forma de coletar o
estado (com `collectAsStateWithLifecycle`, que respeita o ciclo de vida
e não a `collectAsState` ingênua) só está garantida por causa da
cópia — se alguém corrigir uma tela e não a outra, uma delas passa a
coletar fora do ciclo de vida.

### A correção

`rememberAuthUiState(viewModel, onNavigateHome)` — em
`feature/auth/.../ui/AuthScreenState.kt` — é o dono do vínculo. As
duas telas passam a chamar uma linha:

```kotlin
val uiState = rememberAuthUiState(viewModel, onLoginSuccess)
```

O que muda entre elas (o nome do callback) continua explícito na
chamada; o que é comum (coletar o estado, consumir o evento) passa a
mora uma vez.

### Verificação

687 testes unitários, 0 falhas, 0 ignorados; `ktlintCheck`, `detekt` e
os gates de Kover de `:core:domain` e `:core:data` verdes. O caminho
do **cadastro** é coberto pelo `RegisterScreenTest` (Robolectric, roda
local): `cadastro valido persiste a sessao e navega para a home`
afirma que `onRegisterSuccess` foi chamado uma vez. O caminho do
**login** é coberto pelo `LoginScreenTest` instrumentado
(`login_with_valid_credentials_navigates_to_home`), que compila no CI
e roda em dispositivo.

---

## A última API legada da produção (25/10/2026)

Uma varredura por APIs fora do padrão (`java.text.*`, `java.util.Date`,
`java.util.Calendar`, `java.io.File*`, `java.util.Base64`,
`android.util.Base64`) achou **um** resíduo em toda a produção: o nome
sugerido do arquivo de backup.

```kotlin
// antes — `java.text` (a API que o JDK desaconselha)
val stamp = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
return "brainout-backup-$stamp.$BACKUP_FILE_EXTENSION"
```

O projeto já tinha migrado para `java.time` — o `coreLibraryDesugaring`
existe justamente para isso (o `AGENTS.md` da raiz registra: *"o que
fecha a dívida de `java.time`/`java.util.Base64` com `minSdk 24`")* — e
este era o **único** arquivo que ainda usava `java.text`/`java.util.Date`.
Trocando por `LocalDate` + `DateTimeFormatter.ISO_LOCAL_DATE`:

- o padrão ISO é **independente de locale** — o `Locale.US` era uma
  trava explícita para garantir exatamente isso;
- `DateTimeFormatter` é imutável e thread-safe, ao contrário do
  `SimpleDateFormat`;
- o fuso continua sendo o do aparelho (o `LocalDate.now()` usa o fuso
  default, como o `SimpleDateFormat` sem timezone explícito), e o
  resultado é byte a byte o mesmo.

### O que **não** foi mexido (e por quê)

A mesma varredura achou `android.util.Base64` em dois arquivos do
`:core:data` e `java.util.Base64` em um terceiro. Os três usos são
**justificados** e não são duplicação:

| Onde | Por que é diferente |
|---|---|
| `PasswordHasherImpl` | precisa do encoder **URL-safe** (`getUrlEncoder().withoutPadding()`) — o hash vai para um campo textual sem aspas. O `android.util.Base64` não tem URL-safe |
| `BackupCrypto` | o envelope é o **contrato do arquivo**; o decoder do `android.util.Base64` tolera whitespace, o `java.util.Base64` é estrito. Trocar mudaria o comportamento de leitura |
| `PepperProvider` | gravado e lido só pelo app; está no **DEF-75**, sob decisão do responsável |

### A guarda

O formato do nome do arquivo é **contrato** — `docs/qa/LOCAL-03.md`
registra `brainout-backup-2026-10-20.bnbak`, e o roteiro de QA depende
dele para reencontrar o arquivo exportado. A função ficou `internal`
(com a data por parâmetro) para poder ser testada, e o teste fixa o
formato. **Provado por sabotagem:** trocar o `ISO_LOCAL_DATE` por um
`ofPattern("dd/MM/yyyy")` faz o caso
`o nome sugerido do arquivo usa a data em ISO` **falhar**.

### Verificação

688 testes unitários, 0 falhas, 0 ignorados; `ktlintCheck`, `detekt` e
os gates de Kover de `:core:domain` e `:core:data` verdes.