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

- Kotlin 2.3 + AGP 9.4 + Jetpack Compose + Material 3
- Room (persistência local) + DataStore (preferências)
- Hilt (injeção de dependência) + WorkManager (sincronização)
- KSP (processamento de anotações Room/Hilt)
- ktlint + detekt + Android Lint + Kover (cobertura mínima 60%)
- GitHub Actions (CI/CD)

## Pré-requisitos

| Ferramenta        | Versão mínima | Observação |
|-------------------|---------------|------------|
| JDK               | 17            | JDK 21 também funciona (bytecode permanece Java 17). Com só o 21 instalado, exporte `JAVA_HOME=/usr/lib/jvm/java-21-openjdk` antes do `./gradlew`. `gradle.properties` já tem `org.gradle.java.installations.auto-detect=true` e `auto-download=false`. |
| Android SDK      | compileSdk 35 | Instale pelo Android Studio (SDK Manager) ou `sdkmanager`. O caminho vai em `local.properties` (`sdk.dir`). |
| Android Studio   | Hedgehog (2023.1.1)+ | Para emulador, editor e SDK Manager. |
| Emulador ou dispositivo | API 24+ | Emulador de API 35 (imagem `system-images;android-35;google_apis;x86_64`) ou dispositivo físico com depuração USB. |
| Python 3 + pip   | 3.10+         | Só para o backend stub local (seção Backend stub). |

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
  (host a partir do emulador padrão). Override por desenvolvedor:
  `brainout.baseUrl.dev=<url>` em `local.properties`.
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

```bash
# Testes unitários (todos os módulos; inclui :core:domain via wire-up no root)
./gradlew testDebugUnitTest

# Relatórios: <modulo>/build/reports/tests/testDebugUnitTest/
# Android Lint
./gradlew :app:lintDevDebug

# Testes instrumentados — requerem emulador/dispositivo conectado
./gradlew connectedDevDebugAndroidTest
# (o CI usa ./gradlew connectedDebugAndroidTest no job backend-integration)
```

### Análise estática e cobertura

```bash
# Formato + regras (mesma dupla do job static-analysis do CI)
./gradlew ktlintCheck detekt

# Cobertura mínima de 60% de linhas em :core:domain e :core:data (E4.7)
./gradlew :core:domain:koverVerify :core:data:koverVerify

# Relatório HTML de cobertura
./gradlew :core:domain:koverHtmlReport :core:data:koverHtmlReport
# ou o conjunto das duas camadas:
./gradlew koverMergedHtmlReport
# saída: <modulo>/build/reports/kover/html/index.html
```

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
pytest -q                     # 14 casos de contrato
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
sdkmanager "system-images;android-35;google_apis;x86_64"
avdmanager create avd -n pixel8 -k "system-images;android-35;google_apis;x86_64" -d pixel_8

# subir emulador e instalar
$ANDROID_HOME/emulator/emulator -avd pixel8 &
adb install app/build/outputs/apk/dev/debug/app-dev-debug.apk
# ou, pelo Android Studio: Run ▶ com variante devDebug selecionada
```

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
| `connectedDebugAndroidTest` falha no CI | Backend stub não respondeu a tempo | Verificar `docker logs`; conferir o health check do job `backend-integration` |
| `release-apk.yml` falha em `Decode keystore` | Secrets `BRAINOUT_*` ausentes ou base64 corrompido | Cadastrar os 4 secrets; recodificar com `base64 -w 0 brainout-release.jks` |
| `detekt` falha após um PR | Nova regra ou código fora do padrão | `./gradlew detekt --auto-correct` (com cuidado) ou ajustar |
| Workflow não dispara no PR | Branch protection / permissões de Actions | Verificar Settings → Actions → General e as regras de proteção de `main` |

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
  ui/                 # tema e componentes Compose compartilhados
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

## Licença

A definir junto do congelamento de escopo (E4.8).