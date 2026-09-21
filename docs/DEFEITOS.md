# Registro e Classificação de Defeitos — BrainOut (Ciclo 4 / E4.3)

> Documento operacional para registrar, classificar e acompanhar os
> defeitos identificados no BrainOut. Atende ao **item 6.3 do Documento
> Norteador Projeto Integrador ADS 2026-2** e é insumo direto para a
> **N2 (E5.1)**, para o **relatório de testes de E4.1** e para o
> **relatório de achados de E4.2**.
>
> Este arquivo é a fonte canônica do conjunto de defeitos do
> projeto; cada item entra com identificador, origem rastreável e
> classificação segundo a política do `docs/ROADMAP.md` (E4.3).
> Atualizações posteriores a esta entrega devem ser feitas por
> *commit dedicado* nesta mesma branch ou em PRs que referenciem um
> `DEF-NN` específico.

## 1. Política de classificação

A severidade reflete o **impacto sobre os fluxos principais**
(cobertura do N2 item 3: cadastro, login, criar projeto, criar
tarefa, concluir tarefa, sincronizar offline→online, receber
notificação) e define o **prazo de tratamento** antes da N2
(07 a 11/12/2026):

| Severidade     | Definição operacional                                                                                                                  | Tratamento antes da N2                                              |
|----------------|----------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------|
| **Bloqueante** | Impede a execução de um fluxo crítico. O app fecha (crash), perde dados do usuário ou torna-se inutilizável para o caso principal.     | **Obrigatório** corrigir antes da N2.                               |
| **Crítico**    | Degrada um fluxo crítico, mas existe contorno documentado e utilizável pelo usuário.                                                  | **Obrigatório** corrigir antes da N2 (ou registrar contorno homologado). |
| **Menor**      | Cosmético, textual, animação fora do padrão, mensagem ambígua ou *edge case* que não bloqueia o fluxo.                                  | Corrigir se couber no cronograma. Caso não corrigido, exige **justificativa explícita** nesta tabela. |

A contagem de defeitos corrigidos vs. justificados é o critério de
pronto do E4.3 e é referenciada no E5.1 (relatório técnico).

## 2. Estrutura do registro

Cada defeito ocupa **uma linha** da tabela da Seção 5 com os
seguintes campos (todos obrigatórios, exceto `PR/Commit da correção`
e `Justificativa de não-correção`, que ficam vazios conforme o
status):

| Coluna                   | Conteúdo                                                                                                                                                                                                       |
|--------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **ID**                   | `DEF-NN` (número sequencial com zero à esquerda, a partir de `DEF-01`).                                                                                                                                        |
| **Origem**               | Rastreabilidade: `TF-NN` (roteiro funcional, `docs/ROTEIRO-TESTES.md`), `Sessão Pn-Tm` (sessão de usabilidade `P1`–`P5`, tarefa `T1`/`T2`/`T3`), `CI run <id>` (falha de pipeline), `QA manual`, `Revisão` ou `Bug externo`. |
| **Descrição**            | Sintoma observado em uma frase, com o passo para reproduzir quando aplicável.                                                                                                                                  |
| **Severidade**           | `Bloq.` / `Crit.` / `Men.` (forma curta adotada em E4.1).                                                                                                                                                      |
| **Frequência**           | Quantas vezes foi observado até o momento do registro: `1/1`, `2/3`, `100%`, `intermitente`, etc.                                                                                                              |
| **Correção**             | PR (`#NN` com link) ou commit (`<sha7>`) que fecha o defeito, ou literal `não corrigido` quando ainda aberto.                                                                                                 |
| **Justificativa de não-correção** | Texto curto obrigatório quando a coluna `Correção` for `não corrigido`, citando contorno ou motivo.                                                                                                |

A coluna `Correção` admite três valores: (a) link de PR mergeado;
(b) SHA curto de commit mergeado em `main`; (c) literal
`não corrigido`. Para PRs ainda abertos, o registro fica como
`não corrigido` com a justificativa apontando o card da filha em
que a correção está em curso.

## 3. Política e fluxo de triagem

A triagem é responsabilidade do autor (único responsável pelos
marcos do projeto, conforme `docs/ROADMAP.md`):

1. **Detecção** — qualquer defeito identificado durante execução
   dos casos `TF-NN`, nas sessões com os 5 colegas ADS (E4.2), na
   validação dos *pipelines* de CI, ou em revisão de código, é
   **imediatamente** registrado nesta tabela, mesmo que de forma
   provisória. *Não acumular*: defeitos não registrados não
   contam para o critério de pronto do E4.3.
2. **Classificação** — feita pelo autor no momento do registro
   (mesmo dia). Casos duvidosos são provisionados como `Bloq.` até
   desambiguação; a reclassificação para `Crit.` ou `Men.` requer
   justificativa quando implicar mudança de prazo.
3. **Correção ou justificativa** — defeitos `Bloq.` e `Crit.`
   recebem PR de correção até o **congelamento E4.8 (27/11/2026)**;
   defeitos `Men.` são corrigidos se houver capacidade ou
   formalmente justificados.
