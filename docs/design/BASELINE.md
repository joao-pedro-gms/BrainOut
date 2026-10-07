# BrainOut — Baseline técnico e funcional (F0 / NB-01)

**Medido em 02/10/2026** sobre `main` = `78c6b8c670e971d72b54d7a15c1af09bbe448ae8`. Plano de referência: [redesign neobrutalista](../plans/2026-10-01-redesign-neobrutalista.md), tarefa NB-01 e seção 9 (comandos de verificação).

Razão de existir: fixar o ponto de partida **real** do redesign — o que compila, o que passa, o que já falhava antes de qualquer mudança — para que o "antes/depois" de F8 (NB-34) compare builds equivalentes e para que ninguém credite ao redesign um defeito que já existia.

> **Nota de escopo (2026-10-07).** Esta é uma medição datada (02/10/2026). Desde
> então o produto passou a ser **totalmente local** (AD-7): as dependências de
> rede (Retrofit/OkHttp) e a fila de sincronização saíram do escopo (BO-02).
> Os valores abaixo permanecem como registro do que foi medido naquela data.

## 1. Ambiente verificado

| Item | Valor real |
|---|---|
| Commit | `78c6b8c670e971d72b54d7a15c1af09bbe448ae8` (`main`, idêntico a `origin/main`) |
| Data da medição | 02/10/2026 |
| JDK | **OpenJDK 21.0.12.1** (Arch), `JAVA_HOME=/usr/lib/jvm/java-21-openjdk` |
| Gradle | 9.7.1 (wrapper), Kotlin embutido 2.4.0 |
| Variantes usadas | `devDebug` (build, testes, lint) |
| Android SDK | `/home/joaopgms/Android/Sdk` — platform `android-37.0`, `platform-tools` presente |
| `compileSdk` / `minSdk` / `targetSdk` | 37 / 24 / 37 (`app/build.gradle.kts`) |
| Dispositivos | **0 conectados** (`adb devices` vazio) |
| AVD disponível | `Medium_Phone_API_37.0` |

### 1.1 Desbloqueio de ambiente feito nesta tarefa

- A máquina **não tinha JVM alguma** por padrão (`java` ausente do `PATH`, `JAVA_HOME` vazio). Sem isso, nenhum gate local rodava: era a causa real dos primeiros despachos de subagente expirarem sem produzir artefato.
- O arquivo **não versionado** `gradle/gradle-daemon-jvm.properties` exigia daemon **Java 25** e derrubava todo build local com `Cannot find a Java installation … matching: {languageVersion=25…} Toolchain auto-provisioning is not enabled`. Ele foi renomeado para `gradle/gradle-daemon-jvm.properties.bak` (não versionado, não faz parte de nenhum commit). **Não recriar**: com JDK 21 o build funciona e a CI também usa 21.
- `detekt` 1.23.7 roda normalmente em JDK 21 — a limitação registrada em `AGENTS.md` vale para o JBR 25 do Android Studio, não para este ambiente.

## 2. Versões do catálogo e resolução real

Declaradas em `gradle/libs.versions.toml`: AGP 9.4.1, Kotlin 2.3.20, KSP 2.3.12, Hilt 2.60.1, Room 2.8.4, Compose BOM 2024.10.01, coroutines 1.9.0, JUnit 4.13.2, Robolectric 4.15.1, Turbine 1.2.0, MockK 1.13.13, Truth 1.4.4. À época desta medição o catálogo ainda declarava Retrofit 2.11.0 e OkHttp 4.12.0 — dependências de rede hoje **fora do escopo** (BO-02).

Resolução medida com `./gradlew :app:dependencies --configuration devDebugRuntimeClasspath` (**163 artefatos distintos**):

| Artefato | Resolvido |
|---|---|
| `androidx.compose.ui:ui`, `foundation`, `animation` | **1.7.5** (candidatos mais antigos aparecem no grafo: 1.7.2, 1.7.0, 1.6.0, 1.0.1) |
| `androidx.compose.material3:material3` | **1.3.1** |
| `androidx.room:room-runtime` | 2.8.4 |
| `androidx.lifecycle:lifecycle-viewmodel-compose` | 2.8.7 |
| `com.squareup.retrofit2:retrofit` | 2.11.0 — *fora do escopo (BO-02)* |
| `com.squareup.okhttp3:okhttp` | 3.14.9 → **4.12.0** (upgrade no grafo) — *fora do escopo (BO-02)* |
| `org.jetbrains.kotlin:kotlin-stdlib` | 2.3.20 → **2.3.21** |

Isso confirma no projeto real o que o POM da BOM declara (UI 1.7.5 / M3 1.3.1) — base sobre a qual NB-07 (sombra) e NB-22 (DnD) devem reavaliar suas premissas de API.

## 3. Gate da seção 9 — resultado real

