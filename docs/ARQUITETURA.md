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
  `/v1/tasks`) — nenhum host hard-coded; a URL vem do `BuildConfig`.
- **`RemoteDataSource`**: envolve a API, loga erros e re-sinaliza a
  exceção para o chamador (fila offline do E3.4 decide a estratégia).
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

- Fila de operações offline persistida em Room (`pending_operations`).
- `WorkManager` com `Constraints.NetworkType.CONNECTED` reconcilia
  periodicamente e imediatamente após retorno de rede.
- Estratégia **last-writer-wins** com timestamp do cliente; conflitos
  detectados são registrados em log local para revisão.
- *Atende R5 e R6.*

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

João Pedro G M Silva - PUC Goiás ADS - 20251012000740
