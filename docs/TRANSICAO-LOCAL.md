# BrainOut — decisão de escopo e organização do repositório

Decisão aprovada pelo autor em **16/10/2026**. Base inspecionada:
`main` em `e49181b`. Organização: [#210](https://github.com/joao-pedro-gms/BrainOut/issues/210).

## Decisão e estado implementado

O produto alvo é um aplicativo Android **totalmente local**, com Room,
DataStore e lembretes locais. Backend, sincronização, APIs externas e
colaboração remota deixam de fazer parte do produto alvo. A transferência
planejada de dados é um arquivo de backup exportável/restaurável.

**A migração ainda não foi implementada.** O código desta base continua
agendando sincronização, injetando clientes HTTP e consultando feriados pela
rede. Backup manual ainda não existe. Os documentos anteriores descrevem a
implementação legada ou decisões históricas; esta decisão prevalece para
planejar trabalho novo, sem transformar funcionalidades planejadas em prontas.

A mudança de R6 e seu efeito na avaliação acadêmica precisam ser reconciliados
no relatório de conformidade. O PDF norteador em `Documentos/` permanece
inalterado; a decisão de produto não é evidência de aprovação acadêmica.

## Mapa da codebase atual

| Camada | Responsabilidade observada | Direção |
|---|---|---|
| `:app` | Bootstrap Hilt, navegação, workers de sync e de notificações | Preservar navegação e lembretes; retirar sync e cancelar trabalhos legados |
| `:core:domain` | Modelos, regras, portas e casos de uso Kotlin JVM | Preservar independência de Android e regras locais |
| `:core:data` | Room v4, DataStore, sessão, PBKDF2, HTTP e fila `pending_ops` | Preservar dados/segurança; migrar transações e remover rede/fila |
| `:core:ui` | Tema Neo, fontes, vetores e componentes Compose | Preservar identidade e acessibilidade |
| `:feature:*` | Auth, projects, tasks e settings; UiState e ViewModels | Retirar estados remotos, preservar erros e retry locais |
| `backend-stub/` | FastAPI em memória e testes de contrato | Legado a retirar com seus consumidores e gates |
| `.github/workflows/` | Análise estática, testes, cobertura, integração e release | Manter gates locais e substituir a integração remota |

Evidências executáveis principais:

- `app/src/main/kotlin/pucgo/joaopedrogmsilva/brainout/BrainOutApplication.kt`
  chama `syncScheduler.ensurePeriodicSync()` e `syncConnectivityWatcher.start()`.
- `core/data/src/main/kotlin/pucgo/joaopedrogmsilva/brainout/core/data/di/DataModule.kt`
  ainda fornece a infraestrutura de rede e seus consumidores.
- `app/src/main/AndroidManifest.xml` permite backup Android e tráfego cleartext;
  a decisão local não altera automaticamente essas configurações.
- `settings.gradle.kts` inclui oito módulos. A raiz Gradle e os oito módulos
  explicam as referências históricas a nove projetos.
- O catálogo atual fixa Kotlin **2.4.21**, Compose BOM **2026.09.00**, AGP
  **9.4.1**, detekt **1.23.8** e Kover **0.9.9**; o wrapper fixa Gradle
  **9.8.1**. Não usar versões antigas citadas em prosa para fazer alterações.
- `build.gradle.kts` configura `ignoreFailures.set(false)` no ktlint dos
  subprojetos: o gate de estilo bloqueia. DEF-10 já está registrado como corrigido.

## Ordem de execução e backlog

O [épico local #211](https://github.com/joao-pedro-gms/BrainOut/issues/211)
coordena a migração. As issues existentes continuam sendo referências dos
defeitos; os novos itens coordenam a mudança de arquitetura e seus critérios.

| Ordem | Item | Dependência e critério principal |
|---|---|---|
| 0 | [#210 — organização](https://github.com/joao-pedro-gms/BrainOut/issues/210) | Decisão e inventário verificável |
| 1 | [#212 — remover rede/sync](https://github.com/joao-pedro-gms/BrainOut/issues/212) | Migração não destrutiva; preservar CRUD e lembretes; cancelar trabalhos antigos |
| 2 | [#213 — interface local](https://github.com/joao-pedro-gms/BrainOut/issues/213) | Depende de #212; retirar banners remotos e manter tratamento de falha local |
| 3 | [#214 — backup](https://github.com/joao-pedro-gms/BrainOut/issues/214) | Depende de #212; definir portabilidade de contas/pepper antes da implementação |
| 4 | [#215 — regressão/CI](https://github.com/joao-pedro-gms/BrainOut/issues/215) | Depende de #212–#214; instalação, upgrade, restauração e gates locais |
| 5 | [#208 — documentação final](https://github.com/joao-pedro-gms/BrainOut/issues/208) | Após a implementação; reconciliar ledger, arquitetura, roadmap e conformidade |

O [épico de defeitos #209](https://github.com/joao-pedro-gms/BrainOut/issues/209)
permanece como inventário. Sua ordem antiga orientada a sync é histórica e
deve ser lida junto da sequência acima.

| Label | Uso |
|---|---|
| `scope: local` | Produto alvo, bugs locais e trabalho de transição |
| `scope: legacy-network` | Achados de rede a reavaliar como parte da remoção |
| `status: needs-update` | PR/issue requer atualização contra main ou a decisão |
| `status: confirmed-fixed` | Correção integrada comprovada por código e testes |

Um bug de sync não fica corrigido porque se planejou remover sync. Só fechar
esses itens após demonstrar a remoção do caminho de produção, ou substituí-los
com justificativa rastreável. Da mesma forma, não apagar branches apenas por
parecerem antigas: preservar trabalho não integrado e branches de PRs abertos.

## Revisão dos PRs abertos

Na consulta desta auditoria, os quatro PRs tinham conflitos com `main`. Os
checks verdes observados pertenciam aos SHAs antigos; uma atualização exige
nova execução antes do merge.

| PR | Resultado da revisão | Próxima ação |
|---|---|---|
| [#140](https://github.com/joao-pedro-gms/BrainOut/pull/140) | Direção local compatível; draft com instruções de agente pendentes e conflitos | Reconciliar com este documento e preservar descrição honesta da rede ainda existente |
| [#141](https://github.com/joao-pedro-gms/BrainOut/pull/141) | Direção compatível, mas `PERFORMANCE.md` afirma que nenhuma variante chama rede antes da remoção | Separar alvo e estado atual, atualizar conflitos e executar verificadores |
| [#142](https://github.com/joao-pedro-gms/BrainOut/pull/142) | Guia conflita com main e cita versões anteriores | Reconciliar com a decisão local, catálogo e gate ktlint atual |
| [#137](https://github.com/joao-pedro-gms/BrainOut/pull/137) | Melhoria independente do teste de inventário; conflito impede integração | Atualizar o teste contra main e validar inclusão temporária de asset fora das categorias |

Esta revisão não equivale a aprovação formal de PR nem a merge. Nenhum dos
quatro foi integrado por esta organização.

## Pendências com correção já integrada

As issues [#196](https://github.com/joao-pedro-gms/BrainOut/issues/196)
(DEF-58) e [#197](https://github.com/joao-pedro-gms/BrainOut/issues/197)
(DEF-59) descrevem versões anteriores à correção `54f1d79`, integrada pelo
[PR #143](https://github.com/joao-pedro-gms/BrainOut/pull/143).

- `BusinessRuleException` herda de `DomainException`; a regressão é coberta
  por `DomainExceptionHierarchyTest`.
- `PasswordHasherImpl.verify` passa `parsed.iterations` ao PBKDF2 e valida a
  faixa suportada; `PasswordHasherImplTest` cobre custos abaixo/acima do
  padrão e valores inválidos.

Os dois itens foram encerrados administrativamente após a confirmação dos
testes em 16/10/2026, com evidências nas descrições e a label
`status: confirmed-fixed`. Isso não implica que os demais achados da
força-tarefa tenham sido corrigidos.

## Gates e limites da análise

Até a remoção efetiva de rede, preservar também o job `backend-integration`.
Manter ktlint, detekt, testes dev, quatro features explícitas, `core:ui`,
Kover >=60% e lint. JDK 21 está disponível nesta máquina.

Esta auditoria inspeciona código, testes e metadados pelo GH CLI. Não representa
execução em aparelho, teste de TalkBack, restauração entre instalações ou
aprovação de requisitos acadêmicos. A alteração prévia em `gradlew.bat`
pertence ao usuário e fica fora dos commits desta tarefa.

Validação local em 16/10/2026, JDK 21 e SDK instalado indicado via
`ANDROID_HOME`: `ktlintCheck`, `detekt`, `testDevDebugUnitTest`,
`:core:ui:testDebugUnitTest`, os quatro `:feature:*:testDebugUnitTest` e
`:core:domain:koverVerify :core:data:koverVerify` passaram. Nos oito módulos,
os XMLs das suítes dev/debug registraram **559 testes, zero falhas, erros ou
skips** (sem somar novamente variantes prod executadas pelo Kover).
`verify_tokens.py`, `verify_roadmap.py` (2.143 verificações), os links desta
decisão e `git diff --check` também passaram. Nenhum teste novo foi necessário
para esta alteração exclusivamente documental.