| Comando | Resultado | Observação |
|---|---|---|
| `./gradlew ktlintCheck` | ✅ BUILD SUCCESSFUL (9 s) | |
| `./gradlew detekt` | ✅ BUILD SUCCESSFUL (7 s) | roda em JDK 21 |
| `./gradlew :core:domain:test` | ✅ | com cache |
| `./gradlew testDevDebugUnitTest` | ✅ BUILD SUCCESSFUL (44 s) | cobre `:app` e `:core:data` + `:core:domain` pelo wire-up |
| `./gradlew :core:domain:koverVerify :core:data:koverVerify` | ✅ (24 s) | limite de 60 % atendido |
| `./gradlew :feature:tasks:testDebugUnitTest` | ✅ | |
| `./gradlew :feature:auth:testDebugUnitTest` | ✅ | |
| `./gradlew :feature:projects:testDebugUnitTest` | ❌ **4 falhas / 34 testes** | preexistente — detalhe em §4 |
| `./gradlew :feature:settings:testDebugUnitTest` | ❌ não compilava → ✅ **3 testes, 0 falhas** após correção | preexistente — detalhe em §4 |
| `./gradlew :app:lintDevDebug` | ✅ BUILD SUCCESSFUL (44 s) | i18n sem `MissingTranslation` |
| `./gradlew :app:assembleDevDebug` | ✅ BUILD SUCCESSFUL (3 s) | |

## 4. Falhas e defeitos preexistentes (antes de qualquer mudança do redesign)

### 4.1 `:feature:projects` — 4 testes falhando

34 testes executam, 4 falham, todos em `HomeViewModelTest`:

| Teste | Falha real |
|---|---|
| `E2 6 ordenacao persiste entre instancias do ViewModel` | `expected: CreatedAsc \| but was: CreatedDesc` |
| `E2 6 debounce nao bloqueia emissao inicial` | `expected to be false` |
| `E2 8 retry re-assina o Flow apos erro e limpa errorMessage` | `expected: null \| but was: Não foi possível carregar seus projetos` |
| `E2 8 uiState recebe primeira emissao do Flow com isLoading false` | `expected to be false` |

Os testes foram escritos até `9dd8696`; `HomeViewModel` foi refatorado depois em `f846a02` ("decompose HomeViewModel (595→197) + extrai ações/pipeline"). Não se decide aqui qual lado está errado — isso é julgamento de comportamento e pertence a uma tarefa própria. **O redesign não os causa e não os corrige.**

### 4.2 `:feature:settings` — suíte que nunca compilou

`SettingsScreenTest.kt` usava `createComposeRule`, `@Config`, `AndroidJUnit4` e `performScrollTo` sem que o módulo declarasse `ui-test-junit4`, `robolectric`, `androidx.test.core` e `ui-test-manifest`, nem `testOptions.unitTests.isIncludeAndroidResources`. Resultado: `compileDebugUnitTestKotlin FAILED` com `Unresolved reference 'test'` / `'Config'` / `'createComposeRule'`.

Corrigido nesta entrega (PR próprio): dependências e `testOptions` espelhando `:feature:tasks`, mais três ajustes de asserção no teste (rolar até o nó dentro da coluna `verticalScroll`; o título fica na `TopAppBar` e não é rolável; títulos de seção aparecem em caixa alta). Suíte agora: **3 testes, 0 falhas**.

### 4.3 A CI não roda teste de unidade por feature

`ci.yml` executa `./gradlew testDevDebugUnitTest` (que alcança apenas `:app`, `:core:data` e, pelo wire-up do `projectsEvaluated`, `:core:domain`) e, num job separado, `:core:data:testDevDebugUnitTest :app:testDevDebugUnitTest`. **Nenhum comando cita `:feature:*`** — nem em `ci.yml`, nem em qualquer outro workflow. Foi por isso que uma suíte que não compila e outra com 4 falhas passaram meses de CI verde.

A seção 9 do plano afirmava o contrário ("a CI lista os `testDebugUnitTest` por feature explicitamente"); a afirmação foi corrigida no próprio plano nesta entrega, e a dívida ficou registrada no backlog (§1.3) — ligar os testes de feature na CI **antes** exige estabilizar os 4 de `:feature:projects`, senão a CI fica vermelha para todos.

## 5. Roteiro de desempenho

O procedimento reproduzível está em [PERFORMANCE.md](PERFORMANCE.md). **Nenhuma medição foi feita**: não há dispositivo conectado e o roteiro exige build release assinado instalado em hardware. O baseline numérico de F0 fica, portanto, **pendente** — e NB-34 (desempenho comparado) depende de ele existir para poder comparar.

## 6. Bloqueios e próximos passos

| Bloqueio | Impacto | Como destravar |
|---|---|---|
| Nenhum dispositivo/emulador conectado | QA físico (NB-33) e medição de desempenho (NB-01/NB-34) impossíveis | `emulator -avd Medium_Phone_API_37.0` e `adb devices` |
| 4 testes de `:feature:projects` falhando | impede ligar testes de feature na CI | tarefa própria (backlog §1.3); não é do redesign |
| Keystore local ausente | build de medição precisa de release assinado | gerar com `keytool` fora do repo (`BRAINOUT_KEYSTORE_*`), nunca versionar |
| `gradle/gradle-daemon-jvm.properties` exige Java 25 | trava build local inteiro | mantido como `.bak`; não recriar |

Primeiro passo é desbloquear o dispositivo: sem ele, o próprio critério de aceite do plano (duas taxas de atualização, press/sombras/IME) não pode ser verificado.
