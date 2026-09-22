# Smoke test manual — CRUD de Projetos, Tarefas e Tags (Ciclo 2)

> Procedimento de aceitação manual do Ciclo 2 (E2.1, E2.2, E2.3, E2.6, E2.8)
> do BrainOut. Deve ser executado em **emulador Android** (API 33+)
> com a APK debug gerada por `./gradlew :app:assembleDebug`. Os
> resultados devem ser fotografados/anexados ao relatório da entrega N1.

## Pré-condições

- APK debug instalada (`adb install -r app/build/outputs/apk/debug/app-debug.apk`).
- Banco local limpo (desinstalar o app ou `adb shell pm clear pucgo.joaopedrogmsilva.brainout.debug`).
- Nenhum usuário cadastrado previamente.

## Roteiro

### 1. Cadastro do Owner (E1.6)

| Passo | Ação                                                                 | Resultado esperado |
|-------|----------------------------------------------------------------------|--------------------|
| 1.1   | Abrir o app → tela Splash → Login.                                   | Navegação automática para Login. |
| 1.2   | Tocar em **Cadastrar**.                                              | Tela Register. |
| 1.3   | Preencher `Nome=João`, `E-mail=joao@x`, `Senha=12345678`.            | Formulário aceita. |
| 1.4   | Tocar em **Registrar**.                                              | Redireciona para Home; usuário logado com perfil `OWNER`. |

### 2. Criar projeto com tags (E2.1 + E2.6)

| Passo | Ação                                                                          | Resultado esperado |
|-------|-------------------------------------------------------------------------------|--------------------|
| 2.1   | Na Home, tocar em **+ Projeto**.                                              | Diálogo "Novo projeto" abre. |
| 2.2   | `Nome=App Android`, `Descrição=App de estudo`, tags `Estudo`, `Urgente`.       | Campos preenchidos; chips de tag visíveis. |
| 2.3   | Confirmar.                                                                    | Diálogo fecha; projeto aparece na lista da Home com nome e chips de tag. |

### 3. Criar 5 tarefas (E2.2)

| Passo | Ação                                                                                  | Resultado esperado |
|-------|---------------------------------------------------------------------------------------|--------------------|
| 3.1   | Tocar no projeto **App Android** na Home.                                             | Abre `ProjectDetailScreen` com lista vazia. |
| 3.2   | Tocar em **+ Tarefa** cinco vezes, com títulos `T1` a `T5`, prioridade alternando.   | Lista mostra as 5 tarefas em ordem de prioridade. |
| 3.3   | Voltar para Home.                                                                    | Contador de tarefas do projeto exibe `5`. |

### 4. Validar aba Tarefas (E2.6)

| Passo | Ação                              | Resultado esperado |
|-------|-----------------------------------|--------------------|
| 4.1   | Tocar na aba **Tarefas** (bottom). | Abre `TasksScreen` listando as 5 tarefas com nome do projeto. |
| 4.2   | Filtrar por `T3`.                 | Lista filtra para apenas `T3`. |

### 5. Regra de negócio **RN01** — Limite de 50 tarefas (E2.3)

| Passo | Ação                                                                                                | Resultado esperado |
|-------|-----------------------------------------------------------------------------------------------------|--------------------|
| 5.1   | Criar tarefas até atingir **50** (`T6` a `T50`).                                                     | Cada inserção sucede. |
| 5.2   | Tentar criar a **51ª** tarefa (`T51`).                                                              | App exibe mensagem amigável: "Limite de 50 tarefas ativas por projeto (RN01)". |
| 5.3   | Verificar que o contador na Home continua em `50`.                                                  | Sem regressão. |
| 5.4   | Marcar uma tarefa como `DONE`.                                                                        | Contador cai para `49`; nova criação agora é aceita. |

### 6. Editar projeto (E2.1 — Update)

| Passo | Ação                                                                              | Resultado esperado |
|-------|-----------------------------------------------------------------------------------|--------------------|
| 6.1   | Na Home, tocar no projeto **App Android** → **Editar**.                            | Diálogo "Editar projeto" abre com dados preenchidos. |
| 6.2   | Trocar tag `Estudo` por `Faculdade`; renomear para `App Android — v2`.            | Campos atualizados. |
| 6.3   | Confirmar.                                                                        | Lista reflete novo nome e nova tag; tag `Estudo` deixa de aparecer. |

