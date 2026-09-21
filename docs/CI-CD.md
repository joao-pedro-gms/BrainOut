# CI/CD — BrainOut

> Documento de operação dos pipelines de GitHub Actions. Atualize quando
> secrets ou workflows mudarem. Item desejável 5.1 do documento norteador
> (não obrigatório, mas fortalece a rubrica de qualidade técnica).

## Visão geral

| Workflow             | Gatilho                            | O que faz                                                       |
|----------------------|------------------------------------|-----------------------------------------------------------------|
| `ci.yml`             | Push em `main`, PR contra `main`   | Análise estática (ktlint + detekt), testes unitários, integração com backend stub |
| `release-apk.yml`    | Tag `v*`                           | Gera `.aab` assinado (`bundleRelease`); publica como artifact do run             |
| `codeql.yml`         | Push em `main`, PR, semanalmente   | Análise estática de segurança (CodeQL)                          |

Configuração adicional:

- `.github/dependabot.yml` — atualizações semanais de GitHub Actions,
  Gradle e (quando existir) `backend-stub/`.
- `.github/CODEOWNERS` — todo PR precisa de aprovação do
  `@joao-pedro-gms` (ajuste se necessário para revisão adicional).

## Workflows

### `ci.yml`

Três jobs sequenciais:

1. **`static-analysis`** — ktlint + detekt. Falha rápido.
2. **`unit-tests`** — depende de `static-analysis`; roda testes
   unitários e Android Lint. Publica relatório como artifact.
3. **`backend-integration`** — depende de `static-analysis`; constrói
   `backend-stub/Dockerfile`, sobe o container, executa
   `connectedDebugAndroidTest`. É o caminho de teste para R6.

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
- **Artifact:** `brainout-release-aab-<tag>`, contendo o `.aab` de
  `app/build/outputs/bundle/release/`, retenção de 30 dias.
  Não há upload automático para loja externa.

### `codeql.yml`

- Matriz de linguagens `java` + `kotlin` (`fail-fast: false`).
- Roda semanalmente fora do fluxo de PRs para pegar regressões tardias.

## Cobertura de testes (E4.7)

A cobertura de código é medida pelo **Kover** (plugin
`org.jetbrains.kotlinx.kover`, versão em `gradle/libs.versions.toml`)
nos módulos **`:core:domain`** e **`:core:data`**, com um bound mínimo
de **60% de cobertura de linhas** em cada camada.

### Rodar localmente

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk

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
> existirem, o push de uma tag `v*` falha em "Decode keystore" — nenhum
> `.aab` assinado é produzido antes disso.

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
# Opção 1 — jarsigner (JDK)
jarsigner -verify -verbose -certs app-release.aab

# Opção 2 — apksigner (build-tools do Android SDK)
apksigner verify --print-certs app-release.aab
```

## Toolchain

Versões centralizadas em `gradle/libs.versions.toml`. A política é bump
explícito no catálogo e cobertura por `./gradlew :app:assembleDevDebug test
ktlintCheck detekt :app:lintDevDebug` antes de subir PR.

| Componente                 | Versão          | Notas                                                       |
|----------------------------|-----------------|-------------------------------------------------------------|
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
| compileSdk / targetSdk     | 35              | Mantidos em 35 (próximo bump exige E5.x).                   |

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
- Opt-out temporário via `android.builtInKotlin=false` no
  `gradle.properties` permanece disponível para debugging; será
  removido no AGP 10.

### Deprecations resolvidas neste bump

- **`android.nonFinalResIds=false`** removido do `gradle.properties` —
  deprecated no AGP 9, default agora é `true`.
- **Migrate to built-in Kotlin** (developer.android.com/build/migrate-to-built-in-kotlin)
  — ver tabela acima.
- **`android.kotlinOptions` DSL** removida em todos os módulos Android.

### Deprecations pré-existentes (fora do escopo deste bump)

- `compileSdk = 35` (lint sugere 37). Aguardando decisão em E5.x.
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
  - Selecione: `Static analysis (ktlint + detekt)` e `Unit tests`
- ✅ Require conversation resolution before merging
- ✅ Require linear history (evita merges de merge)
- ✅ Include administrators

## Operação local equivalente

```bash
# Validar formato antes de subir PR
./gradlew ktlintCheck detekt

# Rodar unit tests
./gradlew testDebugUnitTest

# Rodar lint Android
./gradlew lintDebug

# Verificar cobertura mínima de 60% (E4.7)
./gradlew :core:domain:koverVerify :core:data:koverVerify

# Subir o backend stub e apontar o app para ele
cd backend-stub
docker build -t brainout-stub .
docker run -d --name brainout-stub -p 8000:8000 brainout-stub
echo "BASE_URL=http://10.0.2.2:8000" >> ../local.properties
# (10.0.2.2 é o host a partir do emulador padrão do Android Studio)
```

## Resolução de problemas

| Sintoma                                       | Causa provável                                  | Ação                                                       |
|-----------------------------------------------|-------------------------------------------------|------------------------------------------------------------|
| `detekt` falha após um PR                     | Nova regra ou código não compatível             | `./gradlew detekt --auto-correct` (com cuidado) ou ajustar |
| `connectedDebugAndroidTest` falha no CI       | Backend stub não respondeu a tempo             | Aumentar `--health-retries` no `ci.yml`; verificar `docker logs` |
| `release-apk.yml` falha em `Decode keystore`  | Secrets `BRAINOUT_*` ausentes ou base64 corrompido | Cadastrar os 4 secrets (ver seção E3.7); recodificar: `base64 -w 0 brainout-release.jks` |
| `validateSigningRelease` falha: `Keystore file '.../app/keystore.jks' not found` | Caminho do keystore relativo à raiz, mas `file()` no módulo `:app` resolve contra `app/` | Corrigido no PR #43: o keystore é decodificado em `$RUNNER_TEMP` (path absoluto). Se reaparecer, conferir que `BRAINOUT_KEYSTORE_PATH` é absoluto |
| `signReleaseBundle` falha: `Get Key failed: Given final block not properly padded` | `BRAINOUT_KEY_PASSWORD` difere da senha da chave do keystore (PKCS12 usa a senha do keystore para a chave) | No `keytool -genkeypair` com `-storetype PKCS12` (padrão do JDK 21), usar a MESMA senha em `-storepass` e `-keypass` e cadastrar as duas secrets com esse valor |
| Gradle build falha com `OutOfMemoryError`     | Runner sem heap suficiente                      | `GRADLE_OPTS` no `ci.yml` já é 4g; revisar plugins pesados  |
| Workflow não dispara em PR                    | Branch protection bloqueou o push               | Verificar **Settings → Actions → General → Allow actions** |
| `Cannot find a Java installation matching languageVersion=17` (local) | JDK 17 não instalado; só JDK 21 disponível | `export JAVA_HOME=/usr/lib/jvm/java-21-openjdk` antes de `./gradlew`. `gradle.properties` já tem `auto-detect=true` e `auto-download=false`, então o JDK 21 local funciona e o bytecode continua Java 17 |