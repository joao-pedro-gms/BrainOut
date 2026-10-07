# BrainOut

Gerenciador de projetos e tarefas para uso individual.
Aplicativo Android nativo escrito em **Kotlin** com **Jetpack Compose** e
persistência **totalmente local** em **Room**. Não há backend, API externa,
conta remota nem sincronização: os dados ficam no próprio dispositivo, e a
única forma de levá-los de uma instalação a outra é o arquivo de backup
exportado e restaurado pelo app (ver [Dados e backup](#dados-e-backup)).

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
- Hilt (injeção de dependência) + WorkManager (lembretes locais de prazo)
- KSP (processamento de anotações Room/Hilt)
- ktlint + detekt + Android Lint + Kover (cobertura mínima 60%)
- GitHub Actions (CI/CD)

## Pré-requisitos

| Ferramenta        | Versão mínima | Observação |
|-------------------|---------------|------------|
| JDK               | 21            | Padrão do projeto: `ci.yml` e `release-apk.yml` provisionam Temurin 21. Piso do Gradle 9.7.1/AGP 9.4.1 é 17, mas todo o fluxo é verificado em 21 — use 21+. O bytecode alvo continua **Java 17**. Sem JDK separado, o JBR do Android Studio serve: `export JAVA_HOME=/opt/android-studio/jbr` antes do `./gradlew`. `gradle.properties` já tem `org.gradle.java.installations.auto-detect=true` e `auto-download=false`. |
| Android SDK      | compileSdk 35 | Instale pelo Android Studio (SDK Manager) ou `sdkmanager`. O caminho vai em `local.properties` (`sdk.dir`). |
| Android Studio   | Hedgehog (2023.1.1)+ | Para emulador, editor e SDK Manager. |
| Emulador ou dispositivo | API 24+ | Emulador de API 35 (imagem `system-images;android-35;google_apis;x86_64`) ou dispositivo físico com depuração USB. |

Clone e preparação mínima:

```bash
git clone https://github.com/joao-pedro-gms/BrainOut.git
cd BrainOut
cp local.properties.example local.properties
# edite sdk.dir para o caminho do SDK (ex.: /home/<user>/Android/Sdk)
```

## Build

### APK de debug

O projeto declara dois flavors de ambiente (`dev` e `prod`) na dimensão
`environment`; eles se diferenciam apenas por configuração de build, sem
qualquer serviço associado. O app não faz requisições de rede em nenhuma
variante.

```bash
# Variante dev (usada no dia a dia e no CI)
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
./gradlew testDevDebugUnitTest

# Relatórios: <modulo>/build/reports/tests/testDevDebugUnitTest/
# Android Lint
./gradlew :app:lintDevDebug

# Testes instrumentados — requerem emulador/dispositivo conectado
./gradlew connectedDevDebugAndroidTest
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

## Dados e backup

O BrainOut funciona **inteiramente no dispositivo**. Não existe servidor,
serviço de retaguarda, API externa, conta remota, telemetria ou
sincronização de dados. Projetos, tarefas, tags, contas locais e
preferências ficam no banco Room do próprio aparelho; nenhuma informação sai
do dispositivo por conta do app.

A única transferência de dados prevista — entre instalações ou como cópia de
segurança — é o **arquivo de backup**, exportado e restaurado pelo próprio
app. Não há nenhum outro canal de entrada ou de saída de dados, e o app não
depende de conectividade para funcionar.

### Executar o app no emulador

```bash
# criar AVD uma vez (SDK cmdline-tools instalado):
sdkmanager "system-images;android-35;google_apis;x86_64"
avdmanager create avd -n pixel8 -k "system-images;android-35;google_apis;x86_64" -d pixel_8

# subir emulador e instalar
$ANDROID_HOME/emulator/emulator -avd pixel8 &
adb install app/build/outputs/apk/dev/debug/app-dev-debug.apk
# ou, pelo Android Studio: Run ▶ com a variante devDebug selecionada
```

No Android Studio, a variante se escolhe em **Build → Select Build
Variant**. O emulador pode ficar sem rede: o app não faz chamadas externas.

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
| `release-apk.yml` falha em `Decode keystore` | Secrets `BRAINOUT_*` ausentes ou base64 corrompido | Cadastrar os 4 secrets; recodificar com `base64 -w 0 brainout-release.jks` |
| `detekt` falha após um PR | Nova regra ou código fora do padrão | `./gradlew detekt --auto-correct` (com cuidado) ou ajustar |
| Workflow não dispara no PR | Branch protection / permissões de Actions | Verificar Settings → Actions → General e as regras de proteção de `main` |

## Estrutura do repositório

```
app/                  # :app — entry point (MainActivity, NavHost, Hilt)
core/
  domain/             # regras de negócio puras (Kotlin JVM) — cobertura Kover
  data/               # Room + DataStore — persistência local — cobertura Kover
  ui/                 # tema e componentes Compose compartilhados
feature/
  auth/               # autenticação local (R2)
  projects/           # CRUD de projetos (R3–R5)
  tasks/              # CRUD de tarefas e prazos (R7–R8)
  settings/           # configurações (R9)
config/detekt/        # detekt.yml centralizado
docs/                 # roadmap, arquitetura, CI/CD, testes, atas
Documentos/           # documento norteador do Projeto Integrador
```

## Licença

A definir junto do congelamento de escopo (E4.8).