### 7. Excluir projeto com cascade (E2.1 — Delete)

| Passo | Ação                                                                                  | Resultado esperado |
|-------|---------------------------------------------------------------------------------------|--------------------|
| 7.1   | Na Home, tocar no projeto **App Android — v2** → **Excluir** → confirmar.             | Projeto some da lista. |
| 7.2   | Abrir a aba **Tarefas**.                                                              | Lista está vazia — tarefas foram removidas em cascade. |
| 7.3   | (Opcional) Inspecionar o SQLite via `adb shell run-as pucgo.joaopedrogmsilva.brainout.debug cat databases/brainout.db > /sdcard/db.sqlite` e validar `SELECT COUNT(*) FROM project_tags WHERE tag_id IN (SELECT id FROM tags) == 0` para tags remanescentes. | Sem órfãos. |

## Critérios de pronto

- [ ] Todos os 7 blocos executados sem crash (sem `ANR`, sem exceção não tratada).
- [ ] Mensagens amigáveis em falhas de validação (sem stack traces expostos).
- [ ] Estado preservado entre rotações (girar o dispositivo durante qualquer tela).
- [ ] Persistência verificada: matar o app e reabrir — projetos, tarefas e tags mantidos.

---

## 8. Notificações de prazo em dispositivo físico (E3.6 — Checkpoint 2)

> Roteiro adicional para o **Checkpoint 2** (dispositivo físico, Android 13+).
> A notificação em horário programado não é verificável em teste de unidade;
> este bloco cobre o critério do E3.6 no aparelho real.

| Passo | Ação | Resultado esperado |
|-------|------|--------------------|
| 8.1   | Primeira entrada na Home após instalação limpa. | Diálogo nativo pede permissão de notificações (`POST_NOTIFICATIONS`). |
| 8.2   | Conceder a permissão. | Sem diálogo repetido nas próximas entradas (flag em DataStore). |
| 8.3   | (Cenário alternativo) Reinstalar, negar a permissão. | Toast explicativo sobre lembretes; app segue funcionando; diálogo não se repete. |
| 8.4   | Criar projeto "App Android" e tarefa "Entrega" com prazo `agora + 2h`. | Tarefa criada; lembrete agendado para `prazo − 1h` (trabalho `deadline-<taskId>` visível em `adb shell dumpsys jobscheduler \| grep -i brainout`). |
| 8.5   | Aguardar o horário do lembrete (ou adiantar o relógio nas Configurações). | Notificação no canal "Lembretes de prazo": título "Prazo: Entrega", texto "Projeto App Android — prazo em 1 hora". |
| 8.6   | Tocar em **Concluir** na notificação. | Notificação some; ao abrir o app a tarefa aparece como `DONE` (mudança aplicada via WorkManager). |
| 8.7   | Criar nova tarefa com prazo `+2h`, aguardar a notificação e tocar em **Dispensar** (ou descartar). | Notificação some; tarefa permanece no status anterior. |
| 8.8   | Concluir a tarefa pela UI antes do lembrete disparar. | Nenhuma notificação dispara no horário (trabalho cancelado). |
| 8.9   | Excluir tarefa com lembrete pendente. | Nenhuma notificação dispara; sem lembrete fantasma. |

**Critérios de pronto adicionais (E3.6):**

- [ ] Passos 8.1–8.9 executados em dispositivo físico sem crash.
- [ ] Notificação disparada dentro da janela de tolerância do WorkManager (≈ minutos do horário programado).
- [ ] Ação "Concluir" altera o status para `DONE` mesmo com o app em segundo plano fechado.

---

## 9. Estados de erro, loaders e validação inline (E2.8)

> Roteiro adicional para o **E2.8** (loader + erro/retry nos três
> ViewModels e validação inline na criação de projeto). Deve ser
> executado em emulador Android (API 33+) com a APK debug gerada
> por `./gradlew :app:assembleDebug`. Os prints ficam anexados
> ao relatório da entrega N2 para evidenciar R10.

### 9.1. Loader + lista vazia (Home)

