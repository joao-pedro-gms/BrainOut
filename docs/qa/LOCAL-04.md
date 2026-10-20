# QA — LOCAL-04 (validação do upgrade local e gates de CI)

Issue [#215](https://github.com/joao-pedro-gms/BrainOut/issues/215),
parte do épico [#211](https://github.com/joao-pedro-gms/BrainOut/issues/211).
Data: **20/10/2026**.

---

## 1. O que esta issue verificou

A #215 não entrega funcionalidade: ela **valida** que o aplicativo
totalmente local continua íntegro depois das remoções de rede e de sync
e **endurece os gates de CI** queprotegem essa integridade. A verificação
tem duas partes: o que roda sozinho na CI (determinístico, a cada push) e
o que exige execução real em aparelho (registrado aqui, separado dos
testes JVM, como a issue pede).

| Frente | Situação antes | Situação agora |
|---|---|---|
| CI com backend-integration | Job apontava para o stub removido | Job `local-integration` (build + instrumentação + manifesto) — já vem da #212 |
| `androidTest` na CI | Não compilava (DEF-66) | Compila e é verificado por script — já vem da #218 |
| Verificadores documentais (#205 / DEF-67) | Rodavam só "quando alguém lembra" | **Rodam na CI** (`static-analysis`) |
| Escopo de cobertura (#202 / DEF-64) | Padrão `*_Impl` sugere esconder as implementações | Padrão estreitado + **verificador na CI** |
| Jornada local ponta a ponta | Espalhada em testes por camada | **Um teste de jornada** cobre CRUD → backup → restauração → login → sessão → lembretes |

---

## 2. Execução real em aparelho (instrumentado)

Rodado no AVD `Medium_Phone_API_37.0` (Android 17 / API 37), com
`./gradlew :core:data:connectedDebugAndroidTest :feature:auth:connectedDebugAndroidTest`:

### `:core:data` — 7 testes, zero falhas

| Teste | O que prova |
|---|---|
| `migracoes_encadeadas_1_a_4_preservam_o_usuario_original` | A cadeia 1→2→3→4 preserva o cadastro. |
| `migrate_1_para_2_cria_tabelas_novas_sem_perder_users` | Migration 1→2 isolada. |
| `migrate_2_para_3_adiciona_completed_at_preservando_tarefas` | Migration 2→3 isolada (e o `DEFAULT NULL` legado). |
| `migrate_3_para_4_cria_pending_ops_preservando_dados_existentes` | Migration 3→4 isolada. |
| `migrate_4_para_5_preserva_tarefas_e_remove_a_fila_remota` | **Upgrade com dados v4**: as tarefas sobrevivem e só a fila remota vai embora. |
| `migrate_4_para_5_repara_banco_atualizado_pela_antiga_migration` | Banco v4 reparado pela 2→3 antiga. |
| `insert_and_findByEmail_returns_inserted_entity` | CRUD real de usuário. |

### `:feature:auth` — 3 testes, zero falhas

| Teste | O que prova |
|---|---|
| `login_with_valid_credentials_navigates_to_home` | Login local navega para a Home. |
| `submitting_with_empty_email_shows_email_error_and_focuses_email` | Erro de e-mail vazio com foco no campo. |
| `login_with_invalid_credentials_surfaces_error_and_does_not_navigate` | Credencial errada não navega. |

> A CI **compila** as suítes instrumentadas (não executa: exige
> emulador). A execução real acima é registrada aqui, separada dos
> testes JVM, como a issue pede.

---

## 3. Upgrade com dados v4 — também na JVM

`core/data/src/test/.../local/LocalUpgradeTest.kt` executa na CI, sem
emulador, e cobre:

- a cadeia **v1 → v5** (a base mais antiga suportada);
- o upgrade **v4 canônico** (criado pelo schema versionado `4.json`);
- o upgrade **v4 legado** (criado sem o `DEFAULT NULL` em
  `tasks.completed_at`, como a antiga 2→3 produzia) — a 4→5 repara a
  base sem perder tarefas nem prazos.

Verificações feitas após a migração: hash do usuário, descrição do
projeto, título/responsável/prazo das tarefas, `completed_at`, cor da
etiqueta, associações `project_tags`, `PRAGMA foreign_key_check` limpo e
a ausência da tabela `pending_ops`.

---

## 4. Jornada local ponta a ponta (novo)

`core/data/src/test/.../backup/LocalBackupJourneyTest.kt` percorre, em um
único teste, o caminho de quem instala o aplicativo em um aparelho novo:

1. **CRUD local** — conta, projeto, tarefa com prazo e etiqueta.
2. **Exportação** — arquivo de backup cifrado.
3. **Restauração em outra instalação** — segundo banco Room, com pepper
   diferente (como em outro aparelho).
4. **Login local** — a conta restaurada autentica com a **senha do
   backup**, porque o hash foi derivado com o pepper do aparelho de
   destino.
5. **Sessão** — a sessão persistida é invalidada pela restauração.
6. **Lembretes** — a tarefa com prazo vira lembrete a ser reagendado.
7. **Preferências** — a ordenação da listagem atravessa a instalação.

Não há nenhuma chamada de rede no caminho: o aplicativo é local, e este
teste é a prova mecânica disso.

---

## 5. Gates de CI

| Gate | Onde | O que garante |
|---|---|---|
| ktlint + detekt | `static-analysis` | Estilo e análise estática em todos os subprojetos. |
| **Verificador de tokens** | `static-analysis` (**novo**) | Paridade light/dark, contraste e links dos tokens de design — rodava só manualmente (DEF-67). |
| **Verificador de roadmap** | `static-analysis` (**novo**) | Links, ids de tarefa, ordenação e aciclicidade do plano — rodava só manualmente (DEF-67). |
| Testes JVM | `unit-tests` | `testDebugUnitTest` em todos os módulos + as suítes explícitas de `feature/*` e `core:ui`. |
| Kover ≥ 60% | `unit-tests` | Bound de cobertura em `:core:domain` e `:core:data`. |
| **Verificador de escopo de cobertura** | `unit-tests` (**novo**) | As implementações de produção de `:core:data` precisam estar no denominador do Kover, e o padrão amplo `*_Impl` não pode voltar às exclusões (DEF-64). |
| Lint Android | `unit-tests` | `lintDebug` nos 8 módulos, com `MissingTranslation` como erro nas features e no app. |
| Build local + instrumentação | `local-integration` | APK debug e as suítes instrumentadas compilam; manifesto final e conteúdo do APK de testes verificados por script. |

### 5.1 O que mudou no escopo de cobertura

O padrão de exclusão `*_Impl` do Kover foi estreitado para
`*Dao_Impl*` e `*Database_Impl*` — que são exatamente as classes
**geradas pelo Room**.

A premissa do DEF-64 era que `*_Impl` esconderia
`ProjectRepositoryImpl` e `PasswordHasherImpl`. **Não esconde**: o
padrão exige um sublinhado, e nenhuma dessas classes tem um. O relatório
do Kover lista as duas (e `BackupRepositoryImpl`, `UserRepositoryImpl`,
`TaskRepositoryImpl`, `TagRepositoryImpl`, `HolidayRepositoryImpl`,
`ThemePreferencesRepositoryImpl`, `ListingPreferencesRepositoryImpl`,
`PasswordHasherImpl`, `SessionStore`) — todas no denominador do gate.

O que o padrão antigo faria é pegar de surpresa qualquer implementação
renomeada para `Algo_Impl`. Por isso o verificador
`.github/scripts/verify_coverage_scope.py` foi escrito: ele falha se
uma dessas classes sumir do relatório **ou** se o padrão amplo voltar
para a lista de exclusões.

```
verify_coverage_scope: OK — 10 classes de produção rastreadas presentes
no relatório; exclusões sem padrão amplo.
```

---

## 6. Verificação local (JDK 21)

| Verificação | Resultado |
|---|---|
| `testDebugUnitTest` (8 módulos) | **verde** |
| `:core:domain:koverVerify` / `:core:data:koverVerify` | **verde** (bound de 60%) |
| `ktlintCheck` | **verde** |
| `detekt` | **verde** |
| `lintDebug` (8 módulos) | **verde** |
| `:app:assembleDebug` | **verde** |
| `:core:data:assembleDebugAndroidTest` | **verde** |
| `:feature:auth:assembleDebugAndroidTest` | **verde** |
| `connectedDebugAndroidTest` (`:core:data`, `:feature:auth`) | **verde** no AVD Android 37 — 7 + 3 testes, zero falhas |

---

## 7. Limites desta verificação

- **Sem aparelho físico** — a execução instrumentada foi no emulador
  Android 17 (API 37).
- **A CI não executa a suíte instrumentada** — ela compila e verifica o
  APK; a execução depende de emulador. Um emulador na CI seria um
  custo alto para o ganho.
- **Medições de desempenho não foram feitas** — a issue pede medição e
  QA em aparelho "registrados separadamente dos testes JVM"; o que
  existe hoje é execução instrumentado real (registrada acima) e a
  suíte JVM. Nenhum número de performance foi medido.
- **A verificação de escopo de cobertura roda sobre o relatório XML**,
  que depende de `koverXmlReport` — o passo da CI gera o relatório
  antes de chamar o verificador.
- **Os defeitos de interface conhecidos (DEF-44 a DEF-57) continuam
  abertos** — esta issue não os fecha; a #213 e a #214 não alteraram
  o comportamento dessas telas.

Refs: R5, R11, E5.1
