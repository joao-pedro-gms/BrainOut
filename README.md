# BrainOut

Gerenciador de projetos e tarefas para uso individual.
Aplicativo Android nativo escrito em **Kotlin** com **Jetpack Compose**,
persistência local com **Room** e sincronização com serviço de retaguarda.

> Projeto Integrador — Análise e Desenvolvimento de Sistemas — PUC Goiás — 2026/2.

**Autor:** João Pedro G M Silva — PUC Goiás ADS — matrícula 20251012000740.

## Documentos do projeto

- [Documento norteador (PDF)](./Documentos/Documento%20Norteador%20Projeto%20Integrador%20ADS%202026-2.pdf)
- [Roadmap de implementação](./docs/ROADMAP.md) — mapeamento dos 4 ciclos,
  dos requisitos R1–R14 e dos marcos de avaliação.
- [Arquitetura](./docs/ARQUITETURA.md) — definição arquitetural e
  justificativa da pilha tecnológica.
- [CI/CD](./docs/CI-CD.md) — operação dos pipelines de GitHub Actions.
- [Contribuindo](./docs/CONTRIBUTING.md) — fluxo de contribuição, convenções
  e padrões de commit.

## Pilha tecnológica

- Kotlin (JDK 17) + Jetpack Compose + Material 3
- Room (persistência local) + DataStore (preferências)
- Hilt (injeção de dependência) + WorkManager (sincronização)
- Ktor ou Retrofit (cliente HTTP)
- ktlint + detekt + Android Lint
- GitHub Actions (CI/CD)

## Status atual

Este repositório está no início do Ciclo 1. O esqueleto de navegação, o
módulo de autenticação e a integração com Room estão previstos para a
entrega da N1 (28/09 a 02/10).

## Como executar

Pré-requisitos:

- JDK 17 (recomendado: Temurin via `pacman -S jdk17-temurin` ou via
  Android Studio). JDK 21 também funciona — exportar `JAVA_HOME` antes
  do `./gradlew`.
- Android Studio Hedgehog (2023.1.1) ou superior com SDK 34.
- Emulador `pixel8` configurado, ou dispositivo físico com depuração USB.

```bash
git clone https://github.com/joao-pedro-gms/BrainOut.git
cd BrainOut
./gradlew assembleDebug
# APK em app/build/outputs/apk/debug/app-debug.apk
```

Para detalhes completos, consulte [docs/CI-CD.md](./docs/CI-CD.md) e
[docs/CONTRIBUTING.md](./docs/CONTRIBUTING.md).

## Licença

Definir no E3.1 conforme decisão própria.