# CI/CD — BrainOut

> Documento de operação dos pipelines de GitHub Actions. Atualize quando
> secrets ou workflows mudarem. Item desejável 5.1 do documento norteador
> (não obrigatório, mas fortalece a rubrica de qualidade técnica).

## Visão geral

| Workflow             | Gatilho                            | O que faz                                                       |
|----------------------|------------------------------------|-----------------------------------------------------------------|
| `ci.yml`             | Push em `main`, PR contra `main`, `workflow_dispatch` | Análise estática (ktlint + detekt), testes unitários, integração com backend stub |
| `release-apk.yml`    | Tag `v*`                           | Gera `.aab` assinado (`bundleRelease`); publica como artifact do run             |
| `codeql.yml`         | Push em `main`, PR, semanalmente   | Análise estática de segurança (CodeQL)                          |
| `pages.yml`          | Push em `main` que altere `docs/wireframes/**`, `workflow_dispatch` | Publica os wireframes em GitHub Pages (`actions/deploy-pages`)  |

Configuração adicional:

- `.github/dependabot.yml` — atualizações semanais de GitHub Actions,
  Gradle e (quando existir) `backend-stub/`.
- `.github/CODEOWNERS` — todo PR precisa de aprovação do
  `@joao-pedro-gms` (ajuste se necessário para revisão adicional).

## Workflows

### `ci.yml`

Três jobs; `unit-tests` e `backend-integration` dependem de
`static-analysis` (e rodam em paralelo entre si). Os três provisionam o
mesmo JDK do release: `actions/setup-java@v4`, `distribution: temurin`,
`java-version: '21'`.

1. **`static-analysis`** — ktlint + detekt. Falha rápido.
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
   (`lintDevDebug` — a tarefa por variante é obrigatória para que `:app`
   e `:core:data` participem). Publica relatório como artifact.
3. **`backend-integration`** — constrói a imagem de `backend-stub/`,
   sobe o container e aguarda o `/health` (loop de 30 tentativas × 2 s),
   roda o smoke de endpoints (`curl`: PUT/DELETE/tags), a suíte pytest do
   stub e o smoke E2E (`backend-stub/tests/smoke_e2e.py`) contra o
   container. Por fim executa
   `./gradlew :core:data:testDevDebugUnitTest :app:testDevDebugUnitTest`
   com `BASE_URL=http://localhost:8000`. A instrumentação Android
   (`connectedDebugAndroidTest`) **não roda no CI** — exige emulador e foi
   substituída pela suíte Robolectric + MockWebServer (decisão P0-5/R6).

Cache de Gradle configurado em todos os jobs (`actions/cache@v4`).

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

### `codeql.yml`

- Matriz de linguagens `java` + `kotlin` (`fail-fast: false`).
- Além de push/PR, roda semanalmente (cron `17 6 * * 0`) para pegar
  regressões tardias.

## Cobertura de testes (E4.7)

A cobertura de código é medida pelo **Kover** (plugin
`org.jetbrains.kotlinx.kover`, versão em `gradle/libs.versions.toml`)
nos módulos **`:core:domain`** e **`:core:data`**, com um bound mínimo
de **60% de cobertura de linhas** em cada camada.

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
explícito no catálogo e cobertura por `./gradlew :app:assembleDevDebug test
ktlintCheck detekt :app:lintDevDebug` antes de subir PR.

| Componente                 | Versão          | Notas                                                       |
|----------------------------|-----------------|-------------------------------------------------------------|
| JDK (CI + local)           | 21 (Temurin)    | `ci.yml` e `release-apk.yml` provisionam Temurin 21 (unificado). Piso oficial do Gradle 9.7.1/AGP 9.4.1 é 17, mas todo o fluxo roda em 21. O bytecode alvo continua **Java 17**; sem JDK separado, o JBR do Android Studio serve (`export JAVA_HOME=/opt/android-studio/jbr`). |
| Gradle (wrapper)           | 9.7.1           | Exigido pelo AGP 9.4 (`>= 9.6.0`). Não bumpar no toolchain. |
| AGP                        | 9.4.1           | Bump de 8.7.3; habilita built-in Kotlin (ver abaixo).       |
| Kotlin Gradle Plugin (KGP) | 2.3.20          | Exigido >= 2.2.10 pelo AGP 9; KGP 2.3.20 é o último 2.3.x estável. |
| KSP                        | 2.3.12          | Acima do piso 2.3.5 (corrige ciclo kapt/ksp do AGP 9).      |
| Hilt                       | 2.60.1          | Mínimo 2.59 declarado pelo time do Hilt para AGP 9.         |
| Room                       | 2.8.4           | Suporta KSP 2.3.x.                                          |
| desugar_jdk_libs           | 2.1.5           | Habilita `coreLibraryDesugaring` no `:app` para `java.time.Instant` em minSdk 24. |
| ktlint (plugin)            | 14.2.0          | Bump de 12.1.2; engine ktlint 1.x é estritamente mais rígida (chained-call, indentation). |
| detekt                     | 1.23.7          | Sem mudança.                                                |
| Kover                      | 0.9.9           | Sem mudança.                                                |
| Compose BOM                | 2024.10.01      | Sem mudança.                                                |
| Robolectric                | 4.15.1          | Bump de 4.13 (4.14+ traz suporte a Android V/SDK 35; 4.15.1 mantém SDK 35 sem ainda suportar Baklava/SDK 36). |
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
  - Selecione: `Static analysis (ktlint + detekt)` e `Unit tests (JUnit + Compose)`
