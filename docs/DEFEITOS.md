# Registro e Classificação de Defeitos — BrainOut (Ciclo 4 / E4.3)

> Documento operacional para registrar, classificar e acompanhar os
> defeitos identificados no BrainOut. Atende ao **item 6.3 do Documento
> Norteador Projeto Integrador ADS 2026-2** e é insumo direto para a
> **N2 (E5.1)**, para o **relatório de testes de E4.1** e para o
> **relatório de achados de E4.2**.
>
> Este arquivo é a fonte canônica do conjunto de defeitos do
> projeto; cada item entra com identificador, origem rastreável e
> classificação segundo a política do `docs/ROADMAP.md` (E4.3).
> Atualizações posteriores a esta entrega devem ser feitas por
> *commit dedicado* nesta mesma branch ou em PRs que referenciem um
> `DEF-NN` específico.

## 1. Política de classificação

A severidade reflete o **impacto sobre os fluxos principais**
(cobertura do N2 item 3: cadastro, login, criar projeto, criar
tarefa, concluir tarefa, sincronizar offline→online, receber
notificação) e define o **prazo de tratamento** antes da N2
(07 a 11/12/2026):

| Severidade     | Definição operacional                                                                                                                  | Tratamento antes da N2                                              |
|----------------|----------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------|
| **Bloqueante** | Impede a execução de um fluxo crítico. O app fecha (crash), perde dados do usuário ou torna-se inutilizável para o caso principal.     | **Obrigatório** corrigir antes da N2.                               |
| **Crítico**    | Degrada um fluxo crítico, mas existe contorno documentado e utilizável pelo usuário.                                                  | **Obrigatório** corrigir antes da N2 (ou registrar contorno homologado). |
| **Menor**      | Cosmético, textual, animação fora do padrão, mensagem ambígua ou *edge case* que não bloqueia o fluxo.                                  | Corrigir se couber no cronograma. Caso não corrigido, exige **justificativa explícita** nesta tabela. |

A contagem de defeitos corrigidos vs. justificados é o critério de
pronto do E4.3 e é referenciada no E5.1 (relatório técnico).

## 2. Estrutura do registro

Cada defeito ocupa **uma linha** da tabela da Seção 5 com os
seguintes campos (todos obrigatórios, exceto `PR/Commit da correção`
e `Justificativa de não-correção`, que ficam vazios conforme o
status):

| Coluna                   | Conteúdo                                                                                                                                                                                                       |
|--------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **ID**                   | `DEF-NN` (número sequencial com zero à esquerda, a partir de `DEF-01`).                                                                                                                                        |
| **Origem**               | Rastreabilidade: `TF-NN` (roteiro funcional, `docs/ROTEIRO-TESTES.md`), `Sessão Pn-Tm` (sessão de usabilidade `P1`–`P5`, tarefa `T1`/`T2`/`T3`), `CI run <id>` (falha de pipeline), `QA manual`, `Revisão` ou `Bug externo`. |
| **Descrição**            | Sintoma observado em uma frase, com o passo para reproduzir quando aplicável.                                                                                                                                  |
| **Severidade**           | `Bloq.` / `Crit.` / `Men.` (forma curta adotada em E4.1).                                                                                                                                                      |
| **Frequência**           | Quantas vezes foi observado até o momento do registro: `1/1`, `2/3`, `100%`, `intermitente`, etc.                                                                                                              |
| **Correção**             | PR (`#NN` com link) ou commit (`<sha7>`) que fecha o defeito, ou literal `não corrigido` quando ainda aberto.                                                                                                 |
| **Justificativa de não-correção** | Texto curto obrigatório quando a coluna `Correção` for `não corrigido`, citando contorno ou motivo.                                                                                                |

A coluna `Correção` admite três valores: (a) link de PR mergeado;
(b) SHA curto de commit mergeado em `main`; (c) literal
`não corrigido`. Para PRs ainda abertos, o registro fica como
`não corrigido` com a justificativa apontando o card da filha em
que a correção está em curso.

## 3. Política e fluxo de triagem

A triagem é responsabilidade do autor (único responsável pelos
marcos do projeto, conforme `docs/ROADMAP.md`):

1. **Detecção** — qualquer defeito identificado durante execução
   dos casos `TF-NN`, nas sessões com os 5 colegas ADS (E4.2), na
   validação dos *pipelines* de CI, ou em revisão de código, é
   **imediatamente** registrado nesta tabela, mesmo que de forma
   provisória. *Não acumular*: defeitos não registrados não
   contam para o critério de pronto do E4.3.
2. **Classificação** — feita pelo autor no momento do registro
   (mesmo dia). Casos duvidosos são provisionados como `Bloq.` até
   desambiguação; a reclassificação para `Crit.` ou `Men.` requer
   justificativa quando implicar mudança de prazo.
3. **Correção ou justificativa** — defeitos `Bloq.` e `Crit.`
   recebem PR de correção até o **congelamento E4.8 (27/11/2026)**;
   defeitos `Men.` são corrigidos se houver capacidade ou
   formalmente justificados.
