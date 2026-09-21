# BrainOut — Roadmap de implementação

> Aplicativo Android nativo (Kotlin + Jetpack Compose) para gestão de projetos
> e tarefas. Persistência local com Room, sincronização com serviço de
> retaguarda, executável em dispositivo físico.
>
> Documento norteador: [`Documentos/Documento Norteador Projeto Integrador ADS 2026-2.pdf`](../Documentos/Documento%20Norteador%20Projeto%20Integrador%20ADS%202026-2.pdf).

Este roadmap cobre os quatro ciclos previstos no cronograma do documento
norteador (Seção 7) em uma trilha de execução **individual** conduzida
pelo autor. Cada item lista o requisito (R1–R14) que atende, a estimativa
em pontos de história (PH) e o critério de pronto. As estimativas usam
a escala 1, 2, 3, 5, 8, 13.

| Marco | Responsável |
|-------|-------------|
| Todos os marcos do Ciclo 1 ao Encerramento | Autor (participação individual) |

> O Fator de Participação Individual (FPI), previsto no item 8.4 do
> documento norteador, permanece em 1,00 enquanto a contribuição
> técnica for contínua (ver `docs/CONTRIBUTING.md`).

## Visão geral

```
Ciclo 1 (Sem 7-8)  → Telas + autenticação + persistência local  → Entrega N1
Ciclo 2 (Sem 11-12) → CRUDs + regras de negócio + visão consolidada
Ciclo 3 (Sem 13-14) → Backend + sincronização + recurso nativo  → Checkpoint 2
Ciclo 4 (Sem 16-17) → Refinamento, acessibilidade, testes       → Congelamento
Encerramento (Sem 18-19) → Documentação final + apresentação    → Entrega N2
```

---

## Ciclo 1 — Esqueleto navegável, autenticação e primeiro módulo

**Período:** 14/09 a 25/09/2026 (Semanas 7 e 8)
**Entrega associada:** N1 (28/09 a 02/10)

### Marcos

- [ ] **E1.1** — Configurar o projeto Gradle com módulos `:app`,
      `:core:data`, `:core:domain`, `:core:ui`, `:feature:auth`,
      `:feature:projects`. Plugin Compose habilitado, KSP configurado.
      *Critério:* `./gradlew tasks` resolve todos os módulos.
      *Estimativa:* 5 PH.

- [ ] **E1.2** — Definir o pacote raiz `pucgo.joaopedrogmsilva.brainout`
      e o pacote de teste `pucgo.joaopedrogmsilva.brainout.testing`.
      *Critério:* package declarations consistentes em todos os módulos.
      *Estimativa:* 1 PH.

- [ ] **E1.3** — Implementar navegação entre 6 telas com
      `androidx.navigation:navigation-compose`. Telas mínimas do esqueleto:
      Splash, Login, Register, Home (lista de projetos), Detalhe do
      projeto, Configurações.
      *Critério:* APK debug roda, fluxo Splash → Login → Home → Detalhe
      funciona sem travar. *Atende R1.* *Estimativa:* 8 PH.

- [ ] **E1.4** — Modelagem de dados inicial em `:core:domain`.
      Entidades `User`, `Project`, `Task`. Enum `UserRole { Owner, Member }`.
      *Critério:* classes Kotlin imutáveis, com testes unitários de
      invariantes (ex.: título não vazio, prioridade no intervalo 0..4).
      *Estimativa:* 3 PH.

- [ ] **E1.5** — Configurar Room com KSP no módulo `:core:data`. DAO para
      `User` com operações básicas. Migração inicial versionada.
      *Critério:* `UserDaoTest` verde usando Robolectric. *Atende R5.*
      *Estimativa:* 5 PH.

- [ ] **E1.6** — Tela de cadastro/login com formulário validado, usando
      `viewModel()`, `StateFlow`, e mensagens de erro inline.
      *Critério:* erros exibidos por `TextInputLayout`/`Supporting text`;
      rotação preserva o estado (sem `android:configChanges`).
      *Atende R2, R10.* *Estimativa:* 5 PH.

