# Smoke test manual — CRUD de Projetos, Tarefas e Tags (Ciclo 2)

> Procedimento de aceitação manual do Ciclo 2 (E2.1, E2.2, E2.3, E2.6)
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