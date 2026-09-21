# Roteiro de Testes Funcionais — BrainOut (Ciclo 4 / E4.1)

> Procedimento de validação funcional do aplicativo BrainOut, cobrindo
> os fluxos principais de cadastro, autenticação, gestão de projetos e
> tarefas, sincronização offline→online, notificações de prazo, tags
> e troca de perfil. Este roteiro complementa o `docs/SMOKE-TEST-CRUD.md`
> (Ciclo 2) e o bloco de notificações do Checkpoint 2, servindo como
> insumo para o relatório de defeitos (E4.3) e para as sessões de
> usabilidade (E4.2).

## 1. Convenções

- **Ambiente de execução:** emulador Android API 33+ ou dispositivo
  físico compatível (ver `docs/DISPOSITIVOS.md`). Para os casos que
  exigem dois dispositivos ou mudança de conectividade, é necessário o
  dispositivo físico emprestado listado em `docs/DISPOSITIVOS.md`.
- **Pré-condição padrão:** APK debug instalada
  (`./gradlew :app:assembleDebug` seguido de
  `adb install -r app/build/outputs/apk/debug/app-debug.apk`) e banco
  local limpo (`adb shell pm clear pucgo.joaopedrogmsilva.brainout.debug`).
- **Coleta de evidência:** captura de tela anexada ao relatório da
  entrega (N2 item 3). Vídeo opcional para os casos de sincronização.
- **Severidade de defeito** (alinhada à política de E4.3):
  - **Bloqueante** — impede a execução de um fluxo crítico; o app
    fecha ou perde dados.
  - **Crítico** — funcionalidade principal degrada, mas há contorno
    documentado.
  - **Menor** — cosmético, mensagem ambígua, animação fora do padrão.
- **Status de cada caso:**
  - ⏳ **Pendente** — ainda não executado nesta build.
  - ✅ **Aprovado** — resultado observado confere com o esperado.
  - ❌ **Reprovado** — resultado observado diverge; defeito registrado.
- **Resultado observado:** preenchido durante a sessão manual. Permanece
  `—` até a execução; os casos de sincronização (TF-09, TF-10)
  dependem da conclusão de E3.3 e E3.4 (ver
  [`docs/ROADMAP.md`](./ROADMAP.md)).

## 2. Tabela de casos

