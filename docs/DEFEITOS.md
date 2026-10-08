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
| DEF-10 | QA manual (gate de estilo) | Com o ktlint finalmente alcancando os 9 projetos (DEF-09), surgiram **191 violações cosméticas em 17 arquivos** (indentação, quebra de chamada, vírgula final) em código com 129 commits já aceitos por revisão humana. Não são bugs de produto e corrigi-los exige uma reformatação massiva e revisável. | Men. | 100% (`./gradlew ktlintCheck` nos 9 projetos) | não corrigido — **`ignoreFailures = true`** até a reformatação dedicada | Contorno deliberado e **pendente**: `build.gradle.kts:128` mantém `ignoreFailures.set(true)` para que o gate comece a fiscalizar de verdade (os relatórios passam a ser produced e arquivados) sem bloquear todo PR enquanto o backlog de estilo não é tratado. O `.editorconfig` da raiz já reduz o ruído — sem ele a engine contava também as ~200 funções `@Composable` reprovadas por `function-naming`. **Plano pendente**: abrir um item dedicado de reformatação — `./gradlew ktlintFormat` em um PR isolado, com o diff conferido módulo a módulo e detekt + testes rodando ao final; ao concluir, voltar `ignoreFailures` para `false` e remover este registro. Registrado como `Men.` e, portanto, se sobrepuser ao congelamento E4.8 (27/11/2026), movível para backlog pós-N2 conforme a Seção 4. |
| DEF-11 | QA manual (higiene do repositório) | O `.gitignore` **não cobre `.venv`**, e o `README.md:148` manda criar `backend-stub/.venv` (`python -m venv .venv`). Um `git add` de rotina na pasta `backend-stub/` passaria a versionar o virtualenv inteiro (milhares de arquivos, alguns com binários por plataforma). O bloco "Python artifacts" do `.gitignore` cobre `__pycache__/`, `*.pyc` e caches, mas nenhum padrão de diretório de ambiente virtual. | Men. | intermitente (depende de quem roda `git add` e de onde) | não corrigido | **Fora do escopo desta entrega** (correção não aplicada): o padrão `backend-stub/.venv/` deve ser adicionado ao `.gitignore`. É alteração de um arquivo fora dos dois documentos desta revisão; registrado aqui para que a correção seja feita em commit próprio, junto da verificação de `git status --short` antes de qualquer staging. Contorno atual: nunca usar `git add .`/`git add -A` dentro de `backend-stub/` (regra já válida pelo `AGENTS.md`, "Never stage gitignored files"). |
| DEF-12 | QA manual (ambiente de execução) | `backend-stub/requirements.txt` fixa `pydantic==2.9.2`, que resolve para `pydantic-core 2.23.4` — sem wheel disponível para **Python 3.14**, e `PyO3` (dependência transitiva) declara suporte máximo até 3.13. `pip install -r requirements.txt` falha em qualquer máquina com 3.14. O projeto declara Python 3.12 (README, `Dockerfile python:3.12-slim`, AGENTS.md), então a incompatibilidade é do pin antigo, não do alvo. | Men. | 100% (toda instalação em Python 3.14) | não corrigido | Contorno: usar Python 3.12 (versão declarada pelo projeto), onde a instalação funciona. Correção proposta e **pendente**: relaxar os pins para faixas que aceitem 3.14 (`pydantic>=2.9` com `pydantic-core` compatível, ou fixar o `requires-python` do stub em `3.12`/`3.13` para transformar a incompatibilidade em erro explícito de instalação em vez de falha de resolução). Os 16 testes do stub passam com as versões atuais. |
| DEF-13 | QA manual (emulador API 37) | `LoginScreenTest` (`feature/auth/src/androidTest`) **compila**, mas os 3 testes falham em **runtime** no emulador API 37 com `java.lang.NoSuchMethodException: android.hardware.input.InputManager.getInstance []`, disparada em `androidx.test.espresso.Espresso.onIdle`. Causa: **Espresso 3.6.1** (`gradle/libs.versions.toml:43`) é incompatível com `compileSdk 37`; o atalho foi removido/reescrito nessa API. O Espresso entra em `feature/auth/build.gradle.kts:112` (`androidTestImplementation(libs.androidx.espresso.core)`). | Crit. | 3/3 testes (100% da suíte instrumentada de `:feature:auth`) | não corrigido | Contorno **não** aceitável como encerramento: a suíte instrumentada de `:feature:auth` está inerte — nenhum teste dela roda em API 37. **Correção pendente**: subir `espresso` para uma versão compatível com API 37 no catálogo de versões (ou remover `espresso-core` da dependência, se o teste passar a usar apenas `compose.ui.test.junit4`, que já está declarado na linha 114). Reexecutar em emulador API 37 e anexar o resultado verde ao registro. Severidade `Crit.` porque deixa sem verificação o login — um dos fluxos cobertos pelo item 3 do N2. |
| DEF-14 | QA manual (métrica de cobertura) | O gate do Kover exclui do denominador os pacotes `*.remote.*` e `*.di.*` (`build.gradle.kts:80-84`). Em `:core:data` isso remove **806 das 3 944 linhas** de código de produção versionadas em `main`: `remote/` (6 arquivos, 593 linhas) e `di/DataModule.kt` (213 linhas) — 20,4% do total. `RemoteDataSource.kt` (253 linhas, **26 blocos `catch`**) e `HolidayRemoteDataSource` ficam fora da métrica, embora sejam exercitados por teste. Isso infla a percentual reportado (`:core:data` 84,1% linha / 73% branch / 80,7% classe) ao excluir justamente a camada com mais lógica de erro. Nenhum módulo `feature/*`, `:app` ou `:core:ui` tem gate de cobertura. | Men. | 100% (todo relatório do gate) | não corrigido | **Fora do escopo desta sessão** (correção **não** aplicada — a exclusão é intencional para classes geradas por Room/Hilt, conforme `AGENTS.md`; estreitar só o padrão `*.remote.*` é decisão de escopo do gate). A exclusão de `*.remote.*` cobre DTOs de mapeamento puro **e** simultaneamente `RemoteDataSource`/`HolidayRemoteDataSource`, que são lógica de produção. Correção proposta e **pendente**: separar os dois casos — manter fora apenas os DTOs (`*.remote.*Dto`, `*.remote.BrainOutApi`) e recolocar `RemoteDataSource`, `HolidayRemoteDataSource` e `HolidayApi` no denominador, medindo o impacto no piso de 60% antes de adotar. Registrar a nova métrica em `docs/CI-CD.md` quando houver. |