| Passo | Ação | Resultado esperado |
|-------|------|--------------------|
| 9.1.1 | Limpar dados (`adb shell pm clear pucgo.joaopedrogmsilva.brainout.debug`) e fazer login com um Owner. | App abre na Home com a lista vazia. |
| 9.1.2 | Criar 1 projeto "Demo". | Card do projeto aparece na Home. Loader NÃO é visível após o card carregar (Room emite rápido em emulador). |

### 9.2. Validação inline na criação de projeto (Home)

| Passo | Ação | Resultado esperado |
|-------|------|--------------------|
| 9.2.1 | Na Home, tocar em **+ Projeto**. | Diálogo "Novo projeto" abre com `Nome` vazio. |
| 9.2.2 | Tocar em **Criar** sem preencher o nome. | Diálogo **permanece aberto**; `TextField` do nome fica com `isError` (borda vermelha) e mostra `supportingText` "Informe um nome para o projeto." (pt) ou "Please enter a project name." (en). |
| 9.2.3 | Digitar "App" e tocar em **Criar**. | Diálogo fecha; card "App" aparece na lista. |
| 9.2.4 | Repetir o passo 9.2.2 para confirmar que a validação continua disparando. | Mesma mensagem de erro. |

### 9.3. Banner de erro de carga + retry (simulado)

> O Room em emulador raramente falha, então o cenário real (ex.
> `SQLiteException`) precisa ser provocado. Em ambiente de
> desenvolvimento, o jeito mais simples é **forçar o banco a
> abrir em modo corrompido** (e.g. setando uma senha errada via
> `Room.databaseBuilder` em build `debug`); este roteiro assume
> esse passo ou um mock equivalente. Para a N2, capturas de
> teste unitário (`HomeViewModelTest.E2 8 erro do Flow de projetos…`)
> servem como evidência substituta caso o cenário real seja difícil
> de reproduzir manualmente.

| Passo | Ação | Resultado esperado |
|-------|------|--------------------|
| 9.3.1 | Provocar falha do Room no `observeAllForOwner` (ver nota acima). | Home mostra `CircularProgressIndicator` brevemente e em seguida banner "Não foi possível carregar seus projetos" (pt) com botões **Tentar novamente** e **Dispensar**. |
| 9.3.2 | Tocar em **Tentar novamente** após restaurar o Room. | Banner desaparece; lista de projetos reaparece. |
| 9.3.3 | Tocar em **Dispensar**. | Banner some; empty state aparece (lista vazia, sem spinner). |

### 9.4. Loader + erro de tarefas (ProjectDetail)

| Passo | Ação | Resultado esperado |
|-------|------|--------------------|
| 9.4.1 | Abrir um projeto com 5 tarefas. | ProjectDetail mostra as 5 tarefas. |
| 9.4.2 | Provocar falha do Room em `observeForProject` (mesma técnica de 9.3.1). | Banner de erro aparece em vez das tarefas; spinner some imediatamente (`isLoading = false`). |
| 9.4.3 | Tocar em **Tentar novamente** após restaurar. | Lista de tarefas reaparece. |

### 9.5. Loader + erro de tarefas globais (Tasks)

| Passo | Ação | Resultado esperado |
|-------|------|--------------------|
| 9.5.1 | Na aba **Tarefas** da Home (bottom bar), com Owner ativo. | Lista global de tarefas aparece. |
| 9.5.2 | Provocar falha do Room em `observeAllForOwner` para tarefas. | Banner "Não foi possível carregar suas tarefas" (pt) com botões **Tentar novamente** e **Dispensar**. |
| 9.5.3 | Tocar em **Tentar novamente** após restaurar. | Lista reaparece. |

### 9.6. Localização pt/en

| Passo | Ação | Resultado esperado |
|-------|------|--------------------|
| 9.6.1 | Trocar idioma do dispositivo para inglês (`Settings → System → Languages`). Reabrir o app. | Todos os textos novos do E2.8 aparecem em inglês: "Loading projects", "Couldn't load your projects", "Try again", "Dismiss", "Please enter a project name." |
| 9.6.2 | Trocar idioma para português. | Textos voltam para o português. |

## 10. Busca, filtro por tag e ordenação (E2.6)