- [ ] **E1.7** — Dois perfis com permissões distintas. `Owner` pode criar
      projetos; `Member` somente visualiza. Diferenciação visível na Home.
      *Critério:* testes unitários verificam a matriz de permissões.
      *Atende R2.* *Estimativa:* 3 PH.

- [ ] **E1.8** — Integração mínima com Room para o módulo de autenticação.
      *Critério:* usuário cadastrado persiste em SQLite local; login
      funciona após reiniciar o app. *Atende R5.* *Estimativa:* 3 PH.

- [ ] **E1.9** — Habilitar lint, ktlint, detekt e testeDebugUnitTest no
      pipeline CI. O CI deve estar verde para esta entrega.
      *Critério:* o run `CI` no GitHub Actions passa em todos os jobs.
      *Estimativa:* 3 PH.

- [ ] **E1.10** — Atualizar `README.md` com instruções de build e execução
      (mesmo que ainda incompletas para a N2). *Critério:* novo
      contribuidor consegue clonar e abrir o projeto no Android Studio.
      *Atende R13.* *Estimativa:* 1 PH.

### Saída do Ciclo 1

APK debug instalável, com esqueleto de navegação, autenticação funcional
contra Room local, dois perfis reconhecidos, CI verde. Backlog atualizado,
histórico Git versionado continuamente.

---

## Ciclo 2 — Operações de manutenção, regras de negócio, listagens

**Período:** 13/10 a 23/10/2026 (Semanas 11 e 12)

### Marcos

- [x] **E2.1** — CRUD completo de `Project` (Create, Read, Update, Delete)
      com validações no ViewModel. Telas: lista, criação, edição, exclusão
      com confirmação.
      *Critério:* todas as operações cobertas por testes instrumentados
      (Compose UI test); validações verificáveis por mensagens na UI.
      *Atende R3.* *Estimativa:* 8 PH.
      _Entregue em PRs #23, #24, #25, #26 — entities, repos/use cases,
      ProjectDetailViewModel, HomeViewModel com diálogo CRUD._

- [x] **E2.2** — CRUD completo de `Task`. Subtelas: criação, edição,
      mudança de status (todo/doing/done), atribuição a membro.
      *Critério:* mesmas condições de E2.1.
      *Atende R3.* *Estimativa:* 8 PH.
      _Entregue em PRs #23, #24, #25 — entities + DAOs,
      ChangeTaskStatusUseCase + ProjectDetailViewModel com diálogo de
      tarefas (criar/editar/mudar status)._

- [x] **E2.3** — Regra de negócio **RN01 — Limite de tarefas por projeto**:
      projeto não pode ter mais que 50 tarefas ativas simultaneamente.
      Tentativas excedentes exibem mensagem de erro.
      *Critério:* teste unitário `ProjectLimitsTest` cobre o caso limite.
      *Atende R4.* *Estimativa:* 2 PH.
      _Entregue em PR #24 — `CreateTaskUseCase` valida o limite e lança
      `BusinessRuleException(RN01)`; cobertura por teste Robolectric em
      `:core:data`._

- [ ] **E2.4** — Regra de negócio **RN02 — Prioridade obrigatória**:
      toda Task deve ter prioridade no intervalo 0..4; alteração de
      prioridade de uma Task concluída é bloqueada.
      *Critério:* teste `TaskPriorityRulesTest` verde; UI reflete a
      proibição. *Atende R4.* *Estimativa:* 3 PH.

- [ ] **E2.5** — Regra de negócio **RN03 — Conclusão cascata**: ao
      concluir a última Task de um Project, o Project assume estado
      "concluído" automaticamente e é exibido em uma aba dedicada.
      *Critério:* teste unitário `ProjectCompletionTest`; UI atualiza a
      aba. *Atende R4.* *Estimativa:* 3 PH.