> **Observação sobre o preenchimento atual.** A tabela acima
> contém apenas defeitos cuja origem é rastreável a um artefato
> versionado (CI run, QA manual, run-log, PR já mergeado). Não
> foram inventados defeitos a partir de cenários não executados:
> casos de teste ainda não rodados (`docs/ROTEIRO-TESTES.md`,
> coluna *Resultado observado* em branco) e sessões de
> usabilidade ainda não realizadas (semana de 17/11/2026,
> `docs/USABILIDADE.md`) **não geram defeitos preventivos** —
> defeitos só entram após detecção real, conforme a Seção 3.
>
> As linhas reservadas `DEF-15` em diante ficam em branco até que
> novas detecções sejam feitas (formulário `QA manual`, falha de
> CI, resultado preenchido em `TF-NN`, achado da sessão `Pn-Tm`).

### 5.1. Reverificação de 08/10/2026 — DEF-06 e DEF-07

A auditoria de 9 frentes de 08/10/2026 (execução real: 618 testes
unitários verdes, build release com R8, emulador API 37) conferiu
dois registros desta tabela que ainda diziam `não corrigido` apesar
de a correção já estar em `main`. O estado foi corrigido; o
histórico de como cada um foi aberto permanece acima, na própria
linha.

**DEF-06 — i18n das notificações de prazo.** As 8 chaves existem
nos dois locales de `:app`, linhas 74-81 de cada arquivo:

```bash
# diff dos conjuntos de chaves PT vs EN nos cinco módulos com strings
for m in app feature/auth feature/projects feature/settings feature/tasks; do
  grep -o 'name="[^"]*"' $m/src/main/res/values/strings.xml    | sort > /tmp/pt
  grep -o 'name="[^"]*"' $m/src/main/res/values-en/strings.xml | sort > /tmp/en
  echo "$m: PT=$(wc -l </tmp/pt) EN=$(wc -l </tmp/en) faltando=$(comm -23 /tmp/pt /tmp/en | wc -l)"
done
```

Resultado: `app` 60/60, `feature/auth` 43/43, `feature/projects`
100/100, `feature/settings` 9/9, `feature/tasks` 34/34 —
**246 chaves, zero faltando em EN, zero sobrando**. As traduções
entraram em `d171d25` (PR #57), o mesmo bump de toolchain que a
justificativa original apontava como branch ainda não mergeado; o
`git log -S'deadline_channel_name' -- app/src/main/res/values-en/strings.xml`
aponta esse commit como origem. O bloqueio de toolchain citado na
justificativa não existe mais.

**DEF-07 — polimento do módulo de feriados.** Os quatro arquivos
do E3.5 estão versionados em `main`:

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