4. **Fechamento** — após merge do PR, atualizar a coluna `Correção`
   com o link do PR ou SHA e remover a justificativa. Sem
   `Correção` preenchida (≠ `não corrigido`), nem a entrega N2 é
   considerada completa.

### 3.1. Procedência dos defeitos (fontes aceitas)

Para evitar a invenção de defeitos, **só são registrados nesta
tabela itens cuja origem seja verificável em um artefato
versionado**:

| Fonte                              | Onde verificar                                              |
|------------------------------------|-------------------------------------------------------------|
| Falha de pipeline CI               | `gh run view <id>` ou `docs/qa/run-log-t_*.md`              |
| Issue aberta no repositório        | `gh issue list` (não aplicável neste ciclo: nenhuma issue aberta no `joao-pedro-gms/BrainOut` antes desta entrega) |
| Achado de revisão de código        | Histórico do PR ou comentário de revisão                    |
| Achado em artefato já documentado  | `docs/CI-CD.md`, `docs/ARQUITETURA.md`, etc.                |
| Execução de caso de teste `TF-NN`  | `docs/ROTEIRO-TESTES.md` (coluna *Resultado observado*)     |
| Sessão de usabilidade `Pn-Tm`      | `docs/USABILIDADE.md` (Parte 2 — a preencher após 17/11/2026) |

Casos `TF-NN` ainda não executados (coluna *Resultado observado*
em branco) e sessões de usabilidade ainda não realizadas **não
geram defeitos preventivos** — defeitos só entram após detecção
real.

## 4. Regra de congelamento (E4.8 — 27/11/2026)

A partir de **27/11/2026**, a aceitação de novos defeitos passa a
ser restrita:

- Apenas defeitos classificados como `Bloq.` ou `Crit.` podem ser
  registrados e entram na fila de correção do E4.8.
- Defeitos `Men.` identificados após esta data são **automaticamente
  movidos para o backlog pós-N2** (a registrar no relatório
  técnico E5.1, Apêndice A.2) e devem ser marcados como
  `não corrigido` com justificativa `Backlog pós-N2`.
- A regra acima também vale para reclassificações: um defeito
  registrado como `Men.` antes do congelamento e reclassificado
  para `Bloq.` ou `Crit.` após essa data é tratado como se
  tivesse sido detectado dentro do prazo.

## 5. Tabela de defeitos

A numeração é contínua: linhas novas devem receber o próximo
`DEF-NN` disponível e manter a ordem cronológica de detecção
(do mais antigo para o mais recente).

