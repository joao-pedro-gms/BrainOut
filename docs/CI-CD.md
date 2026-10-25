# CI/CD — BrainOut

## Operação atual após #212

O produto e os builds são locais, sem flavors. A configuração executável é
`.github/workflows/ci.yml`, com três jobs:

1. `static-analysis`: regressão do workflow de release, ktlint e detekt.
2. `unit-tests`: `testDebugUnitTest` (inclui domínio), suítes explícitas das
   quatro features e core:ui, Kover >=60%, relatórios e `lintDebug`.
3. `local-integration`: APK debug e compilação das suítes instrumentadas de
   core:data e auth. Não executa emulador; os testes JVM de upgrade e
   transação real rodam no job unit-tests. O manifesto final é verificado sem
   permissões de rede. O APK de testes precisa conter AndroidJUnitRunner e
   todos os schemas históricos, evitando o falso verde de suíte vazia.

O job backend-integration, os containers e os testes HTTP foram removidos
junto dos consumidores. Release debug publica `app/build/outputs/apk/debug`;
AAB usa `bundleRelease`, sem keystore local versionado. O APK não contém
permissões INTERNET/ACCESS_NETWORK_STATE; WorkManager permanece para
lembretes locais e o upgrade cancela somente os trabalhos antigos de sync.

