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
| Gradle build falha com `OutOfMemoryError`     | Runner sem heap suficiente                      | `GRADLE_OPTS` no `ci.yml` já é 4g; revisar plugins pesados  |
| Workflow não dispara em PR                    | Branch protection bloqueou o push               | Verificar **Settings → Actions → General → Allow actions** |
| `Cannot find a Java installation matching languageVersion=17` (local) | JDK 17 não instalado; só JDK 21 disponível | `export JAVA_HOME=/usr/lib/jvm/java-21-openjdk` antes de `./gradlew`. `gradle.properties` já tem `auto-detect=true` e `auto-download=false`, então o JDK 21 local funciona e o bytecode continua Java 17 |