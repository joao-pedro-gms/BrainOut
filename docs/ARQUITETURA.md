# BrainOut — arquitetura

> Documento de definição arquitetural. Atualizado ao fim de cada ciclo
> (ver `ROADMAP.md`, marcos E3.1 e E5.1). Insumo direto para o item 2 da N1 e
> item 2 da N2 do documento norteador.
>
> **Escopo vigente (revisado em 2026-10-07): aplicativo totalmente local.**
> O BrainOut não possui backend, serviço de retaguarda, API externa, conta
> remota, fila de sincronização nem colaboração entre dispositivos. Os dados
> vivem exclusivamente no banco Room do próprio aparelho, e a **única**
> transferência de dados prevista — cópia de segurança ou mudança de
> instalação — é o **arquivo de backup exportável/restaurável** (contrato em
> definição no cartão BO-06). As seções que descreviam o escopo anterior de
> backend/sincronização/integração externa permanecem registradas como
> **histórico/superseded** nas decisões (Seção 8) e na Seção 9.

## 1. Decisão de plataforma

A escolha recai sobre **aplicativo Android nativo**, com **Kotlin** como
linguagem principal e **Jetpack Compose** para a camada de interface,
conforme admitido pelo item 4 do documento norteador (abordagem "Nativa
Android"). Justificativas:

- **Mercado e ecossistema:** maior oferta de vagas e de bibliotecas
  oficiais mantidas pelo Google/AndroidX.
- **Adequação ao domínio:** o app exige uso de recursos nativos
  (notificações, sensores, persistência), onde o caminho nativo é mais
  direto que wrappers multiplataforma.
- **Curva de aprendizado:** Kotlin é exigência do curso; Compose é
  recomendado pelo item 4 do documento norteador.
- **Custo:** ferramentas e SDKs gratuitos para uso individual.

## 2. Camadas e módulos

Estrutura multi-módulo Gradle:

```
BrainOut/
├── app/                  → entry point, Application, MainActivity
├── core/
│   ├── domain/           → entidades, regras de domínio, use cases
│   ├── data/             → Room (persistência local), DataStore e repositórios
│   └── ui/               → tema (Color/Shape/Theme/Type); tokens e componentes são proposta
├── feature/
│   ├── auth/             → splash, login e cadastro
│   ├── projects/         → lista/busca/tags de projetos e detalhe com tarefas
│   ├── tasks/            → lista global e painel
│   └── settings/         → lista de preferências (Perfil/Tema/Notificações ainda placeholders)
```

Dependências só apontam para baixo:

```
app → feature → core/{ui,data} → core/domain
```

`:core:domain` não depende de nenhum outro módulo, garantindo portabilidade
e testabilidade (regra R12 do documento norteador).

### 2.1 Persistência local e fluxo de dados

O app não tem camada de rede: toda leitura e escrita de dados passa pelo
Room, dentro de `:core:data`, atrás das portas de repositório declaradas em
`:core:domain`:

```
┌─────────────────────────────────────────────────────────────┐
│ feature/* (ViewModels)                                      │
└──────────────┬──────────────────────────────────────────────┘
               │ portas de repositório (:core:domain)
┌──────────────▼──────────────────────────────────────────────┐
│ :core:data                                                  │
│  ├── local/       Room (DAOs, entidades, migrations)        │
│  ├── datastore/   DataStore Preferences (sessão, listagem)  │
│  └── repository/  implementações das portas                 │
└─────────────────────────────────────────────────────────────┘
```

- **Fluxo observável:** as DAOs devolvem `Flow`; os repositórios mapeiam as
  linhas para modelos de domínio; os ViewModels combinam os fluxos em um
  `UiState` imutável exposto por
  `stateIn(..., WhileSubscribed(5_000), ...)`.
- **Mutações atômicas:** operações com efeito colateral são resolvidas em uma
  única transação Room (`@Transaction` nos DAOs, ex. cascata de conclusão do
  projeto). Não há escrita dupla local+fila.
- **Sem I/O de rede:** nenhum fluxo da UI depende de conectividade. O app
  abre, lê e grava em modo avião.

### 2.2 Transferência de dados para fora do dispositivo

A única saída de dados é o **arquivo de backup** gerado pelo próprio app e
restaurado pelo próprio app. Não há servidor, endpoint, conta remota nem
telemetria. O formato, a versão e as garantias de restauração (validação antes
da escrita, atomicidade, rollback) constituem o escopo do cartão **BO-06**
(contrato versionado) e dos cartões de implementação subsequentes.

## 3. Stack técnica

| Camada                   | Tecnologia                                                   |
|--------------------------|--------------------------------------------------------------|
| Linguagem                | Kotlin (JDK 17)                                              |
| UI                       | Jetpack Compose + Material 3                                  |
| Navegação                | `androidx.navigation:navigation-compose`                     |
| Estado                   | `ViewModel` + `StateFlow` + Hilt                             |
| Injeção de dependência   | Hilt                                                         |
| Persistência local       | Room (KSP) + DataStore Preferences                           |
| Agendamento local        | WorkManager (lembretes de prazo — AD-6)                       |
| Notificações             | NotificationCompat + WorkManager (recursos nativos, R8)     |
| Testes                   | JUnit4, Robolectric, Compose UI Test, Turbine                |
| Qualidade                | ktlint, detekt, Android Lint                                 |
| Build                    | Gradle 9.7.1, AGP 9.4.1, KSP                                 |

Não há linha de "Sincronização" nem de "Backend": ambos pertenciam ao escopo
anterior de serviço de retaguarda, revogado (ver Seção 8, AD-3/AD-4).

## 4. Persistência

- **Local:** Room com esquema versionado. Migrações escritas à mão
  (`Migration` objects) sempre que possível; `fallbackToDestructiveMigration`
  somente em debug. *Atende R5.*
- **Preferências:** `DataStore Preferences` (substituindo o legado
  `SharedPreferences`).
- **Backup:** a exportação/restauração do arquivo de backup é o único
  mecanismo de transferência de dados do app e não usa rede. *Substitui o
  antigo requisito R6 de persistência remota*, que saiu do escopo.

## 5. Conectividade e lembretes locais

- **Conectividade:** o app não observa nem exige rede. Nenhuma tela exibe
  banner de offline, fila de operações ou estado de sincronização, porque não
  existe operação remota a reconciliar.
- **Lembretes de prazo:** o único uso de trabalho em background é a
  notificação local de prazo (`WorkManager`, Seção 10). Ela é agendada e
  cancelada a partir de regras de domínio, sem qualquer dependência de rede.
- **Registro histórico:** o escopo anterior previa uma fila de operações
  offline (`pending_ops`) drenada por um `SyncWorker` contra um serviço
  FastAPI, com reconciliação automática ao retorno da rede. Essa
  infraestrutura saiu do escopo (cartão **BO-02**); o texto anterior permanece
  como registro nas decisões revogadas da Seção 8.

## 6. Segurança

- **Credenciais locais:** senhas de contas locais são persistidas apenas como
  hash (`PasswordHasher`, com iterações persistidas por registro); nunca em
  texto plano nem versionadas. *Atende R12.*
- **Dados locais:** o banco Room e as preferências ficam no sandbox do app; o
  arquivo de backup é o único artefato que pode sair do dispositivo, e seu
  tratamento (integridade, escopo, credenciais) é escopo do cartão **BO-09**.
- **Sem tráfego de rede:** como o app não faz requisições, não há TLS, token
  ou host a proteger.
- Permissões runtime solicitadas no momento do uso (Android 6+),
  justificando cada uma no `AndroidManifest.xml`.

## 7. Acessibilidade

- `contentDescription` em todos os elementos visuais não textuais.
- Áreas de toque ≥ 48dp.
- Contraste AA verificado com Accessibility Scanner.
- Suporte a TalkBack nos fluxos principais. *Atende R11.*

## 8. Decisões registradas

| ID   | Decisão                                                              | Data       |
|------|----------------------------------------------------------------------|------------|
| AD-1 | Multi-módulo Gradle conforme Seção 2                                 | a definir  |
| AD-2 | Stack conforme Seção 3                                               | a definir  |
| AD-3 | ~~Backend: FastAPI próprio (evolução do `backend-stub/`)~~ — **revogada**: escopo local, sem backend | 21/09/2026 (revogada em 2026-10-07) |
| AD-4 | ~~Estratégia de sincronização: last-writer-wins + fila offline~~ — **revogada**: sem sincronização | a definir (revogada em 2026-10-07) |
| AD-5 | ~~Criptografia de tokens com `androidx.security:security-crypto`~~ — **revogada**: não há tokens nem rede | a definir (revogada em 2026-10-07) |
| AD-6 | Lembretes de prazo via WorkManager (`OneTimeWorkRequest` + `setInitialDelay`), sem `SCHEDULE_EXACT_ALARM` (E3.6) | 21/09/2026 |
| AD-7 | **Escopo totalmente local:** sem backend, API externa, conta remota ou sincronização; a única transferência de dados é o arquivo de backup exportável/restaurável | 2026-10-07 |

AD-3, AD-4 e AD-5 descreviam um produto com serviço de retaguarda. Foram
revogadas pela decisão AD-7; o registro permanece para rastrear a mudança de
escopo.

## 9. Serviço de retaguarda — decisão revogada (E3.1, histórico)

**Histórico/superseded (2026-10-07).** O marco E3.1 do Ciclo 3 escolheu, entre
Firebase, Supabase e um backend próprio, o **backend próprio em FastAPI**
(evoluindo o stub `backend-stub/`), com a justificativa publicada em ata
(`docs/ATAS/checkpoint2.md`). Essa decisão pressupunha um produto com
sincronização remota. Com o escopo vigente de **aplicativo totalmente local**
(AD-7), não há serviço de retaguarda: a decisão está revogada e o diretório
`backend-stub/` saiu do escopo (cartão BO-02). A ata do Checkpoint 2 e este
registro permanecem como documentação histórica, sem reescrita.

## 10. Notificações locais — lembretes de prazo (E3.6)

O recurso nativo do Ciclo 3 (R8) é o lembrete local de prazo: quando
uma [Task] possui `dueDate` futura, o app agenda uma notificação para
disparar **1 hora antes** do prazo, com as ações "Concluir" e
"Dispensar".

### 10.1 Escolha do mecanismo de agendamento (AD-6)

Duas alternativas foram avaliadas:

| Alternativa | Prós | Contras |
|-------------|------|---------|
| `AlarmManager.setExactAndAllowWhileIdle` | Precisão de segundos | Exige `SCHEDULE_EXACT_ALARM`/`USE_EXACT_ALARM` (restrita no Android 13+, revogável, exige intenção do usuário nas Configurações); `setExactAndAllowWhileIdle` não dispara em Doze sem permissão especial |
| **WorkManager** (`OneTimeWorkRequest` + `setInitialDelay`) | Sem permissão adicional; sobrevive a reboot/process death; reutiliza o executor já presente no app; testável via `work-testing` | Janela de tolerância (disparo tipicamente dentro de poucos minutos do horário programado) |

A escolha foi **WorkManager**, pela ausência de permissões extras no Android
13+, pela resiliência a reboot/process death e pela janela de tolerância
aceitável para lembretes acadêmicos de prazo ("prazo em 1 hora" com deriva de
minutos). A exatidão de segundo é desnecessária para o domínio. *Critério do
marco atendido sem `SCHEDULE_EXACT_ALARM`.*

### 10.2 Componentes

| Componente | Papel |
|------------|-------|
| `DeadlineNotificationScheduler` (`:core:domain`) | Porta do domínio (interface): `schedule(taskId, triggerAt)` / `cancel(taskId)`; constante `REMINDER_LEAD = 1h` |
| `WorkManagerDeadlineScheduler` (`:app`) | Implementação WorkManager; trabalho único `deadline-<taskId>` com política `REPLACE`; cria o canal em `ensureChannel()` |
| `DeadlineWorker` (`:app`, `@HiltWorker`) | Recarrega a Task do Room no disparo (não notifica tarefa concluída/deletada — evita lembrete fantasma), resolve o nome do projeto e publica a notificação no canal `brainout_deadlines` (importância HIGH) com ações "Concluir"/"Dispensar" |
| `DeadlineReceiver` (`:app`, `BroadcastReceiver`, não exportado) | Botão "Concluir" enfileira o `CompleteTaskWorker` (a mudança de status acontece **via WorkManager**, conforme critério do ROADMAP); cancela o trabalho pendente e a notificação; usa `goAsync()` + coroutine |
| `CompleteTaskWorker` (`:app`, `@HiltWorker`) | Executa `ChangeTaskStatusUseCase(taskId, DONE)`; falha vira `retry` (backoff do WorkManager); tarefa inexistente é idempotente |
| `NotificationPermissionStore` (`:app`) | Flag "permissão já pedida" persistida em `SessionStore` (DataStore) para não insistir após negativa |

### 10.3 Fluxo de dados

- **Criação/edição** (`CreateTaskUseCase` / `UpdateTaskUseCase`) e
  **mudança de status** (`ChangeTaskStatusUseCase`) reconciliam o
  lembrete: tarefa ativa com prazo futuro → agenda para
  `dueDate − 1h`; tarefa `DONE`, sem prazo ou com prazo no passado →
  cancela. Exclusão (`DeleteTaskUseCase`) também cancela.
- O reconciliar vive no domínio (`:core:domain`) — o Android fica
  isolado na implementação da porta, preservando a regra R12.
- **Permissão** (`POST_NOTIFICATIONS`, Android 13+): pedida em
  `MainActivity` na primeira composição via
  `rememberLauncherForActivityResult`; negativa exibe toast
  explicativo e não é repetida (flag em DataStore).
- Sem `SCHEDULE_EXACT_ALARM` no manifest — intencional (AD-6).

## 11. Regras de prazo — dias úteis e feriados

O app sinaliza prazos que caem em fim de semana ou feriado nacional e sugere
antecipação para o último dia útil anterior. A regra vive no domínio
(`CheckDeadlineUseCase`, `DeadlineInfo`, `Holiday`) e é exibida pelo
`DeadlineField` no diálogo de nova tarefa.

| Componente | Módulo | Papel |
| --- | --- | --- |
| `Holiday` | `:core:domain/model/` | Modelo de domínio imutável (`date`, `name`, `type`) |
| `HolidayRepository` | `:core:domain/repository/` | Porta de domínio: `suspend fun getHolidays(year)` |
| `CheckDeadlineUseCase` | `:core:domain/usecase/` | Janela `[prazo − 7d, prazo]`, inclusive; cruza dezembro/janeiro buscando os dois anos; retorna `DeadlineInfo(isBusinessDay, nextHoliday)` |
| `HolidayRepositoryImpl` | `:core:data/repository/` | Implementação da porta; origem dos feriados definida pelo cartão **BO-03** |
| `DeadlineField` | `:feature:projects` | Componente Compose extraído do `NewTaskDialog`: botão de prazo, picker Material 3, dica inline de feriado e aviso de "dia não útil" |

### 11.1 Origem dos dados de feriado — em revisão (BO-03)

A implementação atual atende a porta `HolidayRepository` consultando a
**BrasilAPI** (`https://brasilapi.com.br/api/feriados/v1/{year}`), um serviço
externo. O escopo vigente não admite API externa, de modo que essa dependência
precisa ser substituída por dados locais (base de feriados embarcada/derivada)
ou a regra precisa ser redefinida — decisão do cartão **BO-03**. A regra de
negócio (`CheckDeadlineUseCase`) e o componente de UI permanecem; o que muda é
a fonte de dados. Enquanto a revisão não conclui, o comportamento atual degrada
de forma segura: falha na consulta cai em `unavailable=true`, o `DeadlineField`
omite a dica e o usuário salva o prazo normalmente.

João Pedro G M Silva - PUC Goiás ADS - 20251012000740