| ID      | Fluxo                                     | Passos                                                                                                                                                                                                | Resultado esperado                                                                                                                                                                  | Resultado observado | Status |
|---------|-------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------|--------|
| TF-01   | Cadastro — dados válidos                  | 1) Abrir o app. 2) Tela Login → tocar em **Cadastrar**. 3) Preencher `Nome="Maria"`, `E-mail="maria@brainout.test"`, `Senha="senha-123"`, `Confirmação="senha-123"`. 4) Tocar em **Registrar**.         | Redirecionamento para a Home; usuário logado com perfil `OWNER`; nenhuma mensagem de erro.                                                                                          | —                   | ⏳     |
| TF-02   | Cadastro — e-mail duplicado               | 1) Com o usuário de TF-01 já cadastrado, tocar em **Sair**. 2) Em **Cadastrar**, preencher `Nome="Maria2"`, `E-mail="maria@brainout.test"` (mesmo e-mail), `Senha="outra-123"`. 3) Tocar em **Registrar**. | Permanecer na tela Register; mensagem inline de erro no campo `E-mail` indicando que o endereço já está em uso; nenhum registro gravado.                                            | —                   | ⏳     |
| TF-03   | Login — credenciais corretas              | 1) Com o app aberto na tela Login, preencher `E-mail="maria@brainout.test"`, `Senha="senha-123"`. 2) Tocar em **Entrar**.                                                                              | Redirecionamento para a Home; saudação com o nome do usuário; nenhuma mensagem de erro.                                                                                              | —                   | ⏳     |
| TF-04   | Login — senha errada                      | 1) Na tela Login, preencher `E-mail="maria@brainout.test"`, `Senha="errada-999"`. 2) Tocar em **Entrar**.                                                                                              | Permanecer na tela Login; mensagem inline genérica de "credenciais inválidas" (sem distinguir usuário inexistente de senha incorreta, conforme política de privacidade do R2).        | —                   | ⏳     |
| TF-05   | Criar projeto — válido                    | 1) Na Home, tocar em **+ Projeto**. 2) Preencher `Nome="App Android"`, `Descrição="App de estudo"`. 3) Tocar em **Salvar**.                                                                            | Diálogo fecha; projeto aparece na lista da Home; contador de tarefas exibe `0`; tag/categoria indefinida (será atribuída em TF-13).                                                  | —                   | ⏳     |
| TF-06   | Criar projeto — nome vazio bloqueado      | 1) Na Home, tocar em **+ Projeto**. 2) Deixar o campo `Nome` em branco. 3) Tocar em **Salvar**.                                                                                                       | Diálogo permanece aberto; validação inline marca o campo `Nome` com erro "Campo obrigatório"; nenhum registro gravado.                                                              | —                   | ⏳     |
| TF-07   | Criar tarefa — válida                     | 1) Abrir o projeto "App Android" na Home. 2) Tocar em **+ Tarefa**. 3) Preencher `Título="Entrega N1"`, `Prioridade=2`, `Prazo=hoje + 7 dias`. 4) Tocar em **Salvar**.                                | Lista de tarefas do projeto mostra a nova tarefa com a prioridade e o prazo exibidos; status inicial `TODO`.                                                                        | —                   | ⏳     |
| TF-08   | Criar tarefa — RN01 bloqueia na 51ª ativa | 1) No projeto "App Android", criar 50 tarefas com títulos `T01` a `T50`, todas com status `TODO` ou `DOING` (ativas). 2) Tentar criar a 51ª tarefa ativa com `Título="T51"`. 3) Tocar em **Salvar**. | Snackbar de erro com a mensagem canônica de **RN01** ("Projeto atingiu o limite de 50 tarefas ativas") herdada de `ProjectDetailViewModel`; nenhuma tarefa gravada; lista segue com 50. | —                   | ⏳     |
| TF-09   | Sincronizar — criar offline e reconciliar | 1) Ativar o modo avião no dispositivo. 2) Criar a tarefa `T-offline-01` no projeto "App Android" (banner "offline" visível). 3) Desativar o modo avião. 4) Aguardar o worker `OneTimeWorkRequest` concluir. | A tarefa aparece na lista local imediatamente após a criação (grava em Room); após a reconexão, a requisição é enfileirada por `WorkManager`, reconciliada com o backend e a contagem remota passa a refletir a nova tarefa (E3.3). | —                   | ⏳     |
| TF-10   | Sincronizar — editar em 2 dispositivos    | 1) No dispositivo A, abrir o projeto "App Android". 2) No dispositivo B (emulador ou segundo aparelho físico), autenticado como o mesmo usuário, abrir o mesmo projeto. 3) Editar `T01` em A: alterar `Prioridade` de 2 para 4. 4) Editar `T01` em B: alterar `Prazo` para hoje + 10 dias. 5) Aguardar a próxima rodada de sincronização nos dois lados. | Sem crash; ambas as edições são preservadas (last-writer-wins por campo, conforme estratégia de E3.3) e propagadas para o outro dispositivo após a próxima sincronização. Sem sobreposição silenciosa: o usuário vê banner de "conflito resolvido" se a estratégia exigir confirmação. | —                   | ⏳     |
| TF-11   | Notificação de prazo — agendada dispara   | 1) Criar a tarefa `Lembrete` no projeto "App Android" com `Prazo = agora + 2 horas`. 2) Conceder permissão `POST_NOTIFICATIONS` (diálogo de runtime na primeira entrada da Home). 3) Aguardar o disparo (ou adiantar o relógio nas Configurações do dispositivo). | Notificação aparece no canal "Lembretes de prazo": título "Prazo: Lembrete", texto "Projeto App Android — prazo em 1 hora" (REMINDER_LEAD = 1h, ver `DeadlineWorker`).                | —                   | ⏳     |
| TF-12   | Notificação de prazo — ação "Concluir"    | 1) A partir do estado de TF-11, tocar em **Concluir** na notificação (intent tratada por `DeadlineReceiver`). 2) Reabrir o app.                                                                     | Notificação desaparece; a tarefa `Lembrete` aparece com status `DONE` mesmo com o app em segundo plano fechado (mudança aplicada via `WorkManager`/`CompleteTaskWorker`).          | —                   | ⏳     |
| TF-13   | Tags — associar 2 tags a um projeto       | 1) Em Configurações, garantir que existem as tags `Estudo` e `Urgente`. 2) Na Home, abrir o diálogo **Editar projeto** de "App Android". 3) Selecionar as duas tags e tocar em **Salvar**.            | Projeto passa a exibir dois chips de tag (`Estudo`, `Urgente`) na lista da Home e na tela de detalhe; tags persistidas em Room e refletidas na próxima sincronização.                 | —                   | ⏳     |
| TF-14   | Logout e troca de perfil                  | 1) Na Home, tocar em **Sair**. 2) Confirmar no diálogo. 3) Tela Login → **Cadastrar**. 4) Criar `E-mail="pedro@brainout.test"`, `Senha="outra-456"`. 5) Reabrir o app após matar o processo.        | Após o logout, o app retorna para a tela Login sem mostrar dados do usuário anterior. O novo cadastro autentica Pedro como `OWNER`. Após matar e reabrir, a sessão de Pedro é restaurada e os projetos de Maria não aparecem (escopo por usuário, R2). | —                   | ⏳     |