| ID     | Origem              | Descrição                                                                                                                                                                                                  | Sev.  | Frequência | Correção                                                                                     | Justificativa de não-correção |
|--------|---------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------|------------|----------------------------------------------------------------------------------------------|--------------------------------|
| DEF-01 | CI run 35597593408  | `validateSigningRelease` falha em `Release APK/AAB`: keystore é gravado na raiz do runner (`keystore.jks`), mas `file(brainoutStoreFile!!)` em `:app/build.gradle.kts` resolve o path contra o diretório do módulo `app/`. | Crit. | 1/1        | PR [#43](https://github.com/joao-pedro-gms/BrainOut/pull/43) — decodifica keystore em `${{ runner.temp }}/brainout-release.jks` (path absoluto). | —                              |
| DEF-02 | CI run 35598981118  | `signReleaseBundle` falha com `Get Key failed: Given final block not properly padded`: secret `BRAINOUT_KEY_PASSWORD` difere da senha que abre a chave dentro do PKCS12 (em PKCS12 a senha da chave é a senha do keystore). | Crit. | 1/1        | não corrigido                                                                                 | Contorno documentado em `docs/CI-CD.md` (PR [#47](https://github.com/joao-pedro-gms/BrainOut/pull/47)): `keytool -genkeypair -storetype PKCS12` deve usar a MESMA senha em `-storepass` e `-keypass`, e ambos os secrets cadastrados com esse valor. Não há correção de código a fazer — a falha só se repete se o operador gerar o keystore com senhas divergentes. |
| DEF-03 | QA manual (run-log) | CI run 35599507321 (`v0.3.1-ciclo3`, 2ª tentativa) bem-sucedido após DEF-01/02; documentado em `docs/qa/run-log-t_333ab600.md` como evidência da trilha de release (`brainout-release-aab-v0.3.1-ciclo3`, 4,2 MB). | Men.  | 1/1        | PR [#48](https://github.com/joao-pedro-gms/BrainOut/pull/48) — registro do run-log.           | Não é defeito de produto: marco de *release* válido (`v0.3.1-ciclo3`); registrado para rastreabilidade da cadeia DEF-01 → DEF-02 → DEF-03 e para evidência de que o contorno de DEF-02 produziu artefato assinado. |
| DEF-04 | QA manual (wireframes) | Diagrama do wireframe low-fi (`docs/wireframes/lo-fi.html`) tinha setas entre fileiras sobrepostas à anotação "Voltar para Home" da tela 4, confundindo o fluxo de retorno pós-logout (fileira 2 → fileira 1). | Men.  | 1/1        | commit [`f528ff1`](https://github.com/joao-pedro-gms/BrainOut/commit/f528ff1) — refatoração profunda do layout das fileiras + curva tracejada única entre fileiras 2 e 1. | —                              |
| DEF-05 | QA manual (CI-CD.md) | Comando `keytool -genkeypair` em `docs/CI-CD.md` (seção "Como gerar o keystore localmente") estava sem flag `-keyalg` explícita; chave poderia ser gerada com algoritmo fraco dependendo do JDK.                    | Men.  | 1/1        | PR [#47](https://github.com/joao-pedro-gms/BrainOut/pull/47) — adicionadas flags `-keyalg RSA -keysize 2048`. | —                              |
| DEF-06 | QA manual (i18n)    | 8 chaves `deadline_*` introduzidas pelo E3.6 em `app/src/main/res/values/strings.xml` (PT) não tinham par em `app/src/main/res/values-en/strings.xml`: `deadline_channel_name`, `deadline_channel_description`, `deadline_notification_title`, `deadline_notification_body`, `deadline_action_complete`, `deadline_unknown_project`, `deadline_permission_denied_toast`, `deadline_permission_rationale_title`. Com a locale `en` ativa, as notificações de prazo aparecem em PT. A regra `lint { abortOnError = true; error += "MissingTranslation" }` no `:app` (introduzida pelo PR #49) não pegou porque a pipeline CI roda `ktlint + detekt` (não `lintDebug`). | Crit. | 100% (todas as execuções com locale `en`) | não corrigido | Correção depende do card de notificação atualmente em curso (`chore/toolchain-upgrade-brainout`, commit [`9d48c2d`](https://github.com/joao-pedro-gms/BrainOut/commit/9d48c2d) que adiciona as 8 chaves em `values-en`). Aguarda PR de follow-up específico para a faixa E3.6 ser mergeado em `main` (atualmente só no branch de toolchain). Enquanto isso, fluxo "receber notificação de prazo" continua poluído em inglês. |
| DEF-07 | QA manual (holidays) | Em `main`, o módulo de feriados (E3.5) ainda não havia recebido PR de polimento (assinatura de testes, `@Suppress("NewApi")` para `java.time.*` em `minSdk 24`, supressões justificadas de detekt para `TooManyFunctions`/`LongParameterList`/`LongMethod`, e a adição das traduções EN para `deadline_holidays_unavailable` e `deadline_not_business_day`). detekt e `lintDebug` falhavam em `:core:data` e `:feature:projects`. | Men.  | 100% (`./gradlew detekt` e `lintDebug`) | não corrigido | Correção entregue no commit [`e828544`](https://github.com/joao-pedro-gms/BrainOut/commit/e828544) dentro do PR [#51](https://github.com/joao-pedro-gms/BrainOut/pull/51) (`feat/external-holiday-api`), ainda **aberto** no momento desta entrega. Não se justifica merge do PR #51 só para fechar este defeito (o PR tem escopo maior — feature de feriados). A correção será puxada junto quando o PR #51 for mergeado em `main`. |

> **Observação sobre o preenchimento atual.** A tabela acima
> contém apenas defeitos cuja origem é rastreável a um artefato
> versionado (CI run, QA manual, run-log, PR já mergeado). Não
> foram inventados defeitos a partir de cenários não executados:
> casos de teste ainda não rodados (`docs/ROTEIRO-TESTES.md`,
> coluna *Resultado observado* em branco) e sessões de
> usabilidade ainda não realizadas (semana de 17/11/2026,
> `docs/USABILIDADE.md`) **não geram defeitos preventivos** —
> defeitos só entram após detecção real, conforme a Seção 3.
>
> As linhas reservadas `DEF-08` em diante ficam em branco até que
> novas detecções sejam feitas (formulário `QA manual`, falha de
> CI, resultado preenchido em `TF-NN`, achado da sessão `Pn-Tm`).

## 6. Apêndice A — Referências cruzadas

- Política de severidade e marco E4.3: `docs/ROADMAP.md` (bloco do
  Ciclo 4, marcos).
- Casos funcionais que alimentam a coluna *Origem* (`TF-NN`):
  `docs/ROTEIRO-TESTES.md` (TF-01..TF-14, E4.1).
- Sessões que alimentam a coluna *Origem* (`Sessão Pn-Tm`):
  `docs/USABILIDADE.md` (Parte 2 — preenchida após a execução
  presencial da semana de 17/11).
- Critério de pronto da N2 (E5.1) referencia esta tabela no
  Apêndice A.2 do relatório técnico.
- Congelamento e regra de fila pós-27/11: E4.8 no
  `docs/ROADMAP.md`.
- CI-CD e trilha de release assinada: `docs/CI-CD.md`,
  `docs/qa/run-log-t_333ab600.md`.

---

*Documento elaborado por **João Pedro G M Silva** — ADS, PUC Goiás
(mat. 20251012000740), como entrega do marco **E4.3** do Ciclo 4
do Projeto Integrador.*