- ✅ Require conversation resolution before merging
- ✅ Require linear history (evita merges de merge)
- ✅ Include administrators

## Operação local equivalente

```bash
# Requisitos locais: o Android SDK declarado em `local.properties`
# (`sdk.dir=<Android SDK>`, arquivo gitignored) e `JAVA_HOME` apontando
# para um JDK 21+ — sem JDK separado, o JBR do Android Studio serve
# (`export JAVA_HOME=/opt/android-studio/jbr`). O bytecode alvo continua
# Java 17.

# Validar formato antes de subir PR
./gradlew ktlintCheck detekt

# Rodar unit tests (mesma tarefa do CI: a variante `Dev` é obrigatória
# para que `:app` e `:core:data` entrem — `testDebugUnitTest` os ignora)
./gradlew testDevDebugUnitTest

# Rodar lint Android (idem: `lintDebug` pula os módulos com flavor)
./gradlew lintDevDebug

# Verificar cobertura mínima de 60% (E4.7)
./gradlew :core:domain:koverVerify :core:data:koverVerify

# Subir o backend stub e apontar o app para ele
cd backend-stub
docker build -t brainout-stub .
docker run -d --name brainout-stub -p 8000:8000 brainout-stub
# A chave lida pelo build é `brainout.baseUrl.dev` (a env var `BASE_URL`
# tem prioridade sobre ela); o default do flavor `dev` já é 10.0.2.2:8000
echo "brainout.baseUrl.dev=http://10.0.2.2:8000/" >> ../local.properties
# (10.0.2.2 é o host a partir do emulador padrão do Android Studio)
```

## Resolução de problemas

| Sintoma                                       | Causa provável                                  | Ação                                                       |
|-----------------------------------------------|-------------------------------------------------|------------------------------------------------------------|
| `detekt` falha após um PR                     | Nova regra ou código não compatível             | Corrigir o código manualmente: a correção automática está desativada no projeto (`autoCorrect = false` nos 8 módulos e nenhuma regra com `autoCorrect: true` no `detekt.yml`) |
| Job `backend-integration` falha                 | Backend stub não respondeu a tempo no loop de `/health` (30 tentativas × 2 s) | Aumentar as tentativas do loop no step **Run backend stub container** do `ci.yml`; verificar `docker logs brainout-api` |
| `release-apk.yml` falha em `Validate signing secrets (P0-6)` | Secrets `BRAINOUT_*` ausentes | Cadastrar os 4 secrets (ver seção E3.7) |
| `release-apk.yml` falha em `Decode keystore` | `BRAINOUT_KEYSTORE_BASE64` corrompido/incompleto (base64 inválido) | Recodificar: `base64 -w 0 brainout-release.jks` |
| `validateSigningRelease` falha: `Keystore file '.../app/keystore.jks' not found` | Caminho do keystore relativo à raiz, mas `file()` no módulo `:app` resolve contra `app/` | Corrigido no PR #43: o keystore é decodificado em `$RUNNER_TEMP` (path absoluto). Se reaparecer, conferir que `BRAINOUT_KEYSTORE_PATH` é absoluto |
| `signReleaseBundle` falha: `Get Key failed: Given final block not properly padded` | `BRAINOUT_KEY_PASSWORD` difere da senha da chave do keystore (PKCS12 usa a senha do keystore para a chave) | No `keytool -genkeypair` com `-storetype PKCS12` (padrão do JDK 21), usar a MESMA senha em `-storepass` e `-keypass` e cadastrar as duas secrets com esse valor |
| Gradle build falha com `OutOfMemoryError`     | Runner sem heap suficiente                      | `GRADLE_OPTS` no `ci.yml` já é 4g; revisar plugins pesados  |
| Workflow não dispara em PR                    | Branch protection bloqueou o push               | Verificar **Settings → Actions → General → Allow actions** |
| `Cannot find a Java installation matching languageVersion=17` (local) | Nenhum JDK 17 instalado (só o JBR/JDK 21+) | `export JAVA_HOME=/opt/android-studio/jbr` (JBR do Android Studio, ou outro JDK 21+) antes de `./gradlew`. `gradle.properties` já tem `auto-detect=true` e `auto-download=false`; o bytecode continua Java 17 (`sourceCompatibility = VERSION_17`) |