# BrainOut

> **Contas locais — direção aprovada em 26/10/2026:** cadastro com nome de usuário,
> senha e confirmação; login por nome e senha, com distinção de maiúsculas.
> Uma sessão ativa e dados independentes por conta. Papéis OWNER/MEMBER e e-mail
> deixam o fluxo atual. Room v6 preserva bases anteriores; backup pessoal v2
> importa dados para a conta conectada e lê arquivos v1 mediante seleção.
> Ver [decisão de contas](docs/adr/0002-contas-locais-independentes.md) e [contrato de backup](docs/BACKUP.md).
> As referências históricas abaixo a papéis e backup do aparelho inteiro não
> representam esse novo contrato.

Gerenciador pessoal de projetos e tarefas para Android, escrito em Kotlin
com Jetpack Compose. Dados, autenticação, preferências, calendário de feriados
e lembretes funcionam localmente. O aplicativo não utiliza backend ou APIs
externas e não solicita permissões de internet.

> Projeto Integrador — ADS — PUC Goiás — 2026/2.
> Autor: João Pedro G M Silva — matrícula 20251012000740.

## Documentação

- [Decisão e transição local](docs/TRANSICAO-LOCAL.md)
- [Roadmap](docs/ROADMAP.md) e [arquitetura](docs/ARQUITETURA.md)
- [Operação da CI e releases](docs/CI-CD.md)
- [Contribuição](docs/CONTRIBUTING.md)
- [Relatório técnico](docs/RELATORIO-TECNICO.md)
- [Roteiro de testes](docs/ROTEIRO-TESTES.md) e [smoke de CRUD](docs/SMOKE-TEST-CRUD.md)
- [Acessibilidade](docs/ACESSIBILIDADE.md) e [dispositivos](docs/DISPOSITIVOS.md)
- [Documento norteador](Documentos/Documento%20Norteador%20Projeto%20Integrador%20ADS%202026-2.pdf)

Os relatórios e planos anteriores preservam o histórico acadêmico. As seções
sobre backend/sync nesses documentos não descrevem o produto atual; a
reconciliação final com o código foi feita na issue #208 (ledger
`docs/DEFEITOS.md` e guias de dispositivos, backup e transição).

## Build local

Requisitos: JDK 21 e Android SDK com plataforma 37. O alvo de bytecode é Java
17 e o minSdk é 24. As versões das dependências estão em
`gradle/libs.versions.toml`; o wrapper fixa a versão do Gradle.

```bash
git clone https://github.com/joao-pedro-gms/BrainOut.git
cd BrainOut
cp local.properties.example local.properties
# Ajuste sdk.dir para o SDK instalado, ou configure ANDROID_HOME.
./gradlew :app:assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

No Windows, use `./gradlew.bat`. Não existem mais flavors `dev`/`prod`,
`BASE_URL` ou serviço de backend para iniciar. O applicationId publicado
continua `pucgo.joaopedrogmsilva.brainout`; debug mantém o sufixo `.debug`.

## Qualidade

```bash
./gradlew ktlintCheck detekt
./gradlew testDebugUnitTest
./gradlew :core:domain:koverVerify :core:data:koverVerify
./gradlew lintDebug
python3 -m unittest discover -s .github/tests -v
python3 docs/design/verify_tokens.py
python3 docs/design/verify_roadmap.py
```

Todos os módulos Android usam a variante debug. O comando agregado de testes
também executa o domínio JVM pelo vínculo configurado no build raiz. Kover
exige pelo menos 60% de linhas em `:core:domain` e `:core:data`. Ktlint,
detekt e os gates de tradução das features são bloqueantes.

Os testes JVM com Robolectric exercitam o upgrade v4→v5 contra o schema
exportado, rollback de transações, cascatas de CRUD e cancelamento seletivo
do sync antigo. A suíte instrumentada também compila na CI:

```bash
./gradlew :core:data:assembleDebugAndroidTest :feature:auth:assembleDebugAndroidTest
# Execução real requer emulador/dispositivo:
./gradlew :core:data:connectedDebugAndroidTest :feature:auth:connectedDebugAndroidTest
```

Compilar a suíte instrumentada não equivale a executá-la em um dispositivo.

## Dados e atualização

Room v5 preserva usuários, projetos, tarefas, tags e associações da v4. A
migração remove somente `pending_ops`; os dados já eram gravados localmente.
A v5 também normaliza o default legado de `completed_at` quando necessário,
copiando todas as colunas e preservando os registros e os índices.
Na inicialização, o aplicativo cancela os trabalhos legados de sync por nome
ou tag, sem cancelar os lembretes. WorkManager permanece para notificações
locais de prazo.

Os avisos de feriados usam um calendário nacional calculado no aparelho,
conforme as regras atuais. Não incluem feriados estaduais/municipais nem
pontos facultativos. Mudanças futuras dessas regras exigem atualização do app.

**Backup manual exportável/restaurável** existe (#214): Configurações →
Backup → Exportar/Restaurar, pelo seletor de arquivos do sistema, com
arquivo cifrado (AES-256-GCM) e senha do usuário. O contrato do arquivo
está em [docs/BACKUP.md](docs/BACKUP.md). O backup automático do Android
está **desligado** (#175): `allowBackup=false` e regras de extração que
excluem tudo — o hash de senha e o id de sessão não circulam. Não use
cópia do banco como garantia de portabilidade de contas, pois o pepper
de senhas depende do Android Keystore.

## Releases

Tags `v*` acionam o APK debug e o AAB assinado. O APK é anexado à GitHub Release
como `BrainOut-<tag>-debug.apk`. O AAB requer os quatro secrets `BRAINOUT_*`
descritos em [CI/CD](docs/CI-CD.md). Sem configuração local de assinatura,
o build release permanece unsigned; o workflow assinado valida os secrets.

## Módulos

| Módulo | Responsabilidade |
|---|---|
| `:app` | Application, navegação e WorkManager de lembretes |
| `:core:domain` | Modelos, regras e portas Kotlin JVM |
| `:core:data` | Room, DataStore, autenticação local e calendário |
| `:core:ui` | Tema Neo, tokens, fontes e vetores |
| `:feature:auth` | Cadastro, login e sessão local |
| `:feature:projects` | Projetos, tags e detalhe das tarefas |
| `:feature:tasks` | Tarefas e dashboard |
| `:feature:settings` | Configurações |

`Documentos/`, `.omo/` e `docs/ATAS/` não devem ser editados.