- [x] **E2.6** — Listagem de projetos com filtro por nome, ordenação por
      data de criação ou nome, busca textual. Filtros salvos em
      `DataStore`. *Critério:* testes instrumentados validam a busca;
      busca persiste entre sessões. *Atende R9.* *Estimativa:* 5 PH.
      _Entregue em PRs #23, #26, #27 — `ProjectDao.observeAllForOwner`
      com ordenação, HomeViewModel observando a lista e a aba
      `TasksScreen` substituindo o placeholder da Home._

- [ ] **E2.7** — Visão consolidada em Dashboard: contagem de projetos
      por estado, gráfico de tarefas por prioridade, taxa de conclusão
      semanal.
      *Critério:* gráficos atualizam quando o banco muda; testes de UI
      verificam valores iniciais. *Atende R9.* *Estimativa:* 5 PH.

- [ ] **E2.8** — Tratamento explícito de erros: loaders durante
      carregamento, empty states nas listas, mensagens amigáveis em
      falhas de validação. *Critério:* capturas anexadas ao relatório
      de defeitos cobrem os três cenários. *Atende R10.* *Estimativa:* 3 PH.

### Saída do Ciclo 2

CRUDs funcionais contra Room, três regras de negócio verificadas em testes,
dashboard consolidado.

---

## Ciclo 3 — Sincronização, integração externa, recurso nativo

**Período:** 26/10 a 06/11/2026 (Semanas 13 e 14)
**Marco:** Checkpoint 2 em 06/11 (demonstração da versão beta)

### Marcos

- [x] **E3.1** — Decidir plataforma definitiva do serviço de retaguarda
      (Firebase, Supabase ou backend próprio) e documentar a justificativa
      em `docs/ARQUITETURA.md`. Comunicar a decisão no Checkpoint 2.
      *Critério:* decisão registrada em ata, validada pelo docente.
      *Atende N1 item 2 e N2 item 2.* *Estimativa:* 2 PH.
      _Decisão: backend próprio (FastAPI), evoluindo o stub em
      `backend-stub/`. Justificativa em `docs/ARQUITETURA.md`, Seção 9;
      ata em `docs/ATAS/checkpoint2.md`._

- [ ] **E3.2** — Implementar cliente HTTP (Ktor ou Retrofit) configurado
      por build flavor (`debug` aponta para o stub FastAPI em
      `backend-stub/`; `release` aponta para o serviço definitivo).
      *Critério:* `BASE_URL` injetada via `local.properties`; nenhum host
      hard-coded no código. *Atende R6.* *Estimativa:* 5 PH.

- [ ] **E3.3** — Sincronização bidirecional Room ↔ backend. Estratégia:
      `WorkManager` com `OneTimeWorkRequest` ao detectar conectividade;
      fila de operações offline persistida em Room.
      *Critério:* testes de integração (Compose + Robolectric + MockWebServer)
      validam o fluxo online e offline. *Atende R5 e R6.*
      *Estimativa:* 13 PH.

- [ ] **E3.4** — Tratamento de ausência de conectividade: banner
      persistente "offline", fila visível ao usuário, reconciliação
      automática quando a rede retorna. *Critério:* teste manual em modo
      avião reproduz o fluxo. *Atende R5, R10.* *Estimativa:* 3 PH.

- [ ] **E3.5** — Consumir 1 serviço externo pertinente ao domínio.
      *Sugestões:* ICS feed para sincronizar prazos (iCal); API pública de
      feriados nacionais para alertas de data; serviço de geocoding para
      associar Task a um local.
      *Critério:* integração coberta por testes com MockWebServer.
      *Atende R7.* *Estimativa:* 5 PH.