4. **Fechamento** — após merge do PR, atualizar a coluna `Correção`
   com o link do PR ou SHA e remover a justificativa. Sem
   `Correção` preenchida (≠ `não corrigido`), nem a entrega N2 é
   considerada completa.

### 3.1. Procedência dos defeitos (fontes aceitas)

Para evitar a invenção de defeitos, **só são registrados nesta
tabela itens cuja origem seja verificável em um artefato
versionado**:

| Fonte                              | Onde verificar                                              |
|------------------------------------|-------------------------------------------------------------|
| Falha de pipeline CI               | `gh run view <id>` ou `docs/qa/run-log-t_*.md`              |
| Issue aberta no repositório        | `gh issue list` (não aplicável neste ciclo: nenhuma issue aberta no `joao-pedro-gms/BrainOut` antes desta entrega) |
| Achado de revisão de código        | Histórico do PR ou comentário de revisão                    |
| Achado em artefato já documentado  | `docs/CI-CD.md`, `docs/ARQUITETURA.md`, etc.                |
| Execução de caso de teste `TF-NN`  | `docs/ROTEIRO-TESTES.md` (coluna *Resultado observado*)     |
| Sessão de usabilidade `Pn-Tm`      | `docs/USABILIDADE.md` (Parte 2 — a preencher após 17/11/2026) |

Casos `TF-NN` ainda não executados (coluna *Resultado observado*
em branco) e sessões de usabilidade ainda não realizadas **não
geram defeitos preventivos** — defeitos só entram após detecção
real.

## 4. Regra de congelamento (E4.8 — 27/11/2026)

A partir de **27/11/2026**, a aceitação de novos defeitos passa a
ser restrita:

- Apenas defeitos classificados como `Bloq.` ou `Crit.` podem ser
  registrados e entram na fila de correção do E4.8.
- Defeitos `Men.` identificados após esta data são **automaticamente
  movidos para o backlog pós-N2** (a registrar no relatório
  técnico E5.1, Apêndice A.2) e devem ser marcados como
  `não corrigido` com justificativa `Backlog pós-N2`.
- A regra acima também vale para reclassificações: um defeito
  registrado como `Men.` antes do congelamento e reclassificado
  para `Bloq.` ou `Crit.` após essa data é tratado como se
  tivesse sido detectado dentro do prazo.

## 5. Tabela de defeitos

A numeração é contínua: linhas novas devem receber o próximo
`DEF-NN` disponível e manter a ordem cronológica de detecção
(do mais antigo para o mais recente).

