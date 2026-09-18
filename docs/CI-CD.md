# CI/CD — BrainOut

> Documento de operação dos pipelines de GitHub Actions. Atualize quando
> secrets ou workflows mudarem. Item desejável 5.1 do documento norteador
> (não obrigatório, mas fortalece a rubrica de qualidade técnica).

## Visão geral

| Workflow             | Gatilho                            | O que faz                                                       |
|----------------------|------------------------------------|-----------------------------------------------------------------|
| `ci.yml`             | Push em `main`, PR contra `main`   | Análise estática (ktlint + detekt), testes unitários, integração com backend stub |
| `release-apk.yml`    | Tag `v*` ou execução manual        | Gera `.aab`/`.apk` assinado; publica como artifact do run e cria Release |
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

### `release-apk.yml`

- **Gatilhos:** tag `v*` (ex.: `v1.0.0`) ou execução manual via
  `Actions → Release APK/AAB → Run workflow` com escolha entre `aab` ou
  `apk`.
- **Assinatura:** keystore fornecido via secrets do repositório.
- **Artifact:** nome `brainout-release-<formato>-<sha>`,
  retenção de 30 dias.
- **Tag:** quando acionado por tag, publica o artifact na GitHub Release
  com notas geradas automaticamente. Sem tag, o artifact fica só no run.

### `codeql.yml`

- Matriz de linguagens `java` + `kotlin` (`fail-fast: false`).
- Roda semanalmente fora do fluxo de PRs para pegar regressões tardias.

## Secrets necessários

Configure em **Settings → Secrets and variables → Actions → New repository
secret**. O CI falha com mensagem clara quando algum estiver ausente.

| Secret                         | Usado em            | Conteúdo                                                                 |
|--------------------------------|---------------------|--------------------------------------------------------------------------|
| `RELEASE_KEYSTORE_BASE64`      | `release-apk.yml`   | Conteúdo do `.jks`/`.keystore` codificado em base64 (uma linha só)      |
| `RELEASE_KEYSTORE_PASSWORD`    | `release-apk.yml`   | Senha do keystore                                                        |
| `RELEASE_KEY_ALIAS`            | `release-apk.yml`   | Alias da chave dentro do keystore                                        |
| `RELEASE_KEY_PASSWORD`         | `release-apk.yml`   | Senha da chave (pode ser igual à do keystore)                            |

### Como gerar o base64 do keystore

```bash
base64 -w 0 release.keystore > release.keystore.b64
# copie o conteúdo de release.keystore.b64 para o secret
```

### Como gerar um keystore novo

```bash
keytool -genkey -v \
  -keystore release.keystore \
  -alias brainout \
  -keyalg RSA -keysize 2048 \
  -validity 10000
```

Guarde o arquivo físico em local seguro (1Password/Proton Pass/cofre do
time). Se ele se perder, as builds existentes continuam assinadas, mas você
não conseguirá publicar novas atualizações.

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
| `release-apk.yml` falha em `Decode keystore`  | Secret vazio ou base64 corrompido               | Recodificar: `base64 -w 0 release.keystore`                 |
| Gradle build falha com `OutOfMemoryError`     | Runner sem heap suficiente                      | `GRADLE_OPTS` no `ci.yml` já é 4g; revisar plugins pesados  |
| Workflow não dispara em PR                    | Branch protection bloqueou o push               | Verificar **Settings → Actions → General → Allow actions** |
| `Cannot find a Java installation matching languageVersion=17` (local) | JDK 17 não instalado; só JDK 21 disponível | `export JAVA_HOME=/usr/lib/jvm/java-21-openjdk` antes de `./gradlew`. `gradle.properties` já tem `auto-detect=true` e `auto-download=false`, então o JDK 21 local funciona e o bytecode continua Java 17 |