- [ ] **E3.6** — Recurso nativo do dispositivo: **notificações push
      locais** (lembretes de prazos). Configurado com permissão runtime
      para Android 13+ (`POST_NOTIFICATIONS`), canal dedicado,
      `BroadcastReceiver` para ação de "concluir".
      *Critério:* notificação agendada dispara no horário programado;
      ação altera o status da Task via WorkManager. *Atende R8.*
      *Estimativa:* 5 PH.

- [x] **E3.7** — Atualizar `docs/CI-CD.md` com o procedimento de release
      e os segredos necessários (R12, R14). *Critério:* secrets
      documentados; `release-apk.yml` produz `.aab` assinado.
      *Estimativa:* 2 PH.

### Saída do Ciclo 3

Beta demonstrável no Checkpoint 2: app instalado em dispositivo físico,
sincronização com backend funcional, integração externa coberta por testes,
notificações ativas.

---

## Ciclo 4 — Testes, acessibilidade, congelamento

**Período:** 16/11 a 27/11/2026 (Semanas 16 e 17)
**Marco:** Congelamento de escopo em 27/11

### Marcos

- [ ] **E4.1** — Roteiro de testes funcionais cobrindo os fluxos
      principais (cadastro, login, criar projeto, criar tarefa,
      sincronizar offline→online, receber notificação).
      *Critério:* pelo menos 12 casos com resultado esperado/observado.
      *Atende N2 item 3.* *Estimativa:* 5 PH.

- [ ] **E4.2** — Sessões de teste de usabilidade com 5 usuários externos,
      perfil compatível com o público-alvo. *Critério:*
      relatório com perfil dos participantes, achados priorizados.
      *Atende N2 item 3.* *Estimativa:* 5 PH.

- [ ] **E4.3** — Registro e classificação de defeitos. Política:
      Bloqueante, Crítico, Menor. *Critério:* defeitos Bloqueantes e
      Críticos corrigidos antes da N2; defeitos Menores com
      justificativa de não correção. *Atende 6.3.* *Estimativa:* 3 PH.

- [ ] **E4.4** — Acessibilidade: contraste AA, áreas de toque ≥ 48dp,
      `contentDescription` em todos os elementos visuais não textuais,
      suporte a TalkBack nos fluxos principais.
      *Critério:* varredura com Accessibility Scanner sem alertas
      críticos. *Atende R11.* *Estimativa:* 3 PH.

- [ ] **E4.5** — Tema claro e escuro, com tokens centralizados em
      `:core:ui/theme`. *Critério:* alternância segue a configuração do
      sistema. *Atende item desejável 5.1.* *Estimativa:* 2 PH.

- [ ] **E4.6** — Internacionalização: `values/strings.xml` em português,
      `values-en/strings.xml` em inglês (item desejável 5.1).
      *Critério:* zero hardcoded strings no código Kotlin.
      *Estimativa:* 2 PH.

- [x] **E4.7** — Testes automatizados expandidos: cobertura mínima de
      60% nas camadas `:core:domain` e `:core:data` (item desejável 5.1).
      *Critério:* relatório de cobertura publicado como artifact do CI.
      *Estimativa:* 3 PH.
      *Status:* ✅ Kover 0.9.9 aplicado em `:core:domain` e `:core:data`
      com bound de 60% (`koverVerify`); cobertura atual: domínio 87,9%,
      data 67,3%. Relatório HTML publicado como artifact `coverage-report`
      no job `unit-tests` do `ci.yml`. Detalhes em `docs/CI-CD.md`
      (seção "Cobertura de testes").

- [ ] **E4.8** — Congelamento de escopo em 27/11. A partir desta data,
      apenas correções. Novas features migradas para backlog pós-N2.
      *Critério:* tag `v1.0.0-rc` criada. *Estimativa:* 1 PH.

### Saída do Ciclo 4

App estável, sem defeitos bloqueantes, com cobertura de testes e sessão de
usabilidade concluída.

---

## Encerramento — Documentação final e entrega

