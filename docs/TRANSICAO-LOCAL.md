# BrainOut — decisão de escopo e organização do repositório

Decisão aprovada pelo autor em **16/10/2026**. Base inspecionada:
`main` em `e49181b`. Organização: [#210](https://github.com/joao-pedro-gms/BrainOut/issues/210).

## Atualização de implementação — #212

A camada de rede, o stub e a fila foram removidos. Todos os módulos Android
usam variantes sem flavor. Room v5 migra a v4 removendo apenas `pending_ops`;
`LocalTransaction` preserva escritas atômicas. Bases que passaram pela antiga
2→3 também têm o default de `completed_at` normalizado sem perda de registros. O início do app cancela apenas
os trabalhos antigos de sync; lembretes continuam locais. Banners e fontes
de estado de sync foram retirados como consequência da remoção de seus
consumidores. A #213 alinha os textos de abertura, contas locais,
configurações e falhas do cálculo de feriados em pt/en, remove tags de
banners legados e preserva erros/retry locais. Evidências e limites de QA
estão em [LOCAL-02](qa/LOCAL-02.md); bugs de interface permanecem rastreados
nas issues originais.

## Atualização de implementação — #214

O backup local exportável existe e é manual: Configurações → Backup →
Exportar/Restaurar, via Storage Access Framework, sem serviço remoto. O
contrato do arquivo (formato versionado, cifragem AES-256-GCM com
PBKDF2, limite de 20 MiB, política de contas, reconciliação de lembretes)
está em [BACKUP](BACKUP.md); as evidências de QA em [LOCAL-03](qa/LOCAL-03.md).

Duas decisões merecem destaque:

- **Nenhum material do Keystore sai do aparelho.** O hash de senha é
  derivado com o pepper, que é por instalação — a senha do backup é a
  credencial de cifragem e vira a senha das contas que ainda não existirem
  no aparelho de destino. A conta que já existe mantém o hash local.
- **O backup automático do Android foi desligado** (#175 / DEF-37):
  `allowBackup=false`, `fullBackupContent=false` e `dataExtractionRules`
  excluindo tudo. O hash de senha e o id de sessão não circulam.

Como efeito colateral, `coreLibraryDesugaring` passou a ser habilitado em
`:core:data` (#201 / DEF-63) — o módulo usa `java.time` e
`java.util.Base64` com `minSdk 24`. Os 11 erros [NewApi] do módulo
desapareceram e o gate de lint voltou a ser rígido; os quatro módulos
`feature/*` habilitam o mesmo mecanismo, como exige o metadado do AAR do
AGP.

Feriados usam cálculo local pelas regras nacionais atuais, sem pontos
facultativos/feriados regionais. A referência de datas de 2026 é o
[calendário oficial do MGI](https://www.gov.br/gestao/pt-br/assuntos/noticias/2025/dezembro/confira-o-calendario-oficial-de-feriados-nacionais-e-pontos-facultativos-em-2026).

## Atualização de implementação — #215

A validação do upgrade local e os gates de CI foram fechadas. O job
`backend-integration` já não existe (a #212 trocou por
`local-integration`: build local + compilação da instrumentação +
verificação do manifesto e do APK de testes), e as suítes
instrumentadas rodaram de verdade no AVD Android 37 — sete testes de
dados e migrations e três de login, zero falhas.

O que mudou nesta issue:

- **Verificadores documentais na CI** (#205 / DEF-67):
  `docs/design/verify_tokens.py` e `docs/design/verify_roadmap.py`
  rodam no job `static-analysis` — paridade light/dark, contraste,
  links, ids de tarefa, ordenação e aciclicidade do plano deixam de
  depender de alguém lembrar.
- **Escopo de cobertura auditado** (#202 / DEF-64): o padrão de
  exclusão `*_Impl` do Kover foi estreitado para `*Dao_Impl*` e
  `*Database_Impl*` (as classes geradas pelo Room). A premissa do
  defeito — que `*_Impl` esconderia `ProjectRepositoryImpl` e
  `PasswordHasherImpl` — não se confirmava: o padrão exige
  sublinhado, e essas classes sempre estiveram no denominador. O
  verificador `.github/scripts/verify_coverage_scope.py` falha a CI
  se uma implementação de produção sumir do relatório ou se o padrão
  amplo voltar.
- **Jornada local ponta a ponta**:
  `core/data/src/test/.../backup/LocalBackupJourneyTest.kt` cobre
  CRUD → exportação → restauração em outra instalação → login com a
  senha do backup → sessão invalidada → lembrete reagendado, sem
  nenhuma chamada de rede.

Evidências e limites em [LOCAL-04](qa/LOCAL-04.md).

## Atualização de implementação — #208

O ledger `docs/DEFEITOS.md` foi reconciliado com o código: as 42 linhas
DEF-28 a DEF-70 (DEF-34 nunca existiu) entraram a partir das issues
`[DEF-NN]`, com a origem no vocabulário da seção 2 do próprio ledger
(`Revisão (<área>)` — todas as issues foram registradas por leitura de
código) e a severidade classificada pela seção 1 (`Crit.` ou `Men.`; a
Seção 1 não tem nível intermediário). As cinco linhas cuja correção já
estava integrada (DEF-37 e DEF-63 pelo PR #221; DEF-64 e DEF-67 pelo
PR #222; DEF-70 por este) estão como `CORRIGIDO` com o PR.

Os documentos que afirmavam coisas que o código não faz foram
corrigidos: `docs/DISPOSITIVOS.md` não diz mais `compileSdk/targetSdk =
35` nem fala em login e sincronização contra o backend (o app é local),
e o `README.md` descreve o backup manual como entregue e o backup
automático do Android como desligado. O stub e os documentos que o
descreviam sumiram com a remoção da rede (#212).

## Registro da auditoria anterior à implementação

O restante deste inventário descreve a base histórica `e49181b`. Suas afirmações
sobre código ainda com rede e flavors não descrevem a implementação posterior
à #212; preservam a rastreabilidade da decisão e da auditoria.

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