Comandos atuais: `./gradlew ktlintCheck detekt testDebugUnitTest lintDebug`
e `./gradlew :core:domain:koverVerify :core:data:koverVerify` em JDK21.
As instruções de secrets/assinatura abaixo continuam aplicáveis; a seção
[Registro histórico anterior à migração local](#registro-histórico-anterior-à-migração-local)
guarda o pipeline com `backend-integration` e os flavors `dev`/`prod`, que
sairam com a camada de rede (#212) — e o issue #208, que apontava essa
reconciliação, está **fechado**. As seções
[Escopo do gate de ktlint](#escopo-do-gate-de-ktlint) e
[Operação local equivalente](#operação-local-equivalente) são as que valem
para o pipeline de hoje.

> Documento de operação dos pipelines de GitHub Actions. Atualize quando
> secrets ou workflows mudarem. Item desejável 5.1 do documento norteador
> (não obrigatório, mas fortalece a rubrica de qualidade técnica).

## Visão geral

| Workflow             | Gatilho                            | O que faz                                                       |
|----------------------|------------------------------------|-----------------------------------------------------------------|
| `ci.yml`             | Push em `main`, PR contra `main`, `workflow_dispatch` | Análise estática (ktlint + detekt), testes unitários, build local e compilação das suítes instrumentadas |
| `release-apk.yml`    | Tag `v*`                           | Gera `.aab` assinado (`bundleRelease`); publica como artifact do run             |
| `release-apk-debug.yml` | Tag `v*`, `workflow_dispatch`   | Gera APK debug (`assembleDebug`) e anexa `BrainOut-<tag>-debug.apk` à GitHub Release da tag |
| `codeql.yml`         | Push em `main`, PR, semanalmente   | Análise estática de segurança (CodeQL)                          |
| `pages.yml`          | Push em `main` que altere `docs/wireframes/**`, `workflow_dispatch` | Publica os wireframes em GitHub Pages (`actions/deploy-pages`)  |

Configuração adicional:

- `.github/dependabot.yml` — atualizações semanais de GitHub Actions e
  Gradle.
- `.github/CODEOWNERS` — todo PR precisa de aprovação do
  `@joao-pedro-gms` (ajuste se necessário para revisão adicional).

## Registro histórico anterior à migração local

A partir daqui, o registro do pipeline **com a camada de rede**: três jobs
com `backend-integration`, flavors `dev`/`prod` e as tarefas `*DevDebug*`.
Nada abaixo é executável hoje — a descrição do pipeline vigente está no
[topo deste documento](#operação-atual-após-212) e na seção
[Operação local equivalente](#operação-local-equivalente).

## Workflows

### `ci.yml`

Três jobs; `unit-tests` e `backend-integration` dependem de
`static-analysis` (e rodam em paralelo entre si). Os três provisionam o
mesmo JDK do release: `actions/setup-java` fixado por SHA (a tag fica
como comentário), `distribution: temurin`, `java-version: '21'`.

1. **`static-analysis`** — ktlint + detekt. Falha rápido.
   O `ktlintCheck` roda com o plugin aplicado aos **9 subprojetos**
   (`subprojects { }` no `build.gradle.kts` raiz, `android = true`) e
   com `ignoreFailures = true` — ver [Escopo do gate de
   ktlint](#escopo-do-gate-de-ktlint). O `detekt` é o gate que reprova:
   roda nos 8 módulos com `autoCorrect = false`.
2. **`unit-tests`** — roda testes unitários (`testDevDebugUnitTest`) e,
   em passo explícito aditivo (FIX-02, issue #90), as quatro suítes de
   feature
   (`:feature:projects|tasks|auth|settings:testDebugUnitTest` — os
   módulos `:feature:*` não têm flavor próprio e ficam fora do comando
   agregador) e, aditivamente (NB-05, issue #92),
   `:core:ui:testDebugUnitTest` (mesmo motivo: `:core:ui` também não tem
   flavor; é onde moram os contratos de token, tipografia e asset).
   Depois: `koverVerify`, o relatório HTML do Kover e o
   Android Lint
   (`lintDevDebug lintDebug` — a primeira tarefa alcança `:app` e
   `:core:data`; a segunda inclui as features e `:core:ui`, sem flavor).
   Assim, os gates `MissingTranslation` declarados nas features também
   executam na CI (DEF-40 / #178). Publica relatório como artifact.

   Os dois passos explícitos existem porque `testDevDebugUnitTest` só
   alcança `:app`, `:core:data` e (pelo wire-up do `projectsEvaluated`)
   `:core:domain`. Sem eles, uma suíte de `:feature:*` ou `:core:ui`
   que não compilasse passaria verde em CI. Naquele momento:
   **618 testes
   unitários, 84 classes, 0 falhas, 0 erros** (`:core:data` 302,
   `:core:domain` 133, `:core:ui` 88, `:feature:projects` 35, `:app` 24,
   `:feature:tasks` 18, `:feature:auth` 15, `:feature:settings` 3).
3. **`backend-integration`** — constrói a imagem de `backend-stub/`,
   sobe o container e aguarda o `/health` (loop de 30 tentativas × 2 s),
   roda o smoke de endpoints (`curl`: PUT/DELETE/tags), a suíte pytest do
   stub (**16 casos** em `tests/test_contract.py`) e o smoke E2E
   (`backend-stub/tests/smoke_e2e.py`) contra o
   container. Por fim executa
   `./gradlew :core:data:testDevDebugUnitTest :app:testDevDebugUnitTest`
   com `BASE_URL=http://localhost:8000`. A instrumentação Android
   (`connectedDevDebugAndroidTest`) **não roda no CI** — exige emulador e foi
   substituída pela suíte Robolectric + MockWebServer (decisão P0-5/R6).

Cache de Gradle configurado em todos os jobs (`actions/cache` fixado por SHA).

Todas as actions dos workflows são fixadas por SHA, e o wrapper do
Gradle declara o `distributionSha256Sum` da distribuição — o download é
verificado pelo próprio wrapper (DEF-68).

### Testes instrumentados: compilam, mas não são gate

A suíte instrumentada (`:core:data` → `MigrationTest`,
`UserDaoInstrumentedTest`; `:feature:auth` → `LoginScreenTest`) **compila**
e pode ser executada localmente:

```bash
# requer emulador/dispositivo (adb devices)
./gradlew connectedDevDebugAndroidTest
```

Ela voltou a compilar porque `core/data/build.gradle.kts` passou a
declarar `androidTestImplementation(libs.truth)` e
`androidTestImplementation(libs.kotlinx.coroutines.test)` — `MigrationTest`
e `UserDaoInstrumentedTest` importam Truth e coroutines-test, e sem essas
duas linhas o `compileDevDebugAndroidTestKotlin` falhava com ~40 erros
`Unresolved reference`. A suíte cobre as três migrações do
`BrainOutDatabase` (v4): **1→2, 2→3 e 3→4**, além do encadeado 1→2→3→4,
validando cada passo contra o schema exportado em `core/data/schemas/` e
conferindo que os dados semeados sobrevivem.

**O que isso não é: um gate do CI.** Não existe runner com emulador no
plano gratuito do GitHub Actions, então nenhuma tarefa
`connected*AndroidTest` é invocada em nenhum dos três jobs. O job
`backend-integration` cobre o contrato HTTP pela suíte JVM. A
consequência deve ser dita sem enfeite: **as migrações Room só são
verificadas por quem roda emulador, manualmente.** Se a cobertura de
migração virar requisito de entrega, é preciso um runner self-hosted com
emulador ou uma política explícita de verificação manual.

*(Fim do registro do pipeline com a camada de rede. O que vem abaixo
descreve o build e o gate de estilo **atuais**.)*

## Escopo do gate de ktlint

O plugin ktlint **não** se propaga automaticamente para subprojetos.
Aplicado apenas na raiz, `./gradlew ktlintCheck` inspecionava somente os
2 arquivos de build da raiz e **nenhum** dos 221 arquivos `.kt`
versionados — confirmado em disco: só existia
`build/reports/ktlint/` na raiz. Hoje o `build.gradle.kts` raiz aplica o
plugin a cada subprojeto:

```kotlin
subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    extensions.configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        android.set(true)
        ignoreFailures.set(false)
    }
}
```

`android = true` faz o analisador entender código Android/Compose.
O `ignoreFailures` é **`false`** — o gate **bloqueia**. A engine 1.x do
ktlint 14.2.0 sinalizava violações cosméticas de estilo
(`standard:indent`, `multiline-expression-wrapping`, `function-signature`,
`trailing-comma-on-call-site`, entre outras) que o código carregava há
129 commits; o `ktlintFormat` nos 8 módulos (commit `b59318e`) mais o
acerto das 9 violações não auto-corrigíveis (commit `b6db6e0`) zeraram o
contador (DEF-10 fechado). Violação nova **quebra o PR** — que é o
comportamento pretendido para um gate de estilo.

O passo **Run detekt** é o que reprova de fato no CI — não o ktlint,
enquanto a flag estiver em `true`.

### `release-apk.yml` — Release assinado (E3.7)

- **Gatilho:** exclusivamente o push de tags `v*` (ex.: `v1.0.0`).
- **Build:** `./gradlew :app:bundleRelease` com JDK 21, assinado via
  `signingConfig` do `:app/build.gradle.kts` que lê as variáveis de
  ambiente `BRAINOUT_KEYSTORE_PATH`, `BRAINOUT_KEYSTORE_PASSWORD`,
  `BRAINOUT_KEY_ALIAS` e `BRAINOUT_KEY_PASSWORD`.
- **Sem secrets:** o workflow falha com mensagem clara pedindo o cadastro
  dos secrets (o build comum em PR nunca quebra, pois o `build.gradle.kts`
  cai em build não-assinado quando as variáveis estão ausentes).
- **Artifact:** `brainout-release-aab-<tag>`, contendo
  `app/build/outputs/bundle/`. O caminho do `.aab` muda com os flavors
  (`bundle/<flavor>Release/`), por isso o workflow o localiza com
  `find app/build/outputs/bundle -maxdepth 3 -name '*.aab'` e valida a
  assinatura com `jarsigner -verify` antes do upload. Retenção de 30
  dias. Não há upload automático para loja externa.

### `release-apk-debug.yml` — APK debug na GitHub Release

Companheiro do workflow acima. **Não substitui** o `.aab` assinado e
**não lê** os secrets `BRAINOUT_*`.

- **Gatilho:** push de tags `v*` e `workflow_dispatch` (input `tag`,
  também no formato `v*`).
- **Validação (DEF-41 / #179):** antes do checkout e do build, o primeiro
  passo recebe o input em `env`, sem interpolá-lo como código shell.
  Aceita `v` seguido de letras, números, ponto, sublinhado, `+` ou `-`;
  rejeita espaços, quebras de linha e metacaracteres de shell. Em push,
  usa `GITHUB_REF_NAME`. Só exporta `RELEASE_TAG` após validar o valor inteiro.
  A CI executa `python3 -m unittest discover -s .github/tests -v`, que
  cobre tags válidas, inputs inválidos e tentativas de injeção com o Bash
  real do workflow. Os testes não disparam releases nem usam credenciais.
- **Build:** JDK 21 (Temurin) e `./gradlew :app:assembleDebug`. O Gradle
  9.x do wrapper exige JVM 21; o bytecode do app continua Java 17.
- **Asset:** o flavor `dev` (o `prod` aponta para `https://TBD/`). O
  arquivo publicado se chama `BrainOut-<tag>-debug.apk` e é anexado à
  GitHub Release dessa tag (`contents: write`). O `release-apk.yml`
  segue publicando só o artifact do `.aab`.

### `codeql.yml`

- Matriz de linguagens `java` + `kotlin` (`fail-fast: false`).
- Além de push/PR, roda semanalmente (cron `17 6 * * 0`) para pegar
  regressões tardias.

## Cobertura de testes (E4.7)

A cobertura de código é medida pelo **Kover** (plugin
`org.jetbrains.kotlinx.kover`, versão em `gradle/libs.versions.toml`)
nos módulos **`:core:domain`** e **`:core:data`**, com um bound mínimo
de **60% de cobertura de linhas** em cada camada.

Medição atual, bem acima do bound:

| Módulo       | Linhas         | Branches | Classes |
|--------------|----------------|----------|---------|
| `:core:domain` | 84,9% (320/377) | 77,4% | 73,9% |
| `:core:data`   | 84,1% (596/709) | 73%   | 80,7% |

### Rodar localmente

```bash
export JAVA_HOME=/opt/android-studio/jbr   # JBR do Android Studio (JDK 21+)

# Verificar o bound de 60% (falha se qualquer camada ficar abaixo)
./gradlew :core:domain:koverVerify :core:data:koverVerify

# Gerar os relatórios HTML
./gradlew :core:domain:koverHtmlReport :core:data:koverHtmlReport

# Imprimir a cobertura no console
./gradlew :core:domain:koverLog :core:data:koverLog
```

Os relatórios são gerados em:

- `core/domain/build/reports/kover/html/index.html`
- `core/data/build/reports/kover/html/index.html`

### Como interpretar o relatório

O `index.html` da raiz de cada módulo resume a cobertura por pacote
(`model`, `usecase`, `repository` no domínio; `local`, `remote`,
`repository`, `security`, `session` em data). Perfure até a classe para
ver linha a linha o que foi exercitado: verde = executado, amarelo =
parcial (ramo), vermelho = não executado. Use as porcentagens de
**branch** para priorizar testes de regras condicionais (ex.: transições
de status de tarefa, matriz de permissões).

### Artifact no CI

O job `unit-tests` do `ci.yml` roda `koverVerify` (falha o job se a
cobertura < 60%) e publica o relatório HTML como artifact
**`coverage-report`** (retenção de 14 dias). Baixe em **Actions → run →
Artifacts → coverage-report**; o conteúdo tem um diretório por módulo.

### Filtros documentados

Classes geradas e infra de wiring ficam fora do denominador da
cobertura (configurado no bloco Kover do `build.gradle.kts` raiz):

- `*_Impl`, `*_Impl$*` — implementações geradas pelo Room;
- `*_Factory`, `*_HiltModules`, `*.Hilt_*`, `dagger.hilt.*`,
  `hilt_aggregated_deps.*` — artefatos do Hilt/Dagger;
- `*.di.*` — módulos de DI (wiring, não lógica);
- `*.BuildConfig`, `*.PackageMarker` — classes utilitárias sem lógica;
- `*.remote.*` — DTOs de rede (mapeamento puro, exercitado via
  repositories).

## Secrets necessários (E3.7)

Configure em **Settings → Secrets and variables → Actions → New repository
secret**. São exatamente estes 4 nomes:

| Secret                       | Usado em          | Conteúdo                                                            |
|------------------------------|-------------------|---------------------------------------------------------------------|
| `BRAINOUT_KEYSTORE_BASE64`   | `release-apk.yml` | Conteúdo do `brainout-release.jks` codificado em base64 (uma linha) |
| `BRAINOUT_KEYSTORE_PASSWORD` | `release-apk.yml` | Senha do keystore                                                   |
| `BRAINOUT_KEY_ALIAS`         | `release-apk.yml` | Alias da chave dentro do keystore                                   |
| `BRAINOUT_KEY_PASSWORD`      | `release-apk.yml` | Senha da chave (pode ser igual à do keystore)                       |

> Os secrets devem ser cadastrados pelo dono do repositório. Enquanto não
> existirem, o push de uma tag `v*` falha no step **Validate signing
> secrets (P0-6)** — nenhum `.aab` assinado é produzido antes disso.

### Como gerar o keystore localmente

```bash
keytool -genkeypair -v \
  -keystore brainout-release.jks \
  -alias brainout \
  -keyalg RSA -keysize 2048 \
  -validity 10000
```

### Como gerar o base64 para o secret

```bash
base64 -w 0 brainout-release.jks > brainout-release.jks.b64
# copie o conteúdo de brainout-release.jks.b64 para BRAINOUT_KEYSTORE_BASE64
```

Guarde o arquivo físico em cofre de senha (Proton Pass). Se ele se
perder, as builds existentes continuam instaláveis, mas não é possível
publicar atualizações sobre a mesma assinatura.

### Procedimento de corte de release

1. Atualize `versionName` e `versionCode` em `app/build.gradle.kts`
   (ex.: `versionName = "1.0.0"`, `versionCode = 2`).
2. Confirme que os 4 secrets estão cadastrados no repositório.
3. Crie e envie a tag:

```bash
git tag -a vX.Y.Z -m "Release vX.Y.Z"
git push origin vX.Y.Z
```

4. Acompanhe o run em **Actions → Release AAB assinado**; ao final, baixe
   o artifact `brainout-release-aab-<tag>`.

### Como conferir que o `.aab` está assinado

```bash
# O nome/caminho do .aab varia com o flavor (ex.:
# app/build/outputs/bundle/<flavor>Release/) — localize com o mesmo find
# usado pelo workflow.
jarsigner -verify -verbose -certs <caminho-do-.aab>
```

`apksigner` **não** serve aqui: ele valida APKs (exige
`AndroidManifest.xml` e o esquema APK Signature Scheme) e falha com
`ApkFormatException: Missing AndroidManifest.xml` em um `.aab`. A
assinatura do bundle é a de JAR, verificada por `jarsigner`.

## Toolchain

Versões centralizadas em `gradle/libs.versions.toml`. A política é bump
explícito no catálogo e cobertura por `./gradlew :app:assembleDebug
testDebugUnitTest ktlintCheck detekt lintDebug` antes de subir PR.

| Componente                 | Versão          | Notas                                                       |
|----------------------------|-----------------|-------------------------------------------------------------|
| JDK (CI + local)           | 21 (Temurin)    | `ci.yml` e `release-apk.yml` provisionam Temurin 21 (unificado). Piso oficial do Gradle 9.8.1/AGP 9.4.1 é 17, mas todo o fluxo roda em 21. O bytecode alvo continua **Java 17**; sem JDK separado, o JBR do Android Studio serve (`export JAVA_HOME=/opt/android-studio/jbr`). |
| Gradle (wrapper)           | 9.8.1           | Exigido pelo AGP 9.4 (`>= 9.6.0`). Não bumpar no toolchain. |
| AGP                        | 9.4.1           | Bump de 8.7.3; habilita built-in Kotlin (ver abaixo).       |
| Kotlin Gradle Plugin (KGP) | 2.4.21          | Exigido >= 2.2.10 pelo AGP 9.                             |
| KSP                        | 2.3.12          | Acima do piso 2.3.5 (corrige ciclo kapt/ksp do AGP 9).      |
| Hilt                       | 2.60.1          | Mínimo 2.59 declarado pelo time do Hilt para AGP 9.         |
| Room                       | 2.8.5           | Suporta KSP 2.3.x.                                          |
| desugar_jdk_libs           | 2.1.5           | Habilita `coreLibraryDesugaring` no `:app`, no `:core:data` e nas features para `java.time`/`java.util.Base64` em minSdk 24. |
| ktlint (plugin)            | 14.2.0          | Bump de 12.1.2; engine ktlint 1.x é estritamente mais rígida (chained-call, indentation). Aplicado aos 8 módulos com `ignoreFailures = false` — o gate **bloqueia** — ver [Escopo do gate de ktlint](#escopo-do-gate-de-ktlint). |
| detekt                     | 1.23.8          | **Não roda localmente com o JBR 25** (rejeita `--jvm-target` derivado do JVM do Gradle); roda no CI em Temurin 21. |
| Kover                      | 0.9.11          | Bound de 60% em `:core:domain` e `:core:data`.              |
| Compose BOM                | 2026.09.00      | Gerencia material3 e o grupo `material3.adaptive:*`.       |
| Robolectric                | 4.17            | Sem suporte a Baklava/SDK 36 ainda.                        |
| compileSdk / targetSdk     | 37              | Bump de 35: as dependências do PR #77 exigem compilar contra API 37 (`checkDebugAarMetadata` falhava em `:app`, `:core:data` e nos `feature/*`). `minSdk` segue 24. |

### Built-in Kotlin (AGP 9)

AGP 9 ativa `built-in Kotlin` automaticamente para todos os módulos
Android. Com isso:

- O plugin `org.jetbrains.kotlin.android` foi **removido** dos
  `build.gradle.kts` raiz e dos módulos (`:app`, `:core:data`,
  `:core:ui`, `feature/*`). Apenas `org.jetbrains.kotlin.jvm` permanece
  aplicado em `:core:domain` (módulo puro JVM).
- O bloco `kotlinOptions { jvmTarget = "17" }` (DSL legada) foi
  migrado para `kotlin { compilerOptions { jvmTarget.set(...) } }` em
  todos os módulos Android.
- `kotlin-compose` e `kotlin-serialization` continuam aplicados
  manualmente (são plugins de feature do Kotlin, não de plataforma).
- Opt-out temporário via `android.builtInKotlin=false` permanece
  disponível para debugging (é uma propriedade lida pelo AGP, **não**
  está declarada no `gradle.properties`); será removido no AGP 10.

### Deprecations resolvidas neste bump

- **`android.nonFinalResIds=false`** removido do `gradle.properties` —
  deprecated no AGP 9, default agora é `true`.
- **Migrate to built-in Kotlin** (developer.android.com/build/migrate-to-built-in-kotlin)
  — ver tabela acima.
- **`android.kotlinOptions` DSL** removida em todos os módulos Android.

### Deprecations pré-existentes (fora do escopo deste bump)

- `compileSdk` elevado de 35 para 37 (o lint já sugeria 37).
- 8 erros `MissingTranslation` para `deadline_*` foram corrigidos como
  parte deste bump (PR #49 do E4.6 só traduziu as 52 chaves
  pré-existentes; as 8 do E3.6 entraram depois e ficaram sem par `en`).
- 6 erros pré-existentes em E3.6 (`MissingPermission`, `NewApi` em
  `NotificationChannel`/`Instant.toEpochMilli`) corrigidos: guarda
  `Build.VERSION.SDK_INT >= O` em `ensureChannel`, `@SuppressLint`
  no `notify`, e `coreLibraryDesugaring` no `:app`.

## Branch protection recomendada

Em **Settings → Branches → Branch protection rules → main**, ative:

- ✅ Require a pull request before merging
- ✅ Require approvals: **1** (ou 2 se necessário para revisão adicional)
- ✅ Dismiss stale pull request approvals when new commits are pushed
- ✅ Require status checks to pass before merging
  - Selecione os **três** checks do `ci.yml`: `Static analysis (ktlint +
    detekt)`, `Unit tests (JUnit + Compose)` e `Local build and
    instrumentation compilation`
- ✅ Require conversation resolution before merging
- ❌ **Não** exigir "linear history": o repositório adota **merge commit**
  (as PRs são mescladas com `gh pr merge --merge`, e o histórico de
  `main` é de merges, não de squash nem de rebase)
- ✅ Include administrators

## Operação local equivalente

```bash
# Requisitos locais: o Android SDK declarado em `local.properties`
# (`sdk.dir=<Android SDK>`, arquivo gitignored) e `JAVA_HOME` apontando
# para um JDK 21+ — sem JDK separado, o JBR do Android Studio serve
# (`export JAVA_HOME=/opt/android-studio/jbr`). O bytecode alvo continua
# Java 17.

# Validar formato antes de subir PR.
# NOTA: o `detekt` NÃO roda com o JBR 25 do Android Studio (rejeita o
# --jvm-target derivado do JVM do Gradle). Com JBR, valide só:
./gradlew ktlintCheck

# Rodar unit tests (mesma tarefa do CI).
# NÃO inclui :feature/* nem :core:ui — acrescente:
./gradlew testDebugUnitTest \
          :feature:projects:testDebugUnitTest :feature:tasks:testDebugUnitTest \
          :feature:auth:testDebugUnitTest :feature:settings:testDebugUnitTest \
          :core:ui:testDebugUnitTest

# Testes INSTRUMENTADOS (exigem emulador; o CI NÃO os roda)
./gradlew connectedDebugAndroidTest

# Rodar lint Android
./gradlew lintDebug

# Verificar cobertura mínima de 60% (E4.7)
./gradlew :core:domain:koverVerify :core:data:koverVerify

# Compilar as suítes instrumentadas sem emulador — o que o job
# `local-integration` da CI faz
./gradlew :app:assembleDebug :core:data:assembleDebugAndroidTest \
          :feature:auth:assembleDebugAndroidTest
```

## Resolução de problemas

| Sintoma                                       | Causa provável                                  | Ação                                                       |
|-----------------------------------------------|-------------------------------------------------|------------------------------------------------------------|
| `detekt` falha **localmente** com o JBR do Android Studio | detekt 1.23.8 deriva `--jvm-target` do JVM do Gradle e rejeita `25` | Não é um gate local. Aponte o daemon para um **JDK 21** (o mesmo que a CI provisiona) e rode `ktlintCheck`, `detekt`, `testDebugUnitTest` e `koverVerify`; com o JBR, valide só `ktlintCheck` e `testDebugUnitTest`. Não há workaround por módulo |
| `ktlintCheck` acusa violações de estilo | Engine 1.x do ktlint 14, mais estrita que a 12.x | O gate é **bloqueante** (`ignoreFailures = false` no `build.gradle.kts` raiz — DEF-10 corrigido): cada violação reprova o build. `ktlintFormat` corrige o que é automático; o resto é manual |
| `compileDebugAndroidTestKotlin` falha com `Unresolved reference` | `MigrationTest`/`UserDaoInstrumentedTest` importam libs que estavam só em `testImplementation` | `core/data/build.gradle.kts` declara `androidTestImplementation(libs.truth)` e `androidTestImplementation(libs.kotlinx.coroutines.test)`; não remova essas linhas |
| `connectedDebugAndroidTest` falha local     | Nenhum dispositivo/emulador visível              | `adb devices` vazio = suba o AVD (`Medium_Phone_API_37.0` nesta máquina). **O CI não executa esta tarefa** — não há runner com emulador no plano gratuito |
| `detekt` falha no CI após um PR                | Nova regra ou código não compatível             | Corrigir o código manualmente: a correção automática está desativada no projeto (`autoCorrect = false` nos 8 módulos e nenhuma regra com `autoCorrect: true` no `detekt.yml`) |
| `release-apk.yml` falha em `Validate signing secrets (P0-6)` | Secrets `BRAINOUT_*` ausentes | Cadastrar os 4 secrets (ver seção E3.7) |
| `release-apk.yml` falha em `Decode keystore` | `BRAINOUT_KEYSTORE_BASE64` corrompido/incompleto (base64 inválido) | Recodificar: `base64 -w 0 brainout-release.jks` |
| `validateSigningRelease` falha: `Keystore file '.../app/keystore.jks' not found` | Caminho do keystore relativo à raiz, mas `file()` no módulo `:app` resolve contra `app/` | Corrigido no PR #43: o keystore é decodificado em `$RUNNER_TEMP` (path absoluto). Se reaparecer, conferir que `BRAINOUT_KEYSTORE_PATH` é absoluto |
| `signReleaseBundle` falha: `Get Key failed: Given final block not properly padded` | `BRAINOUT_KEY_PASSWORD` difere da senha da chave do keystore (PKCS12 usa a senha do keystore para a chave) | No `keytool -genkeypair` com `-storetype PKCS12` (padrão do JDK 21), usar a MESMA senha em `-storepass` e `-keypass` e cadastrar as duas secrets com esse valor. Desde o PR #266 o workflow falha **antes** do build, no step **Preflight do keystore (DEF-02)**, com a causa explícita |
| Gradle build falha com `OutOfMemoryError`     | Runner sem heap suficiente                      | `GRADLE_OPTS` no `ci.yml` já é 4g; revisar plugins pesados  |
| Workflow não dispara em PR                    | Branch protection bloqueou o push               | Verificar **Settings → Actions → General → Allow actions** |
| `Cannot find a Java installation matching languageVersion=17` (local) | Nenhum JDK 17 instalado (só o JBR/JDK 21+) | `export JAVA_HOME=/opt/android-studio/jbr` (JBR do Android Studio, ou outro JDK 21+) antes de `./gradlew`. `gradle.properties` já tem `auto-detect=true` e `auto-download=false`; o bytecode continua Java 17 (`sourceCompatibility = VERSION_17`) |