Pré-condições: app instalado, banco limpo, pelo menos 2 Owners
cadastrados com 5 projetos e 3 tags. Cada projeto nomeado de forma
distinta: `App Android`, `Site portfólio`, `Estudo Kotlin`,
`Reunião semanal`, `Backup rotina`. Tags: `Estudo` (cobre App
Android e Estudo Kotlin), `Urgente` (cobre Reunião semanal), `Outros`
(cobre Site portfólio e Backup rotina).

### 10.1. Busca textual

| Passo | Ação | Resultado esperado |
|-------|------|--------------------|
| 10.1.1 | Abrir a aba Projetos com 5 cards visíveis. | Barra de busca aparece no topo da aba com placeholder "Buscar projeto por nome…" (pt) / "Search projects by name…" (en). |
| 10.1.2 | Digitar `Kotlin` no campo de busca. | Após ~300ms (debounce), apenas o card "Estudo Kotlin" permanece; os outros 4 somem. |
| 10.1.3 | Confirmar que o `LIKE` é case-insensitive para ASCII: limpar e digitar `kotlin` (minúsculo). | Mesmo resultado — "Estudo Kotlin" continua aparecendo. |
| 10.1.4 | Digitar `xyz` (sem matches). | Empty state com mensagem "Nenhum projeto encontrado para 'xyz'" e corpo "Ajuste a busca ou o filtro de tag para ver mais resultados." |
| 10.1.5 | Tocar no **X** do campo de busca. | Lista completa dos 5 cards reaparece imediatamente (sem esperar o debounce). |

### 10.2. Filtro por tag

| Passo | Ação | Resultado esperado |
|-------|------|--------------------|
| 10.2.1 | Tocar no chip **Estudo** na `LazyRow` de tags. | Lista reduz a 2 cards: "App Android" e "Estudo Kotlin". O chip fica marcado. |
| 10.2.2 | Tocar de novo no chip **Estudo** para desselecionar. | Lista volta aos 5 cards. |
| 10.2.3 | Tocar em **Todas**. | Mesmo resultado — todos os 5 cards. |
| 10.2.4 | Selecionar **Urgente** + digitar `Reunião` na busca. | Apenas "Reunião semanal" aparece (filtro E busca combinados). |

### 10.3. Ordenação

| Passo | Ação | Resultado esperado |
|-------|------|--------------------|
| 10.3.1 | Tocar no ícone de sort (engrenagem) no canto direito da linha de filtros. | Dropdown com 4 opções: Nome A→Z, Nome Z→A, Mais recentes, Mais antigas. "Mais recentes" tem leading check. |
| 10.3.2 | Selecionar **Nome A→Z**. | Lista reordena alfabeticamente: App Android, Backup rotina, Estudo Kotlin, Reunião semanal, Site portfólio. Leading check passa para "Nome A→Z". |
| 10.3.3 | Selecionar **Nome Z→A**. | Lista inverte. |
| 10.3.4 | Selecionar **Mais antigas**. | Lista reordena por `created_at` ASC. |

### 10.4. Persistência entre sessões

| Passo | Ação | Resultado esperado |
|-------|------|--------------------|
| 10.4.1 | Com busca `Kotlin` + filtro `Estudo` + sort `Nome A→Z` ativos, forçar `kill` do app (`adb shell am force-stop pucgo.joaopedrogmsilva.brainout.debug`). | — |
| 10.4.2 | Reabrir o app e fazer login como mesmo Owner. | Os 3 filtros aparecem restaurados: campo com `Kotlin`, chip Estudo marcado, sort Nome A→Z ativo. Lista filtrada combina os 3. |
| 10.4.3 | Trocar para o segundo Owner (logout/login). | Filtros voltam ao default (busca vazia, "Todas", Mais recentes) — preferências são namespaced por `userId`. |

### 10.5. Localização pt/en

| Passo | Ação | Resultado esperado |
|-------|------|--------------------|
| 10.5.1 | Trocar idioma do dispositivo para inglês. Reabrir o app. | "Search projects by name…", "Clear search", "All", "Sort projects", "Name A→Z", "Name Z→A", "Newest first", "Oldest first", "No projects match \"Kotlin\"", "Adjust the search query or tag filter to see more results." |
| 10.5.2 | Voltar para português. | Textos revertem. |

**Critérios de pronto (E2.6):**