## 3. Cobertura por requisito e marco do roadmap

| Requisito (R) | Marcos atendidos                        | Casos relacionados |
|---------------|------------------------------------------|--------------------|
| R1 — CRUD projeto/tarefa                   | E1.3, E2.1, E2.2, E3.3                   | TF-05, TF-06, TF-07, TF-08 |
| R2 — Autenticação e perfis                | E1.6, E1.7                               | TF-01, TF-02, TF-03, TF-04, TF-14 |
| R3 — Mudança de status de tarefas         | E2.2                                     | TF-12 (via `CompleteTaskWorker`) |
| R4 — Regras de negócio                    | E2.3                                     | TF-08 |
| R5 — Sincronização e conectividade        | E3.3, E3.4                               | TF-09, TF-10 |
| R6 — Backend e cliente HTTP               | E3.1, E3.2, E3.3, E3.4                   | TF-09, TF-10 (caminho online) |
| R8 — Notificações                         | E3.6                                     | TF-11, TF-12 |
| R9 — Listagem e filtros                   | E2.6                                     | TF-05, TF-13 (chips na Home) |
| R10 — Tratamento de erros                 | E2.8                                     | TF-02, TF-04, TF-06, TF-08 (mensagens inline/snackbar) |
| R14 — Versionamento e segredo             | E3.7                                     | (procedimento de execução descrito na seção 1) |

## 4. Procedimento de execução

1. **Preparar ambiente.** Instalar a APK debug e limpar o banco local
   conforme seção 1.
2. **Executar na ordem TF-01 → TF-14.** A ordem garante que os
   pré-requisitos de cada caso (usuário cadastrado, projeto existente,
   permissão concedida) já estejam satisfeitos.
3. **Registrar o resultado observado** na coluna correspondente a
   cada execução. Anexar captura de tela ao relatório da entrega N2.
4. **Registrar defeitos** com severidade Bloqueante/Crítico/Menor
   conforme política do E4.3 (item 6.3 do documento norteador). Cada
   defeito reprovado recebe um identificador `DEF-<seq>` e é listado no
   relatório da entrega.
5. **Reexecutar** os casos marcados como ❌ após a correção do defeito
   associado, mantendo o histórico da execução anterior para evidência.

## 5. Pré-condições especiais

- **TF-09** e **TF-10** exigem que os marcos E3.3 e E3.4 do
  [`docs/ROADMAP.md`](./ROADMAP.md) estejam concluídos. Enquanto
  pendentes, os casos permanecem ⏳ e a coluna "Resultado observado"
  fica em branco. A integração do backend `backend-stub/` deve estar
  acessível no emulador (`10.0.2.2`) ou via Tailscale no dispositivo
  físico, conforme `docs/CI-CD.md`.
- **TF-11** e **TF-12** dependem do E3.6 (já entregue em PR #45) e
  devem ser executados em **dispositivo físico** com Android 13+ para
  validar a permissão `POST_NOTIFICATIONS` de runtime.
- **TF-14** exige o dispositivo físico emprestado listado em
  `docs/DISPOSITIVOS.md` apenas para o caso TF-10; o caso TF-14 em si
  é executável em um único dispositivo.

## 6. Critérios de pronto (E4.1)

- [ ] Os 14 casos da seção 2 foram executados em pelo menos uma
      build (debug ou release).
- [ ] A coluna "Resultado observado" está preenchida para todos os
      casos cujas pré-condições estão satisfeitas.
- [ ] Cada defeito reprovado (❌) está registrado no relatório de
      defeitos (E4.3) com severidade, evidência e referência ao caso
      (`TF-<id>`).
- [ ] Os casos dependentes de E3.3/E3.4 (TF-09, TF-10) foram
      reexecutados após a conclusão desses marcos, e o status atualizado
      para ✅ ou ❌ conforme a evidência.
