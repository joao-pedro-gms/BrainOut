# BrainOut

Gerenciador de projetos e tarefas para uso individual.
Aplicativo Android nativo escrito em **Kotlin** com **Jetpack Compose**,
persistência local com **Room** e sincronização com serviço de retaguarda
(FastAPI, ver [Backend stub](#backend-stub)).

> Projeto Integrador — Análise e Desenvolvimento de Sistemas — PUC Goiás — 2026/2.

**Autor:** João Pedro G M Silva — PUC Goiás ADS — matrícula 20251012000740.

## Documentos do projeto

- [Documento norteador (PDF)](./Documentos/Documento%20Norteador%20Projeto%20Integrador%20ADS%202026-2.pdf)
- [Roadmap de implementação](./docs/ROADMAP.md) — mapeamento dos 4 ciclos,
  dos requisitos R1–R14 e dos marcos de avaliação.
- [Relatório técnico final](./docs/RELATORIO-TECNICO.md) — mapeamento
  R1–R14 → componente, relatório de testes e instruções de instalação
  (gerado no marco E5.1 do Encerramento).
- [Arquitetura](./docs/ARQUITETURA.md) — definição arquitetural e
  justificativa da pilha tecnológica.
- [CI/CD](./docs/CI-CD.md) — operação dos pipelines de GitHub Actions,
  corte de release e troubleshooting de workflow.
- [Contribuindo](./docs/CONTRIBUTING.md) — fluxo de contribuição, convenções
  e padrões de commit.
- [Roteiro de testes](./docs/ROTEIRO-TESTES.md) e
  [Smoke test de CRUD](./docs/SMOKE-TEST-CRUD.md) — roteiros funcionais
  TF-01..TF-14 e smoke de persistência.
- [Acessibilidade](./docs/ACESSIBILIDADE.md) e
  [Dispositivos](./docs/DISPOSITIVOS.md) — pauta AA e ficha dos
  dispositivos físicos de teste (E5.3).

## Pilha tecnológica

- Kotlin 2.3 (KGP 2.3.20) + AGP 9.4 (9.4.1) + Jetpack Compose + Material 3
- Room (persistência local) + DataStore (preferências)
- Hilt (injeção de dependência) + WorkManager (sincronização)
- KSP (processamento de anotações Room/Hilt)
- ktlint + detekt + Android Lint + Kover (cobertura mínima 60%)
- GitHub Actions (CI/CD)

## Pré-requisitos

| Ferramenta        | Versão mínima | Observação |
|-------------------|---------------|------------|
| JDK               | 21            | Padrão do projeto: `ci.yml` e `release-apk.yml` provisionam Temurin 21. Piso do Gradle 9.7.1/AGP 9.4.1 é 17, mas todo o fluxo é verificado em 21 — use 21+. O bytecode alvo continua **Java 17**. Sem JDK separado, o JBR do Android Studio serve para compilar e testar: `export JAVA_HOME=/opt/android-studio/jbr` antes do `./gradlew` — **exceto para o `detekt`**, que rejeita o JBR 25 (ver [Troubleshooting](#troubleshooting)). `gradle.properties` já tem `org.gradle.java.installations.auto-detect=true` e `auto-download=false`. |
| Android SDK      | compileSdk 37 | Instale pelo Android Studio (SDK Manager) ou `sdkmanager`. O caminho vai em `local.properties` (`sdk.dir`). O `targetSdk` também é 37 e o `minSdk` é 24 (`app/build.gradle.kts` e `core/data/build.gradle.kts`). |
| Android Studio   | Hedgehog (2023.1.1)+ | Para emulador, editor e SDK Manager. |
| Emulador ou dispositivo | API 24+ | A plataforma instalada é `android-37.0` (Android 17). Imagem de emulador usada nesta máquina: `system-images;android-37.0;google_apis_playstore;x86_64` (AVD `Medium_Phone_API_37.0`). Em dispositivo físico basta API 24+ com depuração USB. |
| Python 3 + pip   | 3.12+         | Só para o backend stub local (seção Backend stub). O `backend-stub/Dockerfile` usa `python:3.12-slim`. |

Clone e preparação mínima:

```bash
git clone https://github.com/joao-pedro-gms/BrainOut.git
cd BrainOut
cp local.properties.example local.properties
# edite sdk.dir para o caminho do SDK (ex.: /home/<user>/Android/Sdk)
```

## Build

### APK de debug (flavors dev/prod)

O projeto tem dois product flavors de ambiente (`dev` e `prod`) na
dimensão `environment`:

- **dev** — `BASE_URL` aponta para o backend stub em `http://10.0.2.2:8000/`
  (host a partir do emulador padrão). Precedência: env var `BASE_URL`
  (usada no CI, apontando para o stub dockerizado; barra final garantida) >
  `brainout.baseUrl.dev=<url>` em `local.properties` > default do flavor.
- **prod** — placeholder `https://TBD/` até a hospedagem definitiva
  (decisão E3.1: backend próprio FastAPI; ver `docs/ARQUITETURA.md`,
  Seção 9).

```bash
# Variante dev (usada no dia a dia e no CI de integração)
./gradlew :app:assembleDevDebug
# APK: app/build/outputs/apk/dev/debug/app-dev-debug.apk

# Variante prod
./gradlew :app:assembleProdDebug
# APK: app/build/outputs/apk/prod/debug/app-prod-debug.apk

# Todas as variantes de uma vez
./gradlew assembleDebug
```

Builds verificados neste repositório: `:app:assembleDevDebug`,
`:app:assembleProdDebug` e `:app:bundleDevRelease` (Gradle 9.7, JDK 21).

### Testes

Estado medido na-branch: **618 testes unitários** (84 classes de teste),
0 falhas e 0 erros.

```bash
# Testes unitários — é a tarefa que o CI roda (ci.yml, job unit-tests).
# A variante `Dev` é obrigatória: sem ela, a tarefa cruzada ignora
# silenciosamente os módulos com flavor (:app e :core:data).
# Inclui :core:domain pelo wire-up `gradle.projectsEvaluated` do root.
./gradlew testDevDebugUnitTest

# Módulos SEM flavor (:feature:* e :core:ui) não são alcançados por
# `testDevDebugUnitTest`. O CI os invoca em passos explícitos
# (ci.yml, "Run feature unit tests (explicit)" e "Run core:ui unit
# tests (explicit)"); localmente, rode por módulo:
./gradlew :feature:projects:testDebugUnitTest :feature:tasks:testDebugUnitTest \
          :feature:auth:testDebugUnitTest :feature:settings:testDebugUnitTest
./gradlew :core:ui:testDebugUnitTest

# Módulo puro JVM (mais rápido de tudo)
./gradlew :core:domain:test

# Relatórios: <modulo>/build/reports/tests/testDevDebugUnitTest/

# Android Lint
./gradlew :app:lintDevDebug
```

Distribuição atual dos 618 testes: `:core:data` 302, `:core:domain` 133,
`:core:ui` 88, `:feature:projects` 35, `:app` 24, `:feature:tasks` 18,
`:feature:auth` 15, `:feature:settings` 3.

#### Testes instrumentados (exigem emulador)

A suíte instrumentada vive em `src/androidTest` de `:core:data`
(`MigrationTest`, `UserDaoInstrumentedTest`) e `:feature:auth`
(`LoginScreenTest`). Ela **compila** — `core/data/build.gradle.kts`
declara `androidTestImplementation(libs.truth)` e
`androidTestImplementation(libs.kotlinx.coroutines.test)`, sem os quais
o `compileDevDebugAndroidTestKotlin` falhava com ~40 erros
`Unresolved reference`.

```bash
# Requer emulador ou dispositivo conectado (adb devices)
./gradlew connectedDevDebugAndroidTest
```

Nesta máquina o AVD é `Medium_Phone_API_37.0`
(`system-images;android-37.0;google_apis_playstore;x86_64`), compatível
com o `compileSdk 37` do projeto.

> **O CI NÃO roda teste instrumentado.** Não há runner com emulador no
> plano gratuito do GitHub Actions, então o job `backend-integration`
> substitui deliberadamente a instrumentação pela suíte JVM
> Robolectric + MockWebServer (`ci.yml`, comentário do passo P0-5/R6).
> Consequência honesta: as migrações Room (`MigrationTest`) só são
> verificadas por quem roda emulador, manualmente. Ver
> [docs/CI-CD.md](./docs/CI-CD.md).

### Análise estática e cobertura

```bash
# Formato + regras (mesma dupla do job static-analysis do CI)
./gradlew ktlintCheck detekt

# Cobertura mínima de 60% de linhas em :core:domain e :core:data (E4.7).
# Medido na-branch: :core:domain 84,9% (320/377 linhas), :core:data
# 84,1% (596/709) — o gate de 60% passa com folga.
./gradlew :core:domain:koverVerify :core:data:koverVerify

# Relatório HTML de cobertura
./gradlew :core:domain:koverHtmlReport :core:data:koverHtmlReport
# ou o conjunto das duas camadas:
./gradlew koverMergedHtmlReport
# saída: <modulo>/build/reports/kover/html/index.html
```

**Escopo do gate de ktlint.** O plugin é aplicado em todos os **9
subprojetos** via `subprojects { }` no `build.gradle.kts` raiz, com
`android = true`. Antes disso o plugin só era aplicado na raiz e
`./gradlew ktlintCheck` inspecionava os 2 build scripts da raiz e
**nenhum** dos 221 arquivos `.kt` versionados — o gate não existia de
fato.

Ele roda hoje com **`ignoreFailures = true`**: a engine 1.x do ktlint
14.2.0 sinaliza **191 violações cosméticas de estilo em 17 arquivos**
(indentação, quebra de chamada, vírgula final) já presentes no código.
A flag fica em `true` até a reformatação dedicada, para que o gate
passe a fiscalizar de verdade sem bloquear todo PR. **A reformatação
está pendente** — quando for feita, `ignoreFailures` volta a `false`
e o gate fecha de verdade.

**Limitação local do detekt.** O `detekt` 1.23.7 deriva o `--jvm-target`
do JVM do Gradle e **rejeita o JBR 25** do Android Studio. Ele roda no
CI em Temurin 21. Localmente, uma falha de `detekt` não é um gate
verdadeiro: rode `ktlintCheck`, `testDevDebugUnitTest` e `koverVerify`,
que funcionam com o JBR.

### Release (.aab assinado) — resumo

O release é um `.aab` assinado gerado por `./gradlew :app:bundleRelease`.
A assinatura lê as variáveis de ambiente `BRAINOUT_KEYSTORE_PATH`,
`BRAINOUT_KEYSTORE_PASSWORD`, `BRAINOUT_KEY_ALIAS` e `BRAINOUT_KEY_PASSWORD`.
Sem elas, o build sai **não-assinado** (builds de CI comuns não quebram).

O caminho completo — geração do keystore com `keytool`, cadastro dos
secrets `BRAINOUT_*` no GitHub (o workflow `release-apk.yml` exige
`BRAINOUT_KEYSTORE_BASE64` + as 3 senhas) e corte de tag `v*` — está
documentado em [docs/CI-CD.md](./docs/CI-CD.md). O keystore físico nunca
entra no repositório (`keystore.properties.example` mostra o formato dos
campos; `local.properties` e `keystore.properties` são ignorados pelo git).

## Backend stub

O `backend-stub/` é o serviço de retaguarda usado em desenvolvimento e no
CI: FastAPI com persistência em memória e contrato REST `/v1/projects`,
`/v1/tasks` e `/v1/tags` (política cliente-supplied UUID, upsert
idempotente — detalhes em [backend-stub/README.md](./backend-stub/README.md)).

### Subir o stub com venv

```bash
cd backend-stub
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
python -m uvicorn server:app --host 0.0.0.0 --port 8000
# health: curl http://127.0.0.1:8000/health → {"status":"ok","env":"dev"}
```

### Alternativa com Docker

```bash
cd backend-stub
docker build -t brainout-stub .
docker run -d --name brainout-stub -p 8000:8000 brainout-stub
```

### Testes do stub

```bash
cd backend-stub
pytest -q                     # 16 casos de contrato (tests/test_contract.py)
# smoke ponta-a-ponta (requer servidor de pé):
python -m uvicorn server:app --host 127.0.0.1 --port 8765 &
python tests/smoke_e2e.py
```

### Apontar o app (flavor dev) para o stub

O flavor `dev` já aponta por padrão para `http://10.0.2.2:8000/` —
endereço do host **a partir do emulador**. Se o emulador estiver com o
stub de pé na porta 8000 do host, nada a configurar. Para override:

```bash
echo "brainout.baseUrl.dev=http://10.0.2.2:8000/" >> local.properties
./gradlew :app:assembleDevDebug
```

Em dispositivo físico, troque pelo IP local do computador
(`ip addr` / `hostname -I`), ex. `http://192.168.0.10:8000/`, e rode o
stub com `--host 0.0.0.0`.

### Executar o app no emulador

```bash
# criar AVD uma vez (SDK cmdline-tools instalado):
sdkmanager "system-images;android-37.0;google_apis_playstore;x86_64"
avdmanager create avd -n pixel8 -k "system-images;android-37.0;google_apis_playstore;x86_64" -d pixel_8

# subir emulador e instalar
$ANDROID_HOME/emulator/emulator -avd pixel8 &
adb install app/build/outputs/apk/dev/debug/app-dev-debug.apk
# ou, pelo Android Studio: Run ▶ com variante devDebug selecionada
```

Nesta máquina o AVD já existe com o nome `Medium_Phone_API_37.0`:
`$ANDROID_HOME/emulator/emulator -avd Medium_Phone_API_37.0`.

No Android Studio, a variante se escolhe em **Build → Select Build
Variant** (`devDebug` para desenvolvimento com o stub).

## Troubleshooting

### Erros de build (KSP / Hilt / Room)

| Sintoma | Causa provável | Ação |
|---------|----------------|------|
| `error: unresolved reference` em classes `*_Impl` / `Hilt_*` | KSP não rodou antes da compilação (cache sujo após bump de Kotlin/AGP) | `./gradlew clean` e rebuild; confira versões de KSP/KGP no `gradle/libs.versions.toml` (AGP 9 exige piso KGP 2.2.10) |
| `Cannot find a Java installation matching languageVersion=17` | Só JDK 21 instalado | `export JAVA_HOME=/usr/lib/jvm/java-21-openjdk` antes do `./gradlew` (auto-download está desligado em `gradle.properties`) |
| `MissingTranslation` falha o lint | String nova em `values/strings.xml` sem tradução em `values-en/` | Traduzir a chave (E4.6 — lint com `abortOnError` e `MissingTranslation` como erro) |
| `OutOfMemoryError` no Gradle | Heap insuficiente | `org.gradle.jvmargs=-Xmx4g` já no `gradle.properties`; feche daemons antigos com `./gradlew --stop` |
| Teste unitário do `:app` não acha recursos mesclados | Robolectric precisa dos recursos do app | Já configurado (`testOptions.unitTests.isIncludeAndroidResources = true`); não remover |

### Keystore e assinatura local

- O `:app` lê a assinatura **de variáveis de ambiente**, não de
  `local.properties`. Para release local:
  ```bash
  export BRAINOUT_KEYSTORE_PATH=/caminho/absoluto/brainout-release.jks
  export BRAINOUT_KEYSTORE_PASSWORD=...
  export BRAINOUT_KEY_ALIAS=...
  export BRAINOUT_KEY_PASSWORD=...
  ./gradlew :app:bundleRelease
  ```
- Com PKCS12 (padrão do JDK 21), gere o keystore com a **mesma** senha em
  `-storepass` e `-keypass`; senão `signReleaseBundle` falha com
  `Given final block not properly padded`.
- Sem as variáveis, o build sai não-assinado — de propósito, para não
  quebrar builds comuns.
- O caminho em `BRAINOUT_KEYSTORE_PATH` precisa ser **absoluto** (o
  `file()` no `:app` resolve relativo contra `app/`).

### CI / GitHub Actions

Detalhes completos de workflows, secrets e branch protection em
[docs/CI-CD.md](./docs/CI-CD.md) (seção Resolução de problemas). Resumo:

| Sintoma | Causa provável | Ação |
|---------|----------------|------|
| `connectedDevDebugAndroidTest` falha local | Nenhum dispositivo/emulador visível | `adb devices` vazio = suba o AVD antes; o build do APK instrumentado também é opcional (`assembleDevDebugAndroidTest`). **No CI isso não se aplica** — ver a nota abaixo |
| `compileDevDebugAndroidTestKotlin` falha com `Unresolved reference` | Dependências de teste faltando no bloco `androidTest` | Em `:core:data`, `truth` e `kotlinx-coroutines-test` estão declaradas como `androidTestImplementation`; não mova essas libs só para `testImplementation` |
| `release-apk.yml` falha em `Decode keystore` | Secrets `BRAINOUT_*` ausentes ou base64 corrompido | Cadastrar os 4 secrets; recodificar com `base64 -w 0 brainout-release.jks` |
| `detekt` falha após um PR | Nova regra ou código fora do padrão | `./gradlew detekt --auto-correct` (com cuidado) ou ajustar. **Se a falha for local com o JBR 25**, ela não é um gate: o detekt não roda local no JBR — quem julga é o CI em Temurin 21 |
| `ktlintCheck` acusa centenas de violações de estilo | Engine 1.x do ktlint 14, mais estrita que a 12.x |Esperado enquanto `ignoreFailures = true` estiver no `build.gradle.kts` raiz; a reformatação dedicada está pendente e é o que fecha o gate |
| Workflow não dispara no PR | Branch protection / permissões de Actions | Verificar Settings → Actions → General e as regras de proteção de `main` |

> **O job `backend-integration` NÃO executa `connectedDebugAndroidTest`.**
> Não há runner com emulador no plano gratuito do GitHub Actions; o
> passo foi deliberadamente substituído pela suíte JVM Robolectric +
> MockWebServer (comentário P0-5/R6 em `ci.yml:273-277`). Se você
> procurar esse comando no `ci.yml`, ele não está lá — e essa foi uma
> divergência real entre o que a documentação prometia e o que o CI
> fazia.

### Backend stub e conectividade

- `Connection refused` no app em emulador: o stub não está de pé ou está
  em outra porta. O emulador alcança o host em `10.0.2.2`, não em
  `localhost`.
- Dispositivo físico: use o IP LAN do host e `--host 0.0.0.0` no uvicorn.
- O stub reinicia com dados vazios (persistência em memória) — para reset
  completo, reinicie o processo.

## Estrutura do repositório

```
app/                  # :app — entry point (MainActivity, NavHost, Hilt)
core/
  domain/             # regras de negócio puras (Kotlin JVM) — cobertura Kover
  data/               # Room + DataStore + cliente HTTP — cobertura Kover
  ui/                 # identidade Neo: tema, tokens e componentes Compose
feature/
  auth/               # autenticação (R2)
  projects/           # CRUD de projetos (R3–R5)
  tasks/              # CRUD de tarefas e prazos (R7–R8)
  settings/           # configurações (R9)
backend-stub/         # FastAPI de retaguarda (dev + CI, R6)
config/detekt/        # detekt.yml centralizado
docs/                 # roadmap, arquitetura, CI/CD, testes, atas
Documentos/           # documento norteador do Projeto Integrador
```

### Identidade Neo (`:core:ui`)

`:core:ui` é o módulo que define a identidade visual do app. `MainActivity`
continua chamando `BrainOutTheme { … }`, mas essa função entrega hoje a
**identidade Neo** (neobrutalista) via `BrainOutNeoTheme.kt`:

| Arquivo | Papel |
|---------|-------|
| `theme/BrainOutNeoTheme.kt` | Wrapper Compose Neo — `NeoThemeSpec`, os temas claro/escuro singletons e a injeção de `LocalNeoColors` |
| `theme/NeoColor.kt` | `NeoColors` (paleta Neo) e o mapeamento para os slots do Material 3 (`toMaterialColorScheme`) |
| `theme/NeoTokens.kt` | `NeoSpacing`, `NeoRadii`, `NeoBorders`, `NeoShadows`, `NeoSizes`, `NeoLayout`, `NeoScrim` |
| `theme/NeoTypography.kt` | `NeoFonts`, `NeoTypography` e os quinze estilos tipográficos, sobre as cinco fontes de `res/font` |
| `theme/Theme.kt` | Ponto de entrada `BrainOutTheme` (delega ao Neo) e a variante dinâmica preservada |
| `theme/Shape.kt` | `NeoShapes` (4/4/8/8/16 dp), mais os shapes legados |
| `theme/Color.kt`, `Type.kt` | Paleta e tipografia legadas, mantidas como registro |

`dynamicColor` continua desligado por padrão: ligado em API 31+, ele
substituiria a paleta Neo pela do sistema, quebrando a identidade
determinística que os tokens garantem. A paleta legada
(`BrainOutLightColors`/`BrainOutDarkColors`) ainda existe e é assinada
pelos testes `ThemeSelectionTest` e `ContrastRatioTest`; sua remoção,
junto com a revisão desses testes, é pendência conhecida.

Os 88 testes unitários de `:core:ui` (contratos de token, contraste AA,
tipografia e assets) ficam fora de `testDevDebugUnitTest` — o módulo não
tem flavor. O CI os invoca no passo explícito **Run core:ui unit tests
(explicit)**.

## Licença

A definir junto do congelamento de escopo (E4.8).