| ID     | Origem              | Descrição                                                                                                                                                                                                  | Sev.  | Frequência | Correção                                                                                     | Justificativa de não-correção |
|--------|---------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------|------------|----------------------------------------------------------------------------------------------|--------------------------------|
| DEF-01 | CI run 35597593408  | `validateSigningRelease` falha em `Release APK/AAB`: keystore é gravado na raiz do runner (`keystore.jks`), mas `file(brainoutStoreFile!!)` em `:app/build.gradle.kts` resolve o path contra o diretório do módulo `app/`. | Crit. | 1/1        | PR [#43](https://github.com/joao-pedro-gms/BrainOut/pull/43) — decodifica keystore em `${{ runner.temp }}/brainout-release.jks` (path absoluto). | —                              |
| DEF-02 | CI run 35598981118  | `signReleaseBundle` falha com `Get Key failed: Given final block not properly padded`: secret `BRAINOUT_KEY_PASSWORD` difere da senha que abre a chave dentro do PKCS12 (em PKCS12 a senha da chave é a senha do keystore). | Crit. | 1/1        | não corrigido                                                                                 | Contorno documentado em `docs/CI-CD.md` (PR [#47](https://github.com/joao-pedro-gms/BrainOut/pull/47)): `keytool -genkeypair -storetype PKCS12` deve usar a MESMA senha em `-storepass` e `-keypass`, e ambos os secrets cadastrados com esse valor. Não há correção de código a fazer — a falha só se repete se o operador gerar o keystore com senhas divergentes. |
| DEF-03 | QA manual (run-log) | CI run 35599507321 (`v0.3.1-ciclo3`, 2ª tentativa) bem-sucedido após DEF-01/02; documentado em `docs/qa/run-log-t_333ab600.md` como evidência da trilha de release (`brainout-release-aab-v0.3.1-ciclo3`, 4,2 MB). | Men.  | 1/1        | PR [#48](https://github.com/joao-pedro-gms/BrainOut/pull/48) — registro do run-log.           | Não é defeito de produto: marco de *release* válido (`v0.3.1-ciclo3`); registrado para rastreabilidade da cadeia DEF-01 → DEF-02 → DEF-03 e para evidência de que o contorno de DEF-02 produziu artefato assinado. |
| DEF-04 | QA manual (wireframes) | Diagrama do wireframe low-fi (`docs/wireframes/lo-fi.html`) tinha setas entre fileiras sobrepostas à anotação "Voltar para Home" da tela 4, confundindo o fluxo de retorno pós-logout (fileira 2 → fileira 1). | Men.  | 1/1        | commit [`f528ff1`](https://github.com/joao-pedro-gms/BrainOut/commit/f528ff1) — refatoração profunda do layout das fileiras + curva tracejada única entre fileiras 2 e 1. | —                              |
| DEF-05 | QA manual (CI-CD.md) | Comando `keytool -genkeypair` em `docs/CI-CD.md` (seção "Como gerar o keystore localmente") estava sem flag `-keyalg` explícita; chave poderia ser gerada com algoritmo fraco dependendo do JDK.                    | Men.  | 1/1        | PR [#47](https://github.com/joao-pedro-gms/BrainOut/pull/47) — adicionadas flags `-keyalg RSA -keysize 2048`. | —                              |
| DEF-06 | QA manual (i18n)    | 8 chaves `deadline_*` introduzidas pelo E3.6 em `app/src/main/res/values/strings.xml` (PT) não tinham par em `app/src/main/res/values-en/strings.xml`: `deadline_channel_name`, `deadline_channel_description`, `deadline_notification_title`, `deadline_notification_body`, `deadline_action_complete`, `deadline_unknown_project`, `deadline_permission_denied_toast`, `deadline_permission_rationale_title`. Com a locale `en` ativa, as notificações de prazo aparecem em PT. A regra `lint { abortOnError = true; error += "MissingTranslation" }` no `:app` (introduzida pelo PR #49) não pegou porque a pipeline CI roda `ktlint + detekt` (não `lintDebug`). | Crit. | 100% (todas as execuções com locale `en`) | **CORRIGIDO** — commit [`d171d25`](https://github.com/joao-pedro-gms/BrainOut/commit/d171d25) (PR [#57](https://github.com/joao-pedro-gms/BrainOut/pull/57), *bump toolchain*, mergeado em `main` em 21/09/2026) | — (reverificado em 08/10/2026; ver nota abaixo) |
| DEF-07 | QA manual (holidays) | Em `main`, o módulo de feriados (E3.5) ainda não havia recebido PR de polimento (assinatura de testes, `@Suppress("NewApi")` para `java.time.*` em `minSdk 24`, supressões justificadas de detekt para `TooManyFunctions`/`LongParameterList`/`LongMethod`, e a adição das traduções EN para `deadline_holidays_unavailable` e `deadline_not_business_day`). detekt e `lintDebug` falhavam em `:core:data` e `:feature:projects`. | Men.  | 100% (`./gradlew detekt` e `lintDebug`) | **CORRIGIDO** — PR [#59](https://github.com/joao-pedro-gms/BrainOut/pull/59) (`feat(holidays): integrar consulta de feriados e prazos (E3.5)`), mergeado em `main` em 21/09/2026 como commit [`bb95109`](https://github.com/joao-pedro-gms/BrainOut/commit/bb95109) | — (o PR #51 foi **fechado sem merge**; ver nota abaixo) |
| DEF-08 | QA manual (build de `:core:data` androidTest) | A suíte instrumentada de `:core:data` **não compilava**: `MigrationTest` e `UserDaoInstrumentedTest` importam `com.google.common.truth.Truth` e `kotlinx.coroutines.test.runTest`, mas as dependências estavam declaradas apenas em `testImplementation`. `compileDevDebugAndroidTestKotlin` falhava com ~40 erros `Unresolved reference` — nenhuma das três migrations do Room (`MIGRATION_1_2`, `MIGRATION_2_3`, `MIGRATION_3_4`) era exercitada em lugar nenhum, nem local nem no CI (que também não invoca `connectedAndroidTest`). | Crit. | 100% (toda tentativa de rodar a suíte instrumentada) | **CORRIGIDO nesta sessão** — `core/data/build.gradle.kts:181-182` (`androidTestImplementation(libs.truth)` + `androidTestImplementation(libs.kotlinx.coroutines.test)`); a suíte passa a compilar e `MigrationTest` cobre 2→3 e 3→4 em SQLite real | — |
| DEF-09 | QA manual (gate de estilo) | O plugin ktlint era aplicado **apenas na raiz** do build. O `build.gradle.kts:16` afirmava que o plugin "cobre automaticamente todos os subprojetos", o que é falso: `./gradlew ktlintCheck` inspecionava 2 arquivos de build da raiz e **zero dos 221 arquivos `.kt` versionados** (contagem de `git ls-files '*.kt'` = 221). Confirmado em disco — só existia `build/reports/ktlint/` na raiz, nenhum relatório nos módulos. O gate de estilo do CI era verde por construção. | Crit. | 100% (todo run de `ktlintCheck`) | **CORRIGIDO nesta sessão** — `build.gradle.kts:112-129` (`subprojects { apply(plugin = "org.jlleitschuh.gradle.ktlint") }` com `android = true`); agora há relatório de ktlint nos 9 projetos (`app`, `core/{data,domain,ui}`, `feature/{auth,projects,settings,tasks}`) | — |
| DEF-10 | QA manual (gate de estilo) | Com o ktlint finalmente alcancando os 9 projetos (DEF-09), surgiram violações cosméticas em 17 arquivos (indentação, quebra de chamada, vírgula final) em código com 129 commits já aceitos por revisão humana. Não são bugs de produto e corrigi-los exigia uma reformatação massiva e revisável. | Men. | 100% (`./gradlew ktlintCheck` nos 9 projetos) | **CORRIGIDO** — `ktlintFormat` nos 9 subprojetos (commit `b59318e`) + acerto das 9 violações não auto-corrigíveis (commit `b6db6e0`): **770 → 0**. `ignoreFailures` voltou para `false` (commit `631a4ba`), então o gate volta a bloquear de verdade. | Fechado. As 9 violações restantes dividiam-se em duas famílias, tratadas separadamente: **7 de convenção** (`const val Home` em `ProjectsRoutes`/`SettingsRoutes`/`NeoScrim.alpha` contra screaming snake case) resolvidas no `.editorconfig` como `ktlint_standard_property-naming = disabled` — renomear quebraria a API pública de `:core:ui` consumida por `BrainOutNavHost` e pelos testes de navegação; **3 de documentação** (KDoc de arquivo seguido de comentário avulso em `Color.kt`/`RemoteDtos.kt`, backing field `_searchInput` cujo par público é `searchQuery`, e a factory PascalCase `SyncPayloadSerializer`) resolvidas no código. As renomeações que Fiz foram só de símbolos **privados**; a API pública ficou intacta. Verificado: 662 testes, 0 falhas, e `ktlintCheck` verde com `ignoreFailures = false` nos 9 subprojetos. |
| DEF-11 | QA manual (higiene do repositorio) | O `.gitignore` **nao cobria `.venv`**, e o `README.md:148` manda criar `backend-stub/.venv` (`python -m venv .venv`). Um `git add` de rotina na pasta `backend-stub/` passaria a versionar o virtualenv inteiro (milhares de arquivos, alguns com binarios por plataforma). O bloco "Python artifacts" cobria `__pycache__/`, `*.pyc` e caches, mas nenhum padrao de diretorio de ambiente virtual. | Men. | intermitente (depende de quem roda `git add` e de onde) | **CORRIGIDO nesta sessao** — `.gitignore:66-73` agora cobre `.venv/`, `venv/`, `env/` e `backend-stub/.venv/`. O padrao especifico vem por ultimo de proposito: no git, o ultimo padrao que casa e o que vale. **Verificado** com `git check-ignore -v` sobre um `backend-stub/.venv/teste.py` sintetico — retorna `.gitignore:72:backend-stub/.venv/`. | — |
| DEF-12 | QA manual (ambiente de execucao) | `backend-stub/requirements.txt` fixava `pydantic==2.9.2`, que resolve para `pydantic-core==2.23.4` — sem wheel para **Python 3.14**, e `PyO3` declara suporte maximo ate 3.13. `pip install -r requirements.txt` caia no fallback sdist e falhava ao tentar compilar via Rust, com `the configured Python interpreter version (3.14) is newer than PyO3's maximum supported version (3.13)` — minutos de build por um erro que nao era do projeto. O projeto declara Python 3.12 (README, Dockerfile, AGENTS.md), entao a incompatibilidade era do pin antigo, nao do alvo. | Men. | 100% (toda instalacao em Python 3.14) | **CORRIGIDO nesta sessao** — pins exatos relaxados para faixas: `fastapi>=0.118.3,<1.0`, `uvicorn>=0.38.0,<1.0`, `pydantic>=2.12,<3.0`, `pytest>=8.4,<10`, `httpx>=0.28.1,<1.0`. Cada piso e a primeira release que declara suporte a 3.14, entao o mesmo arquivo instala em 3.12, 3.13 e 3.14 sempre por wheel, sem toolchain. Os tetos existem so para barrar majors. **Verificado** em venv limpo com `--only-binary=:all:`: instala sem compilar nada, `pytest` **16 passed**, e o stub sobe com `uvicorn` respondendo em `/health`. | — |
| DEF-13 | QA manual (emulador API 37) | `LoginScreenTest` (`feature/auth/src/androidTest`) **compilava**, mas os 3 testes falhavam em **runtime** no emulador API 37 com `java.lang.NoSuchMethodException: android.hardware.input.InputManager.getInstance []`, disparada em `androidx.test.espresso.Espresso.onIdle` (chamado por dentro em `androidx.compose.ui.test.runEspressoOnIdle`). Causa: **Espresso 3.6.1** usava `InputManager.getInstance`, metodo removido da plataforma; a estrategia de injecao de eventos da propria biblioteca quebrava em API 37. | Crit. | 3/3 testes | **CORRIGIDO nesta sessão** — `espresso` 3.6.1 -> **3.7.0** no catalogo. A 3.7.0 contem o commit android/android-test `fddcda4` ("Cleanup reflection usage in InputManagerEventInjectionStrategy"), que troca `InputManager.getInstance` por `Context.getSystemService` em API > 23. O `minSdk` do espresso sobe para 24 na 3.7.0, que ja e o `minSdk` do projeto. **Verificado no emulador**: `connectedDebugAndroidTest` -> 3 testes, 0 falhas. | — |
| DEF-14 | QA manual (metrica de cobertura) | O gate do Kover excluia do denominador os pacotes `*.remote.*` e `*.di.*`. Medido: em `:core:data` isso tirava **806 linhas** de codigo de producao (`remote/` = 593, incluindo `RemoteDataSource.kt` com 253 linhas e 26 blocos `catch`; `di/DataModule.kt` = 213) de um total de 3.944. O comentario no codigo justificava `*.remote.*` como "DTOs de rede (mapeamento puro, coberto via repositories)", mas o padrao excluia a camada HTTP inteira — inclusive `RemoteDataSource`, que **tem** teste (`BrainOutApiTest` com MockWebServer). | Men. | 100% (metrica do gate) | **CORRIGIDO nesta sessão** — `*.remote.*` estreitado para `*Dto` e `*Dto$*`, que e o que a justificativa original realmente pretendia. O padrao passou a bater so com as data classes de DTO; `RemoteDataSource`, `BrainOutApi` e `HolidayRemoteDataSource` voltaram ao denominador. `*.di.*` **foi mantido**: `DataModule` tem 19 metodos `@Provides` e zero logica — e wiring, nao codigo testavel — mas o comentario agora diz isso explicitamente. **Numeros reais apos a correcao**: `:core:domain` 90,0% de linhas (era 84,9%), `:core:data` **83,6%** (era 84,1%) — caiu 0,5 ponto porque `remote` entrou no denominador, e o gate de 60% continua passando. A metrica deixou de estar inflada. | — |
| Arquivo | Estado |
|---|---|
| `core/data/.../remote/HolidayApi.kt` | presente (29 linhas) |
| `core/data/.../remote/HolidayDto.kt` | presente (33 linhas) |
| `core/data/.../remote/HolidayRemoteDataSource.kt` | presente (48 linhas) |
| `core/data/.../repository/HolidayRepositoryImpl.kt` | presente (23 linhas) |

Todos os itens que a descrição do defeito exigia estão
satisfeitos: `HolidayRemoteDataSource.kt:29-30` tem o cache em
memória com `Mutex`/`withLock`; as traduções
`deadline_holidays_unavailable` e `deadline_not_business_day`
existem em `feature/projects/src/main/res/values-en/strings.xml:41,43`;
e a suíte `HolidayRepositoryTest` (159 linhas, 6 casos) está em
`core/data/src/test/`. Entraram pelo **PR #59** (mergeado em
21/09/2026, `bb95109`). O **PR #51 citado na justificativa original
foi fechado sem merge** — `gh pr view 51` retorna
`state: CLOSED`, `mergedAt: null`; o conteúdo dele chegou a `main`
pelo PR #59, não pelo #51. Por isso a referência a "#51 ainda
aberto" não descreve mais o repositório.

### 5.1 — Revisão da gravação de tarefas (09/10/2026)

Origem `Revisão`: auditoria da cadeia de gravação de tarefas
(`TaskRepositoryImpl` → `TaskDao` + `PendingOpDao`), com verificação
**empírica** de cada achado em teste real contra o Room em memória
(Robolectric) antes de qualquer correção — nenhum item desta seção é
inferido só por leitura de código. Testes de evidência em
`core/data/src/test/.../TaskSaveAuditTest.kt` (11 casos). Suíte
completa na revisão: **662 testes, 0 falhas**.

| ID     | Origem              | Descrição                                                                                                                                                                                                 | Sev.  | Frequência | Correção                                                                                     | Justificativa de não-correção |
|--------|---------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------|------------|----------------------------------------------------------------------------------------------|--------------------------------|
| DEF-15 | Revisão (gravação de tarefas) | `TaskRepositoryImpl.update` gravava uma tarefa que não existia no banco local **e** enfileirava a op de sincronização. Causa: o `@Update` do Room é um no-op silencioso quando a linha não existe (`UPDATE tasks SET ... WHERE id = ?` afeta 0 linhas, sem erro), então o repositório devolvia a entidade como se tivesse sido persistida. Efeito observável: o `SyncWorker` enviava um PUT upsert e o backend criava no servidor uma tarefa que não existia no device; a fila marcava a operação como concluída, e a divergência só apareceria no próximo `list_tasks`. | Crit. | 1/1 | **CORRIGIDO nesta sessão** — guarda de existência em `update()`, lançando `TaskNotFoundException` **antes** do `enqueueInTx`; a fila permanece limpa. Verificado por `ACHADO 2 update de tarefa inexistente lanca e nao enfileira op` (antes da correção o teste provava 1 op órfã na fila). | — |
| DEF-16 | Revisão (gravação de tarefas) | `TaskRepositoryImpl.completeAndCascade` retornava no-op para tarefas já em `DONE`, sem reidratar a flag `projects.is_completed`. Tarefas concluídas antes da v3 (antes da coluna `completed_at`) ficavam com o projeto permanentemente ativo mesmo com **zero** tarefas ativas — a RN03 (E2.5) nunca era aplicada a esses registros. Efeito observável: projeto concluso pelo usuário nunca somia da lista de ativos. | Crit. | 1/1 | **CORRIGIDO nesta sessão** — `repairCompletedProject()` reconta as ativas e marca o projeto quando o total é zero; reparo puramente local e idempotente, sem gerar op pendente (o backend deriva a mesma marca pelo mesmo caminho de negócio). Coberto por `ACHADO 4` e `ACHADO 4b` (que impede a marcação indevida quando ainda há tarefas ativas). | — |
| DEF-17 | Revisão (gravação de tarefas) | `TaskSyncPayload` serializa apenas `id`, `project_id`, `title`, `priority` e `done`. `due_date` e `assignee_id` **não** entram no payload de sincronização — e o contrato do stub (`backend-stub/server.py`, `TaskUpsert`) também não os modela. Efeito observável: prazo e responsável configurados no app nunca chegam ao servidor; um `list_tasks` posterior devolve tarefas sem prazo. Como o lembrete de prazo é agendado localmente (WorkManager, E3.6), o usuário **não** percebe a perda na sessão — ela só aparece ao trocar de device ou inspecionar o backend. | Crit. | 1/1 | não corrigido | Correção exige mudança de **contrato**, não só de código cliente: adicionar `due_date`/`assignee_id` em `TaskSyncPayload` e nos modelos Pydantic do stub (`TaskIn`/`TaskUpsert`), com retrocompatibilidade (campos opcionais) para installs já sincronizados. Fora do escopo de um patch local; a decisão sobre escopo é do roadmap E3.3/sync. Impacto funcional para o usuário: nenhum na sessão corrente — o app segue funcional, apenas com o dado incompleto no servidor. |
| DEF-18 | Revisão (gravação de tarefas) | A fila `pending_ops` não faz *coalescing*: cada `update()` enfileira uma op completa com o payload integral da tarefa. Verificado: 3 edições sucessivas da mesma tarefa geram 3 ops distintas, todas com payloads diferentes. Efeito observável: offline prolongado acumula envios redundantes ao servidor; o `SyncWorker` drena em ordem e para no primeiro erro, então a fila cresce sem limite proporcional às edições reais do usuário. | Men. | 100% (cada sequência de edições da mesma tarefa) | não corrigido | Contorno: o `SyncWorker` já trata falha permanente descartando a op (`SyncOutcome.Permanent`), então a fila não trava permanentemente — apenas envia trabalho redundante. O ganho real vem de um `upsert` por entidade (substituir a op anterior do mesmo `entity_id` em vez de anexar), que é mudança de semântica da fila e merece decisão própria. Backlog pós-N2 se sobrepuser ao congelamento E4.8 (27/11/2026). |
| DEF-19 | Revisão (gravação de tarefas) | `TaskDao.updateStatus` grava apenas a coluna `status`, sem tocar em `completed_at`. Uma tarefa levada a `DONE` por esse caminho fica concluída sem carimbo temporal, quebrando o invariante de RN03 (`Task.init` só aceita `completedAt != null` quando `status == DONE`, mas o inverso — DONE sem carimbo — é aceito como registro legado). Efeito observável: a tarefa nunca conta em `doneThisWeekCount`, distorcendo a taxa de conclusão semanal do Dashboard. **Latente**: `updateStatus` não tem chamador de produção hoje (só a suíte de testes o exercita) — o defeito se manifestaria no primeiro uso do método. | Men. | 100% (todo uso de `updateStatus`) | **CORRIGIDO** — PR [#146](https://github.com/joao-pedro-gms/BrainOut/pull/146) (commit [`6b20eff`](https://github.com/joao-pedro-gms/BrainOut/commit/6b20eff)): o método `TaskDao.updateStatus` foi **removido** (não migrado), por decisão: tinha **zero** chamadores de produção, era duplicata estrita de `updateStatusAndCompletedAt` (que `cascadeCompleteTask`/`cascadeReopenTask` já usavam), e duas queries com semânticas diferentes sobre a mesma coluna é exatamente o que tornou o defeito possível. Removido, o invariante de RN03 fica **estrutural**: não existe mais caminho que grave `status` sem o par `completed_at`. Auditoria do resto de `TaskDao`: o único outro gravador de `status` é `updateStatusAndCompletedAt` (via `@Update insert/update` gravam a entidade inteira, nunca só o status). Coberto por `ACHADO 6` (concluir ⇒ `status = DONE` **e** `completed_at` não-nulo **e** a tarefa conta em `doneThisWeekCount`) e `ACHADO 6b` (reabrir limpa o carimbo) — testes de **invariante**, não de método: o `ACHADO 6` original provava o defeito e foi invertido. Mutação verificada: reintroduzir uma escrita só de `status` faz esses testes falharem (`expected not to be: null`). | — |
| DEF-20 | Revisão (gravação de tarefas) | `reopenAndCascade` reabre uma tarefa para estado ativo sem revalidar o teto de RN01 (50 tarefas ativas por projeto). O limite só é verificado em `CreateTaskUseCase`. Efeito observável: um projeto com 50 tarefas ativas e 1 concluída chega a **51** tarefas ativas pela reabertura, violando RN01 sem erro — a regra não é um invariante do sistema, apenas um guard de criação. | Men. | 100% (toda reabertura em projeto no teto) | **CORRIGIDO** — PR [#149](https://github.com/joao-pedro-gms/BrainOut/pull/149) (merge [`c7724ad`](https://github.com/joao-pedro-gms/BrainOut/commit/c7724ad)): a correção é do domínio, como a própria justificativa pedia — `ChangeTaskStatusUseCase.ensureReopenFitsActiveCap` consulta `countActiveByProject` e rejeita com `ProjectTaskLimitReachedException` (a mesma exceção e a mesma mensagem do caminho de criação) **antes** de `reopenAndCascade`. Rejeita em `active >= 50` porque a tarefa reaberta está em DONE e não entra na contagem: o pós-estado é `active + 1`, exatamente a pré-condição que `CreateTaskUseCase` aplica. O repositório continua sem guarda (persistência; `reopenAndCascade` tem outros chamadores) e `TaskSaveAuditTest.ACHADO 7` segue mostrando que a camada DAO não revalida. `ProjectDetailViewModel.changeStatus` ganhou `catch (ProjectTaskLimitReachedException)` para não cair no fallback genérico de carga. 6 testes novos (5 de domínio, 1 de VM); mutação verificada — sem a chamada do guard, 3 falham. | — |

### 5.2 — Revisão de UI/UX e acessibilidade (09/10/2026)

Origem `Revisão`: auditoria das telas com verificação no fonte. As
severidades seguem a Seção 1. Os achados de contraste foram
**recalculados** a partir dos tokens Neo (`NeoColor.kt`) e não
aceitos por leitura.

| ID     | Origem              | Descrição                                                                                                                                                                                                 | Sev.  | Frequência | Correção                                                                                     | Justificativa de não-correção |
|--------|---------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------|------------|----------------------------------------------------------------------------------------------|--------------------------------|
| DEF-21 | Revisão (UI/UX) | `TasksScreen` e `DashboardScreen` não usavam `Scaffold`: sem `TopAppBar`, sem `navigationIcon` e sem consumo de `innerPadding`. Combinado com `enableEdgeToEdge()` em `MainActivity.kt:78` (que só 5 telas de 9 compensavam), o título ficava sob a barra de status e a lista sob a barra de gestos. Pushed a partir da bottom bar, o botão do sistema voltava mas não havia voltar visível. | Crit. | 100% (abertura de ambas as telas) | **CORRIGIDO nesta sessão** — ambas envolvidas em `Scaffold` com `TasksTopBar`/`DashboardTopBar` (`navigationIcon` + callback), `innerPadding` consumido, e as strings novas em `values/` **e** `values-en/` | — |
| DEF-22 | Revisão (UI/UX) | Seis chips informativos usavam `AssistChip(onClick = {})` — `HomeProjectCard.kt:95`, `HomeTopBar.kt:78`, `TaskRow.kt:233,248`, `TasksScreen.kt:324,361`. O TalkBack anuncia um `AssistChip` como acionável e o callback vazio engole toques dentro do card do projeto. `DESIGN.md §6` é explícito: *"chips informativos não são botões, não callbacks vazios"*. | Crit. | 100% (exibição de qualquer card de projeto) | **CORRIGIDO nesta sessão** — novo componente `NeoInfoChip` em `:core:ui` (não interativo por construção) substituindo os seis; `clearAndSetSemantics` nos casos em que o chip é decorativo dentro de um rótulo já falado | — |
| DEF-23 | Revisão (UI/UX) | `HomeFilterRow.kt:138` usava `Icons.Filled.Add` (um **mais**) como marcador de "selecionado" no menu de ordenação. O KDoc na linha 98 fala em *checkmark*; o código desenha Mais, que o usuário lê como "adicionar". Além disso `home_filter_aria_label` (linhas 78-82) era renderizado como `Text` visível entre os chips, sendo que o próprio KDoc o chama de "rótulo de acessibilidade oculto" — nada o esconde. | Crit. | 100% (abertura do menu de ordenação) | **CORRIGIDO nesta sessão** — `Icons.Filled.Check` no item ativo; o rótulo saiu do fluxo visual e foi para `Modifier.semantics { contentDescription = … }` | — |
| DEF-24 | Revisão (UI/UX) | **Contraste.** Oito sites aplicam `alpha` reduzido sobre tokens que, sozinhos, passam do limite com folga. Medido contra os tokens reais (`NeoPrimitive.neutral E5E5DE`, `graphite 303030`, `ink 181818`, fundos `paper F4F4F0` e `night 191919`): `outline` puro dá **16,1:1** e `outline.copy(alpha = 0.3f)` dá **1,94:1**; os divisores (6 sites) são corrigíveis. Já `surfaceVariant` puro dá **1,15:1** (claro) e **1,32:1** (escuro) — abaixo de 3:1 **sem** o alpha. WCAG 2.1 §1.4.11 exige 3:1 para o que **delimita visualmente** uma região; um preenchimento quase idêntico ao fundo não delimita, quem delimita é a borda. | Men. | 100% (exibição de divisores e empty states) | **PARCIAL** — divisores corrigidos (passam a `outline` em alpha 1.0, 16,1:1). Empty states **não corrigidos** | O preenchimento dos empty states fica em `surfaceVariant` sem alpha, o que é a melhor leitura honesta do token (o alpha só piorava, de 1,15 para 1,09). A alternativa seria introduzir um token de superfície com contraste ≥3:1, o que altera a identidade visual Neo — decisão de design do responsável, não correção de bug. O teste `EmptyStateContrastTest` fixa os dois lados do invariante para que a escolha fique explícita e não silenciosa. |
| DEF-25 | Revisão (UI/UX) | `renameTask` era um caminho morto: `ProjectDetailScreen.kt:134` passa `onRenameTask = viewModel::renameTask`, o ViewModel implementa (`ProjectDetailViewModel.kt:348`) e o use case existe — mas `ProjectDetailBody.kt:41` declara o parâmetro `@Suppress("unused")` e **nenhum item de menu o chama**. Código completo, inalcançável da UI. | Men. | 100% (nunca executável) | não corrigido | Decisão de produto: ligar ao menu (adiciona um item "Renomear" gated a tarefas não concluídas, como o de prioridade) **ou** remover o parâmetro, o método do ViewModel e o parâmetro do use case. Enquanto a decisão não sai, `grep renameTask` retorna 3 pontos que não chegam ao usuário. Backlog pós-N2 se sobrepuser ao congelamento E4.8. |
| DEF-26 | Revisão (UI/UX) | Três padrões divergentes de resolução de mensagem de erro. `HomeErrorBanner.kt:86` resolve a chave canônica para a string localizada; `ProjectDetailStates.kt:64` e `TasksScreen.kt:140` recebem `message` e a **descartam** (`@Suppress("UNUSED_PARAMETER")`), exibindo sempre o texto fixo de "falha ao carregar". Efeito observável: uma falha de escrita (`ERROR_ACTION_FAILED`, "Não foi possível concluir a operação") é indistinguível de uma falha de leitura do Room para o usuário. | Men. | 100% (qualquer erro em ProjectDetail e Tasks) | não corrigido | Contorno: nenhum automático — a mensagem exibida é sempre verdadeira no caso do banner de carga, apenas genérica demais no caso de escrita. Depende do DEF-21 fechar (as telas passam a ter `Scaffold` e um único ponto de exibição), então corrigir antes causaria conflito. Backlog pós-N2 se sobrepuser ao congelamento E4.8. |

**Hipótese refutada (registrada para rastreabilidade):** a revisão
suspeitou inicialmente de um crash em
`TaskDao.observeCompletionStats` — `SUM(...)` devolve `NULL` quando a
agregação não percorre nenhuma linha, e a propriedade de destino em
`CompletionStatsRow` é `Int` não-nula. **O defeito não existe**:
verificado contra Room real, `Cursor.getInt` coalesce `NULL` para `0`
e o resultado é o correto. Três testes de regressão foram mantidos
(`REFUTADO 1`, `1b`, `1c`) para travar esse comportamento, já que ele
depende de um detalhe implícito do driver em vez de um `COALESCE`
explícito na query — fragilidade documentada, não defeito ativo.

## 6. Apêndice A — Referências cruzadas

- Política de severidade e marco E4.3: `docs/ROADMAP.md` (bloco do
  Ciclo 4, marcos).
- Casos funcionais que alimentam a coluna *Origem* (`TF-NN`):
  `docs/ROTEIRO-TESTES.md` (TF-01..TF-14, E4.1).
- Sessões que alimentam a coluna *Origem* (`Sessão Pn-Tm`):
  `docs/USABILIDADE.md` (Parte 2 — preenchida após a execução
  presencial da semana de 17/11).
- Critério de pronto da N2 (E5.1) referencia esta tabela no
  Apêndice A.2 do relatório técnico.
- Congelamento e regra de fila pós-27/11: E4.8 no
  `docs/ROADMAP.md`.
- CI-CD e trilha de release assinada: `docs/CI-CD.md`,
  `docs/qa/run-log-t_333ab600.md`.
- Reverificação de 08/10/2026 dos estados de DEF-06 e DEF-07 (com
  os comandos de conferência): Seção 5.1 deste documento. Para o
  estado dos P0 do mesmo dia, ver
  `docs/AUDITORIA-2026-09-24.md` (§ Reverificação 2026-10-08).

---

*Documento elaborado por **João Pedro G M Silva** — ADS, PUC Goiás
(mat. 20251012000740), como entrega do marco **E4.3** do Ciclo 4
do Projeto Integrador.*