- [ ] Passos 10.1 a 10.4 executados sem crash e com os 3 filtros combinados.
- [ ] Caso 10.4.3 cobre que preferências não vazam entre contas.
- [ ] `MissingTranslation` continua fatal — chaves E2.6 sem par pt/en quebram o build.

**Critérios de pronto (E2.8):**

- [ ] Passos 9.2.x e 9.6.x executados sem crash e com a localização correta.
- [ ] Cenários 9.3, 9.4, 9.5 cobertos por pelo menos uma captura OU por referência explícita ao teste unitário correspondente (`HomeViewModelTest.E2 8 erro do Flow...`, `ProjectDetailViewModelTest.E2 8 erro do Flow de tarefas...`, `TasksViewModelTest.E2 8 erro do Flow de tasks...`).
- [ ] `MissingTranslation` continua fatal — qualquer chave nova sem par pt/en quebra o build.
---

## 11. Banner offline + fila visível + reconciliação (E3.4)

> Roteiro adicional para o **E3.4** (banner offline, contagem de
> operações pendentes, reconciliação ao voltar a rede). Deve ser
> executado em **emulador Android (API 33+)** com a APK debug
> (`./gradlew :app:assembleDebug`) e o backend-stub rodando no host
> (`python3 backend-stub/server.py` — o app dev aponta para
> `http://10.0.2.2:8000/`).

### Pré-condições

- Backend-stub ativo no host (porta 8000) e app conectado a ele.
- Owner com sessão ativa na Home.

### 11.1. Banner offline + contagem da fila

| Passo | Ação | Resultado esperado |
|-------|------|--------------------|
| 11.1.1 | Com o app aberto na Home e backend acessível, ativar o **modo avião** do emulador (`Extended Controls → Cellular/Wi-Fi` off, ou `adb shell cmd connectivity airplane-mode enable`). | Banner persistente "Sem conexão" aparece no topo da aba Projetos (testTag `home_offline_banner`), acima da barra de busca. |
| 11.1.2 | Com o modo avião ativo, criar um projeto "Offline P1" e uma tag "Offline". | Escritas locais sucedem (Room); banner mostra "2 alterações aguardando sincronização" (projeto + tag enfileirados em `pending_ops`). |
| 11.1.3 | Abrir o projeto "Offline P1" e criar a tarefa "Tarefa offline". | Banner do detalhe também aparece ("Sem conexão" + "3 alterações aguardando sincronização", testTag `project_detail_offline_banner`). |
| 11.1.4 | Verificar a fila local: `adb shell run-as pucgo.joaopedrogmsilva.brainout.debug sqlite3 databases/brainout.db "SELECT COUNT(*) FROM pending_ops"`. | Contagem = 3 (ou o número de escritas feitas offline). |

### 11.2. Reconciliação ao voltar a rede

| Passo | Ação | Resultado esperado |
|-------|------|--------------------|
| 11.2.1 | Desativar o modo avião (rede volta). | O callback de rede enfileira um `OneTimeWorkRequest` (`brainout-sync-now`); o banner some quando a rede é detectada. |
| 11.2.2 | Aguardar alguns segundos e verificar a fila (`SELECT COUNT(*) FROM pending_ops`). | Contagem zera — o `SyncWorker` drenou as 3 ops contra o stub. |
| 11.2.3 | Verificar o backend-stub: `curl -s http://localhost:8000/v1/projects | grep "Offline P1"`. | O projeto criado offline aparece no backend (reconciliado). |
| 11.2.4 | Reabrir a Home. | Nenhum banner; sem indicação de pendências (fila vazia). |

### 11.3. Localização pt/en

| Passo | Ação | Resultado esperado |
|-------|------|--------------------|
| 11.3.1 | Com o app em inglês e o modo avião ativo. | "No connection" + "2 changes waiting for sync". |
| 11.3.2 | Voltar para português. | "Sem conexão" + "2 alterações aguardando sincronização". |

**Critérios de pronto (E3.4):**

- [ ] Passos 11.1 a 11.3 executados sem crash; banner aparece offline, some online e a fila drena.
- [ ] Caso 11.2.3 evidencia reconciliação ponta a ponta (projeto offline presente no backend).
- [ ] `MissingTranslation` continua fatal — chaves `home_offline_banner_title`/`home_pending_ops_message*` têm par pt/en.