**Período:** 30/11 a 11/12/2026 (Semanas 18 e 19)
**Entrega:** N2 (07 a 11/12)

### Marcos

- [ ] **E5.1** — Relatório técnico final em PDF conforme Apêndice A.2.
      Inclui mapeamento R1–R14 → componente, relatório de testes,
      instruções de instalação e credenciais por perfil.
      *Atende N2 item 4.* *Estimativa:* 5 PH.

- [ ] **E5.2** — `README.md` completo: pré-requisitos, build, execução,
      troubleshooting, links para o documento norteador e para o
      relatório técnico.
      *Critério:* copy-paste dos comandos leva a um app rodando.
      *Atende R13.* *Estimativa:* 2 PH.

- [ ] **E5.3** — APK release assinado e testado em pelo menos 2
      dispositivos físicos diferentes.
      *Critério:* workflow `Release APK/AAB` verde, artifact baixado e
      instalado com sucesso. *Atende R14.* *Estimativa:* 2 PH.

- [ ] **E5.4** — Apêndice C (Lista de verificação de conformidade
      técnica) preenchido. *Critério:* 14 linhas marcadas como
      "Atendido" com referência ao caminho no app/repositório.
      *Estimativa:* 1 PH.

- [ ] **E5.5** — Apresentação da N2: 20 min + 10 min de arguição.
      *Critério:* ensaio geral realizado antes da data.
      *Atende N2 item 5.* *Estimativa:* 3 PH.

---

## Mapeamento R1–R14

| Requisito | Onde é atendido                                              |
|-----------|--------------------------------------------------------------|
| R1        | E1.3, E1.6, E2.1, E2.2, E3.3                                 |
| R2        | E1.6, E1.7, E1.8                                             |
| R3        | E2.1, E2.2                                                   |
| R4        | E2.3, E2.4, E2.5                                             |
| R5        | E1.5, E1.8, E3.3, E3.4                                       |
| R6        | E3.1, E3.2, E3.3, E3.4                                       |
| R7        | E3.5                                                         |
| R8        | E3.6                                                         |
| R9        | E2.6, E2.7                                                   |
| R10       | E1.6, E2.8, E3.4                                             |
| R11       | E4.4, E4.5                                                   |
| R12       | E1.1, E3.2, E4.7                                             |
| R13       | E1.9, E1.10, E5.2                                            |
| R14       | E3.7, E5.3                                                   |

## Riscos e mitigações

| Risco                                                    | Probabilidade | Impacto | Mitigação                                                                  |
|----------------------------------------------------------|---------------|---------|----------------------------------------------------------------------------|
| Defasagem entre o cronograma e a execução individual     | Média         | Alto    | Checkpoint quinzenal pessoal com status por ciclo                          |
| Backend escolhido muda de escopo no meio do semestre     | Média         | Alto    | Stub FastAPI em `backend-stub/` desacopla o app; contrato versionado       |
| Keystore perdido antes da N2                             | Baixa         | Alto    | Keystore obrigatoriamente guardado em cofre pessoal (1Password/Proton)     |
| Não-conformidade de acessibilidade identificada tardia  | Média         | Médio   | E4.4 começa com auditoria no fim do Ciclo 2, não no Ciclo 4                |
| Dependabot quebrando builds por atualização major        | Baixa         | Médio   | Groups configurados para minor/patch apenas; majors viram Auto-PR manual   |

## Acompanhamento

- **Quadro de tarefas:** GitHub Projects (link a adicionar).
- **Backlog priorizado:** Issues com label `backlog`.
- **Sincronização do quadro com o roadmap:** feita a cada checkpoint,
  atualizando este documento em PR dedicado.
- **Fator de Participação Individual (FPI):** avaliado pelo docente
  com base no histórico de PRs, issues e commits. A contribuição
  técnica deve ser contínua e compatível com o escopo do semestre
  (item 8.4 do documento norteador).