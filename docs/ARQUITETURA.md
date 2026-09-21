# BrainOut — arquitetura

> Documento de definição arquitetural. Atualizado ao fim de cada ciclo
> (ver `ROADMAP.md`, marcos E3.1 e E5.1). Insumo direto para o item 2 da N1 e
> item 2 da N2 do documento norteador.

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
│   ├── data/             → Room, DataStore, repositórios, remote/ (E3.2)
│   │   └── remote/       → DTOs remotos, BrainOutApi (Retrofit), RemoteDataSource
│   └── ui/               → tema, tokens, componentes reutilizáveis
├── feature/
│   ├── auth/             → telas de login/cadastro
│   ├── projects/         → lista, criação, edição, dashboard
│   ├── tasks/            → CRUD de tarefas
│   └── settings/         → preferências, perfil, tema
└── backend-stub/         → FastAPI mínimo usado pelo CI (R6)
```

Dependências só apontam para baixo:

```
app → feature → core/{ui,data} → core/domain
```

`:core:domain` não depende de nenhum outro módulo, garantindo portabilidade
e testabilidade (regra R12 do documento norteador).

### 2.1 Camada remote (E3.2)

O subpacote `core/data/remote/` isola todo o acesso HTTP ao serviço de
retaguarda (decisão E3.1: backend próprio FastAPI):

```
┌─────────────────────────────────────────────────────────────┐
│ feature/* (ViewModels)                                      │
└──────────────┬──────────────────────────────────────────────┘
               │ portas de repositório (:core:domain)
┌──────────────▼──────────────────────────────────────────────┐
│ :core:data                                                  │
│  ├── local/       Room (DAOs, entidades, migrations)        │
│  ├── repository/  implementações das portas                 │
│  └── remote/      BrainOutApi (Retrofit) ← RemoteDataSource │
│         ↑ URL base injetada via BuildConfig.BASE_URL        │
│           (flavor dev: http://10.0.2.2:8000/ — emulador;    │
│            flavor prod: placeholder, ver decisão E3.1)      │
└──────────────┬──────────────────────────────────────────────┘
               │ HTTP/JSON (snake_case, contrato /v1/*)
┌──────────────▼──────────────────────────────────────────────┐
│ Backend FastAPI (backend-stub/ no CI; serviço próprio depois)│
└─────────────────────────────────────────────────────────────┘
```

- **DTOs remotos** (`RemoteDtos.kt`): espelham o contrato do backend com
  `@SerialName` snake_case; conversão para o domínio fica nos repositórios.
- **`BrainOutApi`**: interface Retrofit (`/v1/ping`, `/v1/projects`,
  `/v1/tasks`, `/v1/tags`) — nenhum host hard-coded; a URL vem do `BuildConfig`.
- **`RemoteDataSource`**: envolve a API, loga erros e re-sinaliza a
  exceção para o chamador (fila offline do E3.3 decide a estratégia).
- **Política de IDs**: o app gera UUID localmente (Room) e envia no
  body (`POST`) ou no path (`PUT`); o servidor respeita o ID recebido
  e nunca o substitui. Veja detalhes em `backend-stub/README.md` e na
  seção 5 (sincronização).
- **Testes**: `MockWebServer` exercita parse e erros HTTP sem rede real.

## 3. Stack técnica

| Camada                   | Tecnologia                                                   |
|--------------------------|--------------------------------------------------------------|
| Linguagem                | Kotlin (JDK 17)                                              |
| UI                       | Jetpack Compose + Material 3                                  |
| Navegação                | `androidx.navigation:navigation-compose`                     |
| Estado                   | `ViewModel` + `StateFlow` + Hilt                             |
| Injeção de dependência   | Hilt                                                         |
| Persistência local       | Room (KSP) + DataStore Preferences                           |
| Sincronização            | WorkManager + Ktor/Retrofit                                  |
| Notificações             | NotificationCompat + AlarmManager (recursos nativos, R8)    |
| Testes                   | JUnit4, Robolectric, Compose UI Test, MockWebServer, Turbine |
| Qualidade                | ktlint, detekt, Android Lint                                 |
| Build                    | Gradle 8.7, AGP 8.7, KSP                                     |
| Backend (R6)             | FastAPI em container (CI) → definido formalmente no E3.1     |

## 4. Persistência

- **Local:** Room com esquema versionado. Migrações escritas à mão
  (`Migration` objects) sempre que possível; `fallbackToDestructiveMigration`
  somente em debug. *Atende R5.*
- **Preferências:** `DataStore Preferences` (substituindo o legado
  `SharedPreferences`).
- **Remota:** cliente HTTP em `:core:data/remote/` (E3.2). Endpoints
  declarados na interface `BrainOutApi` (Retrofit) e expostos pela
  `RemoteDataSource` para isolar a implementação concreta. A URL base é
  injetada por flavor via `BuildConfig.BASE_URL` (dev: `10.0.2.2:8000`,
  prod: placeholder até a hospedagem definitiva — E3.1), com override
  opcional por desenvolvedor em `local.properties`
  (`brainout.baseUrl.dev`). *Atende R6.*

## 5. Sincronização e conectividade

- Fila de operações offline persistida em Room (`pending_ops`) —
  cada item guarda `entity_type`, `entity_id`, `op_type`
  (`INSERT|UPDATE|DELETE`), payload JSON serializado, `created_at`
  e contador de `attempts`.
- `WorkManager` com `Constraints.NetworkType.CONNECTED` reconcilia
  periodicamente (`enqueueUniquePeriodicWork`, 15 min) e imediatamente
  após retorno de rede. Backoff exponencial entre tentativas.
- Estratégia **last-writer-wins** com timestamp do cliente; conflitos
  detectados são registrados em log local para revisão.
- *Atende R5 e R6.*

### 5.1 Contrato do backend para sincronização

O stub em `backend-stub/` (e o backend FastAPI definitivo, AD-3)
expõe o contrato abaixo. **Política de IDs: cliente-supplied UUID** —
o app gera o UUID localmente e envia no `POST`/`PUT`; o servidor
respeita o ID recebido, garantindo idempotência de replay e
estabilidade das foreign keys (project_id ↔ tag, project_id ↔ task).

| Método | Caminho                          | Função                                       |
|--------|----------------------------------|----------------------------------------------|
| GET    | `/v1/projects`                   | Lista projetos                               |
| POST   | `/v1/projects`                   | Cria projeto (id do cliente opcional)        |
| GET    | `/v1/projects/{id}`              | Busca projeto                                |
| PUT    | `/v1/projects/{id}`              | **Upsert idempotente**                       |
| DELETE | `/v1/projects/{id}`              | Remove projeto (cascade de tasks) — **204**  |
| POST   | `/v1/projects/{id}/tags`         | Associa tag existente                        |
| GET    | `/v1/projects/{id}/tags`         | Lista tags do projeto                        |
| GET    | `/v1/tasks`                      | Lista tarefas (filtro `?project_id=`)        |
| POST   | `/v1/tasks`                      | Cria tarefa (id do cliente opcional)         |
| PUT    | `/v1/tasks/{id}`                 | **Upsert idempotente**                       |
| DELETE | `/v1/tasks/{id}`                 | Remove tarefa — **204**                      |
| GET    | `/v1/tags`                       | Lista tags                                   |
| POST   | `/v1/tags`                       | Cria tag (id gerado no servidor)             |
| DELETE | `/v1/tags/{id}`                  | Remove tag — **204**                         |

Regras:

- `POST` com `id` no corpo: se já existir registro com esse `id`, o
  servidor devolve o registro existente (não duplica).
- `PUT` é upsert: cria se ausente, substitui se presente; `created_at`
  é fixado na primeira inserção e preservado em updates.
- `DELETE` é intencionalmente idempotente (204 mesmo ausente) para
  não travar o replay da fila offline.
- IDs mal formados (não-UUID) → `400`. `id` do body divergente do
  path no `PUT` → `400`.

A suíte pytest em `backend-stub/tests/test_contract.py` cobre esses
casos; o smoke HTTP roda no job `backend-integration` do CI.

## 6. Segurança

- Tokens armazenados em `EncryptedSharedPreferences` (Tink) ou
  `androidx.security:security-crypto`. **Nunca** em texto plano nem
  versionados. *Atende R12.*
- Tráfego de rede em produção: HTTPS obrigatório. *Atende R7.*
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
| AD-3 | Backend: FastAPI próprio (evolução do `backend-stub/`)                | 21/09/2026 |
| AD-4 | Estratégia de sincronização: last-writer-wins + fila offline         | a definir  |
| AD-5 | Criptografia de tokens com `androidx.security:security-crypto`       | a definir  |

## 9. Serviço de retaguarda — decisão (E3.1)

A definição da plataforma definitiva do serviço de retaguarda (R6)
constitui o marco E3.1 do Ciclo 3. Três alternativas foram avaliadas:
**Firebase** (BaaS da Google, modelo NoSQL em tempo real), **Supabase**
(BaaS open source sobre PostgreSQL) e **backend próprio** (API REST
dedicada, já representada neste repositório pelo stub FastAPI em
`backend-stub/`, usado pelo pipeline de integração contínua). A tabela a
seguir resume a comparação segundo os critérios relevantes ao projeto.

### 9.1 Análise comparativa

| Critério                        | Firebase                                                                 | Supabase                                                                 | Backend próprio (FastAPI)                                                       |
|---------------------------------|--------------------------------------------------------------------------|--------------------------------------------------------------------------|----------------------------------------------------------------------------------|
| Custo                           | Camada gratuita limitada; cobrança por leituras/escritas pode escalar     | Camada gratuita generosa; cobrança por projeto ativo após o limite         | Zero — hospedado em VM própria na rede Tailscale                                  |
| Controle de dados               | Baixo: dados residem em infraestrutura de terceiros, fora do domínio acadêmico | Médio: opção self-hosted existe, porém a configuração gerenciada é o caminho comum | Alto: esquema, retenção e cópias de segurança sob controle integral do autor     |
| Esforço de integração           | Baixo: SDKs nativos, porém acoplam a camada de dados ao fornecedor        | Médio: cliente PostgreSQL/REST; modelagem distinta do contrato REST já definido | Baixo: contrato REST (`/v1/projects`, `/v1/tasks`) já implementado e exercitado no CI |
| Curva de aprendizado            | Média: modelo de documentos e regras de segurança proprietárias           | Média: exige familiaridade com PostgreSQL e Row Level Security             | Baixa: Python + FastAPI com tipagem Pydantic, alinhados à formação do autor       |
| Deploy e operação               | Gerenciado pelo fornecedor; sem controle de versão implantado             | Gerenciado (nuvem) ou manual (self-hosted via Docker)                      | Container Docker simples, já empacotado no CI; implantação em VM caseira via Tailscale |

### 9.2 Decisão recomendada

A alternativa recomendada é o **backend próprio com FastAPI**. A
justificativa central é o controle de dados: a aplicação trata dados de
autenticação e perfis com permissões distintas (R2), e manter esses
dados em infraestrutura sob o próprio domínio elimina a dependência de
fornecedores externos e o compartilhamento de informações de usuário com
terceiros. O modelo de dados relacional já consolidado em Room
(users/projects/tasks/tags) mapeia diretamente para um backend
relacional, sem a tradução para documentos exigida pelo Firebase.

O critério econômico corrobora a decisão: Firebase e Supabase implicam
custos recorrentes à medida que o volume de sincronização cresce,
enquanto um container FastAPI implantado em VM própria, acessível pela
VPN Tailscale, opera a custo zero e mantém o tráfego fora da internet
pública. Ademais, parte significativa do esforço de integração já foi
realizada: o stub FastAPI em `backend-stub/` roda no CI com o contrato
REST de projetos e tarefas, de modo que a evolução do stub para o
serviço definitivo consiste em substituir a persistência em memória por
PostgreSQL, preservando o contrato de endpoints consumido pelo cliente
Android (E3.2).

Por fim, a decisão preserva a testabilidade e a portabilidade exigidas
pela arquitetura multi-módulo: o cliente HTTP permanece isolado em
`:core:data/network/` atrás de `RemoteDataSource`, e o mesmo contrato
REST é exercido localmente pelo stub no CI. Não há, portanto, perda de
capacidade de teste em relação às alternativas gerenciadas — apenas a
responsabilidade de operação, assumida conscientemente pelo autor como
parte do escopo de aprendizado do projeto integrador.

| AD-6 | Lembretes de prazo via WorkManager (`OneTimeWorkRequest` + `setInitialDelay`), sem `SCHEDULE_EXACT_ALARM` (E3.6) | 21/09/2026 |

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
| **WorkManager** (`OneTimeWorkRequest` + `setInitialDelay`) | Sem permissão adicional; sobrevive a reboot/process death; integrado com a estratégia de sincronização do E3.3; testável via `work-testing` | Janela de tolerância (disparo tipicamente dentro de poucos minutos do horário programado) |

A escolha foi **WorkManager**, pela consistência com o E3.3 (fila de
sincronização no mesmo executor), pela ausência de permissões extras no
Android 13+ e pela janela de tolerância aceitável para lembretes
acadêmicos de prazo ("prazo em 1 hora" com deriva de minutos). A
exatidão de segundo é desnecessária para o domínio. *Critério do marco
atendido sem `SCHEDULE_EXACT_ALARM`.*

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

João Pedro G M Silva - PUC Goiás ADS - 20251012000740
