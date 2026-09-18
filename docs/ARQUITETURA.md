# BrainOut — arquitetura

> Documento de definição arquitetural. Atualizado ao fim de cada ciclo
> (ver `ROADMAP.md`, marcos E3.1 e E5.1). Insumo direto para o item 2 da N1 e
> item 2 da N2 do documento norteador.

## 1. Decisão de plataforma

A equipe opta por **aplicativo Android nativo**, com **Kotlin** como
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
- **Custo:** ferramentas e SDKs gratuitos para a equipe.

## 2. Camadas e módulos

Estrutura multi-módulo Gradle:

```
BrainOut/
├── app/                  → entry point, Application, MainActivity
├── core/
│   ├── domain/           → entidades, regras de domínio, use cases
│   ├── data/             → Room, DataStore, repositórios, fontes remotas
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
- **Remota:** cliente HTTP em `:core:data/network/`. Endpoints consumidos
  via interface `RemoteDataSource` para isolar a implementação concreta.
  *Atende R6.*

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
| AD-3 | Backend: substituir `backend-stub/` pela plataforma definitiva no E3.1 | a definir  |
| AD-4 | Estratégia de sincronização: last-writer-wins + fila offline         | a definir  |
| AD-5 | Criptografia de tokens com `androidx.security:security-crypto`       | a definir  |