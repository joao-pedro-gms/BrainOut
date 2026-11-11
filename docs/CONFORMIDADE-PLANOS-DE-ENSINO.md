# BrainOut × Planos de Ensino — Auditoria de conformidade

**Data:** 2026-11-11 · **Base:** planos de ensino ADS1251, ADS1252, ADS1253, ADS1254 e CMP1024 (PUC Goiás ADS 2026/2) · **Escopo auditado:** árvore `main` do repositório.

Este documento audita, disciplina a disciplina, se os conceitos trabalhados em sala têm aplicação real no BrainOut. Para cada conceito há **status** (`✔` implementado, `◐` parcial, `✘` ausente, `N/A` não aplicável ao contexto do projeto) e a **evidência** (`arquivo:seção`). No fim de cada disciplina, os gaps viram **plano de implementação** priorizado.

## Resumo executivo

| Disciplina | Cobertura | Veredito |
|---|---|---|
| ADS1252 — Desenvolvimento de Aplicações para Dispositivos Móveis | ~90% | Fortemente aplicada — o app inteiro é o objeto da disciplina |
| ADS1253 — Programação Orientada a Objetos com Banco de Dados | ~85% | Aplicada; falta modelagem UML versionada |
| ADS1251 — Ferramentas Visuais de Desenvolvimento de Software | ~60% | Parcial: IHC e prototipação fortes; núcleo VS/.NET não aplicável |
| ADS1254 — Gestão de Projetos de Software (FASE) | ~70% | Processo vivo real; faltam artefatos formais pedidos na matéria |
| CMP1024 — Governança em Tecnologia da Informação | ~45% | Práticas presentes, mas sem o alinhamento documentado a frameworks |

---

## 1. ADS1252 — Desenvolvimento de Aplicações para Dispositivos Móveis (70h)

### 1.1 Matriz de conformidade

| Conceito do plano | Status | Evidência no BrainOut |
|---|---|---|
| Ecossistema e arquitetura da plataforma Android | ✔ | AGP 9.4.1 + KGP 2.4.21 + Gradle 9.8.1; `compileSdk/targetSdk 37`, `minSdk 24`; `coreLibraryDesugaring` para `java.time` |
| Ferramentas e ambiente (Android Studio, emuladores) | ✔ | `local.properties` + JBR/JDK 21 documentados; testes instrumentados compilados para emulador (`:core:data:assembleDebugAndroidTest`, `:feature:auth:assembleDebugAndroidTest`) |
| Activities e ciclo de vida | ✔ | `app/src/main/kotlin/.../MainActivity.kt` (Compose host, resolução de start-destination, `POST_NOTIFICATIONS` em Tiramisu+) |
| Material Design / componentes visuais | ✔ | Compose Material 3 + tema Neo em `:core:ui` (`NeoColor`, `NeoTokens`, `NeoTypography`, `NeoShapes`, componentes `Neo*`) |
| Listas e grids | ✔ | `LazyColumn`/Compose equivalente ao `RecyclerView` do plano; `HomeListingPipeline` com busca debounceada |
| Navegação entre telas | ✔ | `BrainOutNavHost.kt` + `BrainOutRoutes.kt` (fonte única de rotas) + `TasksRoutes.kt` |
| Arquitetura de aplicações móveis | ✔ | MVVM + camadas: `:app → :feature:* → :core:{ui,data} → :core:domain`; ViewModel + `StateFlow` + use cases + repositórios |
| Persistência local de informações | ✔ | Room v6 (schemas exportados, migrations 1→6), DataStore Preferences, backup local cifrado (`docs/BACKUP.md`) |
| Arquitetura Orientada a Serviços / REST / SOAP / APIs externas | ✘ | **Removida por decisão de produto** (#212, `docs/TRANSICAO-LOCAL.md`): produto 100% local. Ver plano GAP-1 |
| Testes, depuração e distribuição | ◐ | Robolectric + Turbine + Compose UI + `MigrationTest`; release assinado via CI (`release-apk.yml`, AAB/APK); **falta publicação em loja** (ver GAP-2) |

### 1.2 Gaps e plano de implementação

**GAP-1 — Integração opcional pós-transição-local (conceitos SOA/REST).**
A disciplina exige comunicação cliente-servidor; o produto é local por direção aprovada. Em vez de reintroduzir rede no app, criar um **PBL de integração opcional e desacoplada**: módulo `:feature:share` (ou similar) que exporta/importa o backup via `FileProvider`/`SAF` (já é fronteira de I/O) e, como exercício de REST, um script utilitário **fora do app** (Python stdlib) que valida o JSON do backup contra o contrato de `docs/BACKUP.md`. Assim o conceito de contrato de API/serialização é exercitado sem violar a direção local.
*Entregáveis:* documento de contrato do formato (já existe em `docs/BACKUP.md`), script validador em `.github/scripts/` com teste no CI. *Esforço:* 1 sessão.

**GAP-2 — Plano de distribuição (publicação).**
*Etapas:* (1) gerar AAB assinado (`release-apk.yml` já produz); (2) criar app no Google Play Console (conta de estudante/testador); (3) publicar em *internal testing track* com a lista de e-mails da turma; (4) registrar prints do rollout em `docs/qa/`. *Esforço:* 2–3 sessões + tempo de revisão do Google (até 7 dias).

**GAP-3 — Registro formal do equivalente Compose.**
O plano cita `RecyclerView`/`Activity` XML; o projeto usa LazyColumn/Compose. Registrar a tabela de equivalências (Activity↔NavHost, RecyclerView↔LazyColumn, SharedPreferences↔DataStore, SQLiteOpenHelper↔Room) em apêndice deste documento para evidência de aprendizado. *Esforço:* 30 min.

---

## 2. ADS1253 — Programação Orientada a Objetos com Banco de Dados (80h)

### 2.1 Matriz de conformidade

| Conceito do plano | Status | Evidência |
|---|---|---|
| Paradigma OO: classes, atributos, métodos, herança, polimorfismo, encapsulamento | ✔ | Kotlin no projeto inteiro; hierarquia de exceções `DomainException` (`DomainExceptionHierarchyTest`); sealed/enum (`TaskPriority`, `AuthErrorType`); data classes imutáveis de UI state |
| UML como ferramenta de modelagem | ✘ | Não há diagramas UML versionados (só diagrama de módulos textual em `docs/ARQUITETURA.md` §2). Ver GAP-4 |
| Diagramas de classe UML | ✘ | Idem — GAP-4 |
| Persistência OO ↔ relacional (JDBC no plano; DAO/Room no Android) | ✔ | `*Dao` (Room), `LocalTransaction`, repositórios (`core/data/.../repository/`); o papel do JDBC é exercido pelo Room como camada de mapeamento OO-relacional idiomática da plataforma |
| Conexão, CRUD e manipulação de dados | ✔ | DAOs com `@Query`/`@Insert`/`@Update`; cascatas `@Transaction` (`cascadeCompleteTask`); reparo `repairCompletedProject` |
| Integração de projeto prático OO + BD | ✔ | O BrainOut é exatamente o projeto integrador: modelos de domínio + Room + transações atômicas |
| Testes de programas orientados a objetos | ✔ | `Fake*Dao` (sem MockK para DAO), `MigrationTest` (cadeia 1→6), testes de casos de uso no `:core:domain` |

### 2.2 Gaps e plano de implementação

**GAP-4 — Modelagem UML versionada.**
*Etapas:* (1) criar `docs/MODELAGEM.md` com: **classDiagram** (mermaid) das entidades de `:core:domain` (`User`, `Project`, `Task`, `Tag` + exceções); **ER/DER** das tabelas Room a partir de `core/data/schemas/`; diagrama de sequência do fluxo "criar tarefa com lembrete" (Screen → ViewModel → UseCase → Repository → LocalTransaction → DAO + WorkManager); (2) adicionar verificador leve no CI (`.github/scripts/verify_modelagem.py`, stdlib) que garante que os blocos mermaid existem e referenciam as classes reais (grep em `core/domain/src/main/kotlin`); (3) linkar de `docs/ARQUITETURA.md` §2. *Esforço:* 1–2 sessões.

---

## 3. ADS1251 — Ferramentas Visuais de Desenvolvimento de Software (60h)

> O núcleo da disciplina é Visual Studio/.NET/C#/low-code — **não aplicável** a um app Android nativo. Os conceitos transferíveis são: ambiente visual de desenvolvimento, prototipação, design de interfaces (IHC) e documentação visual. A equivalência é registrada, não copiada.

### 3.1 Matriz de conformidade

| Conceito do plano | Status | Evidência |
|---|---|---|
| Ambiente visual de desenvolvimento | ✔ | Android Studio (equivalente direto do Visual Studio para a plataforma alvo): Layout Inspector, previews Compose, emulador |
| Formulários visuais e designer de UI | ✔ | Jetpack Compose (declarativo — evolução do designer visual); design system Neo em `:core:ui` |
| Prototipação (wireframes lo-fi/hi-fi) | ✔ | `docs/wireframes/` (9 telas HTML lo-fi) + deploy automático em GitHub Pages (`pages.yml`) |
| IHC — avaliação com usuários | ✔ | `docs/USABILIDADE.md`, `USABILIDADE-SUS.md` (questionário SUS), `USABILIDADE-tcle-TCLE.md` (termo de consentimento), `USABILIDADE-ficha-observacao.md`, capturas em `docs/qa/` |
| Acessibilidade | ✔ | `docs/ACESSIBILIDADE.md` + seção 7 de `docs/ARQUITETURA.md` |
| Design tokens / identidade visual | ✔ | `docs/design/tokens.json` + `verify_tokens.py` no CI + 5 fontes locais e 28 drawables `neo_*` |
| UML/CASE como ferramenta visual | ✘ | Compartilhado com ADS1253 — GAP-4 |
| Low-code | N/A | Low-code (Power Apps/AppSheet) não se aplica; o análogo moderno adotado é UI declarativa em código (Compose) |

### 3.2 Gaps e plano de implementação

**GAP-5 — Justificativa de equivalência VS↔Android Studio.**
Adicionar seção neste documento (ou em `docs/ARQUITETURA.md` §3) mapeando item a item o conteúdo programático da disciplina à stack real escolhida, para evidência acadêmica. *Esforço:* 30 min, junto com GAP-3.

---

## 4. ADS1254 — Gestão de Projetos de Software — FASE (60h)

### 4.1 Matriz de conformidade

| Conceito do plano | Status | Evidência |
|---|---|---|
| Escopo — definição e controle | ✔ | `docs/ROADMAP.md`: requisitos R1–R14, mapeamento R1–R14, ciclos com "Saída do Ciclo N" (escopo validado por ciclo) |
| Tempo — cronograma e marcos | ◐ | Marcos datados por ciclo no ROADMAP; **sem Gantt** (ver GAP-7) |
| Qualidade | ✔ | Kover 60% (`:core:domain`/`:core:data`), ktlint `ignoreFailures=false`, detekt `maxIssues=0`, Android lint com gate de i18n, revisão via PR + CODEOWNERS + PR template |
| Riscos | ✔ | `docs/ROADMAP.md` §"Riscos e mitigações" — tabela com probabilidade, impacto e mitigação revisada (backend removido, keystore, acessibilidade, dependabot) |
| Comunicação | ✔ | ATAs em `docs/ATAS/`, Conventional Commits com `Refs:`, templates de issue (`user-story.md`, `bug.md`, `config.md`) |
| Metodologias ágeis (Scrum/XP) | ✔ | Ciclos iterativos com entregas incrementais; práticas XP: CI contínuo, TDD nos use cases, refactorings registrados em `docs/DEFEITOS.md`, ADRs (`docs/adr/`) |
| Aquisições e custos | N/A | Projeto solo sem aquisições; custo zero (toolchain open source) — registrar justificativa |
| Declaração de Objetivos do Projeto | ✘ | Parcialmente espalhada, sem documento formal — GAP-6 |
| Análise SWOT | ✘ | Ausente — GAP-6 |
| EAP/WBS (Estrutura Analítica) | ✘ | Ausente — GAP-7 |
| Gráfico de Gantt | ✘ | Ausente — GAP-7 |
| Diagrama de Ishikawa | ✘ | Ausente — GAP-8 |
| Quadro Kanban (GitHub Projects) | ✘ | ROADMAP §Acompanhamento diz "link a adicionar" — GAP-9 |

### 4.2 Gaps e plano de implementação

**GAP-6 — Declaração de Objetivos + SWOT (`docs/GESTAO/01-DECLARACAO-E-SWOT.md`).**
Documento de 1–2 páginas: problema, objetivo mensurável, justificativa, stakeholders (autor, orientador, turma como usuários de teste), premissas/restricões — seguido da matriz SWOT do projeto (forças: stack moderna e cobertura de testes; fraquezas: equipe solo e conhecimento em construção; oportunidades: portfólio, Play interno; ameaças: prazo semestral, revisão de loja). *Esforço:* 1 sessão.

**GAP-7 — EAP + Gantt (`docs/GESTAO/02-EAP-E-CRONOGRAMA.md`).**
EAP nível 2–3 (entregável → pacotes: app, documentação, QA, gestão) e Gantt em mermaid (`gantt`) retroalimentado pelos ciclos já executados do ROADMAP — retrospectivo para os ciclos 1–4 e prospectivo para o backlog aberto. *Esforço:* 1 sessão.

**GAP-8 — Ishikawa (`docs/GESTAO/03-ISHIBKAWA-QUALIDADE.md`).**
Diagrama de espinha de peixe (mermaid `mindmap` ou imagem) com o efeito "qualidade percebida do app" e causas nas 6M adaptadas (método, máquina/ferramenta, medição — cobertura/lint, mão de obra solo, material — requisitos, ambiente — prazos). Conectar aos gates reais (Kover/ktlint/detekt/lint). *Esforço:* 1 sessão.

**GAP-9 — GitHub Projects.**
Criar quadro "BrainOut" no GitHub Projects com colunas Backlog/Em progresso/Revisão/Concluído, importar o backlog aberto de `docs/TRANSICAO-LOCAL.md` e `docs/DEFEITOS.md`, e atualizar o link no ROADMAP §Acompanhamento. *Esforço:* 30 min.

---

## 5. CMP1024 — Governança em Tecnologia da Informação (40h)

### 5.1 Matriz de conformidade

| Conceito do plano | Status | Evidência |
|---|---|---|
| Governança corporativa × TI (papéis, decisão) | ◐ | Prática real de gestão de mudanças (branch → PR → CI → merge commit → delete branch), mas sem documento de alinhamento a framework — GAP-10 |
| Frameworks (COBIT, ITIL, ISO/IEC 38500) | ✘ | Nunca citados no repo — GAP-10 |
| Segurança da informação | ✔ | `docs/ARQUITETURA.md` §6: PBKDF2-HMAC-SHA256 120k iterações + pepper em `EncryptedSharedPreferences`/`AndroidKeystore`; backup AES-256-GCM (`docs/BACKUP.md`); sem tokens; credenciais via env var; permissões justificadas |
| Auditoria e melhoria contínua | ✔ | `docs/AUDITORIA-2026-09-24.md` (P0/P1/P2 com reverificação 2026-10-08 evidenciada por `arquivo:linha`); `docs/DEFEITOS.md`; CodeQL no CI (`codeql.yml`) |
| Métricas e indicadores | ✔ | Cobertura Kover agregada (`koverMergedHtmlReport`), gates de qualidade no CI, Dependabot com groups |
| LGPD / privacidade | ◐ | TCLE existe para a pesquisa de usabilidade; **não há política de privacidade do app nem classificação de dados** — GAP-11 |
| Gestão de serviços (ITIL) | N/A | Sem operação de TI; o análogo é o suporte via issues — mapear em GAP-10 |
| Continuidade e backup | ✔ | `docs/BACKUP.md`: formato versionado, validação, restauração preservando credenciais; backup automático do Android (§7) |

### 5.2 Gaps e plano de implementação

**GAP-10 — Documento de alinhamento de governança (`docs/GOVERNANCA.md`).**
Mapear as práticas já vivas do repositório a referências da disciplina: ISO/IEC 38500 (princípios de governança — responsabilidade, estratégia, desempenho, conformidade, comportamento humano) e COBIT 2019 (processos como BAI06/Gestão de mudanças ↔ PR+CI; BAI03/Gestão de configuração ↔ versionamento e tags; EDM/MEA ↔ auditoria P0/P1/P2). Incluir classificação de dados do app (público: código; interno: docs; pessoal/sensível: hashes de senha, dados de tarefas no banco e no backup cifrado) e o análogo ITIL de suporte (issue `bug.md` = incidente, `user-story.md` = solicitação de mudança). *Esforço:* 1–2 sessões.

**GAP-11 — Privacidade LGPD no app.**
(1) Texto curto de política de privacidade local (dados nunca saem do dispositivo; backup só sai quando o usuário exporta; exclusão de conta apaga tudo) em `values/strings.xml` + `values-en/`, exibido em Settings; (2) nota de retenção em `docs/BACKUP.md` §Política. Respeita o gate `MissingTranslation`. *Esforço:* 1 sessão (+ testes de ViewModel da tela).

---

## 6. Backlog consolidado (ordem sugerida)

| # | Entrega | Disciplina | Esforço estimado |
|---|---|---|---|
| GAP-3+5 | Tabelas de equivalência (Compose/Android Studio) neste doc | ADS1252/ADS1251 | 1h |
| GAP-4 | `docs/MODELAGEM.md` (UML/ER/sequência) + verificador CI | ADS1253/ADS1251 | 1–2 sessões |
| GAP-6 | Declaração de Objetivos + SWOT | ADS1254 | 1 sessão |
| GAP-7 | EAP + Gantt | ADS1254 | 1 sessão |
| GAP-8 | Ishikawa da qualidade | ADS1254 | 1 sessão |
| GAP-9 | GitHub Projects + link no ROADMAP | ADS1254 | 30 min |
| GAP-10 | `docs/GOVERNANCA.md` (38500/COBIT + classificação de dados) | CMP1024 | 1–2 sessões |
| GAP-11 | Política de privacidade LGPD no app | CMP1024 | 1 sessão |
| GAP-1 | Validador de backup + contrato (exercício REST/PBL) | ADS1252 | 1 sessão |
| GAP-2 | Publicação em internal testing do Play | ADS1252 | 2–3 sessões + revisão Google |

## 7. Notas metodológicas

- A auditoria considerou a **direção de produto local aprovada** (`docs/TRANSICAO-LOCAL.md`): conceitos de rede/backend foram avaliados como "removidos por decisão", não como negligência, e os gaps correspondentes foram convertidos em alternativas compatíveis (GAP-1).
- Evidências históricas do ROADMAP que citam backend/sync estão marcadas como históricas na própria documentação do projeto; a matriz acima usa sempre o estado atual de `main`.
- Cada GAP é entregável independente (branch própria, Conventional Commits, `Refs:` correspondente ao requisito do ROADMAP quando houver).
