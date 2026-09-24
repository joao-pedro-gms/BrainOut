<!-- João Pedro G M Silva - PUC Goiás ADS - 20251012000740 -->
# core/data — AGENTS

## OVERVIEW
Camada de persistência (Room), rede (Retrofit), preferências (DataStore), sessão/segurança e sync; única origem do `BASE_URL`/`HOLIDAYS_BASE_URL` por flavor; gate Kover 60%.

## STRUCTURE
```
core/data/
├── build.gradle.kts            # flavors dev/prod + BuildConfig.BASE_URL/HOLIDAYS_BASE_URL
├── consumer-rules.pro
├── schemas/                    # schemas Room exportados (versionados)
│   └── pucgo.joaopedrogmsilva.brainout.core.data.local.BrainOutDatabase/
│       ├── 1.json
│       ├── 2.json
│       ├── 3.json
│       └── 4.json
└── src/
    ├── main/kotlin/pucgo/joaopedrogmsilva/brainout/core/data/
    │   ├── PackageMarker.kt
    │   ├── di/         DataModule.kt
    │   ├── local/      BrainOutDatabase, Migrations + converter/ + dao/ + entity/
    │   ├── remote/     BrainOutApi, RemoteDataSource, RemoteDtos, HolidayApi, HolidayDto, HolidayRemoteDataSource
    │   ├── repository/ *RepositoryImpl (User/Project/Task/Tag/Holiday)
    │   ├── security/   PasswordHasherImpl, PepperProvider
    │   ├── session/    SessionStore, ActiveUserProvider
    │   ├── sync/       SyncDispatcher, BrainOutSyncDispatcher, PendingSyncMonitor, ConnectivityObserver
    │   └── preferences/ ListingPreferencesRepositoryImpl
    ├── test/           # JVM + Robolectric
    └── androidTest/    # UserDaoInstrumentedTest, MigrationTest
```

## WHERE TO LOOK
| Tarefa | Onde |
|--------|------|
| Novo DAO/entity | `local/dao/` + `local/entity/` + exportar schema (commit em `schemas/...`) |
| Nova migration | `local/Migrations.kt` (manual; bump `@Database(version=…)`) |
| Novo endpoint `/v1/*` | `remote/BrainOutApi.kt` + `remote/RemoteDataSource.kt` + DTOs em `RemoteDtos.kt` |
| Feriado (BrasilAPI) | `remote/HolidayApi.kt` + `HolidayRemoteDataSource.kt` (cache em memória + mutex) + `repository/HolidayRepositoryImpl.kt` |
| Preferences de listagem | `preferences/ListingPreferencesRepositoryImpl.kt` |
| Sync/fila offline | `sync/BrainOutSyncDispatcher.kt` consumindo `PendingOpDao` (queue `pending_ops` INSERT/UPDATE/DELETE, last-writer-wins por `updated_at`) |
| Wiring Hilt | `di/DataModule.kt` (bind portas de `:core:domain` → `*Impl`) |

## CONVENTIONS
- Room: `room.schemaLocation` em `core/data/schemas` (JSONs 1–4 commitados; revisar diff em PR).
- Migrations: escritas à mão em `Migrations.kt`; bump de versão sincroniza JSON exportado.
- DTOs Retrofit: campos em `snake_case` via `@SerialName("…")`; conversão para modelo de domínio acontece no `RemoteDataSource`, **nunca** no DAO/entity.
- `Instant` ↔ coluna `TEXT`: via `local/converter/InstantConverter.kt` (Kotlinx Serialization `Instant.parse`).
- URL base: **somente** via `BuildConfig.BASE_URL` / `BuildConfig.HOLIDAYS_BASE_URL` (BrasilAPI); override por `local.properties` (`brainout.baseUrl.dev=…`). Nada hard-coded.
- Identidade de registros: **UUID cliente-supplied** (R6). Upsert idempotente em `BrainOutApi`.
- Sync: `pending_ops` armazena `op` (INSERT/UPDATE/DELETE), `entity`, `payloadJson`, `updated_at`; conflito resolvido por **last-writer-wins** no servidor.
- Header `// João Pedro G M Silva - PUC Goiás ADS - 20251012000740` em todo `.kt`.
- Pacotes minúsculos (regra detekt).

## TESTS
- **JVM (`src/test/`)** — Robolectric para DAOs com `Instant`/`TEXT`; MockWebServer para Retrofit (`BrainOutApi`, `HolidayApi`).
- **androidTest (`src/androidTest/`)** — `UserDaoInstrumentedTest` (CRUD real), `MigrationTest` (1→4 no emulador).
- **Gate Kover 60%** verificado via `./gradlew :core:data:koverVerify` (exclusions no root: `*_Impl`, `Hilt_*`, `*.remote.*`, `*.di.*`).
- **Gap**: `TaskRepositoryImpl`, `ProjectRepositoryImpl`, `TagRepositoryImpl` ainda **sem** testes diretos — adicionar `*RepositoryTest` (MockWebServer + DAO fake) ao cobrir bug/nova feature.

## ANTI-PATTERNS
- **Não** usar `fallbackToDestructiveMigration` em release — escrever `Migration` à mão.
- **Não** hard-code host (`10.0.2.2`, IP LAN, `localhost`) fora de `BuildConfig`.
- **Não** acoplar `:core:domain` a Room/Retrofit/DataStore — `repository/*Impl` bind em portas de domínio; inversão de dependência.
- **Não** importar `*Impl` de fora de `:core:data` — consumir via porta (`@Inject constructor` em `Impl` + `bind` no `DataModule`).
- **Não** mover WorkManager/sync Workers para `feature/*` — glue fica em `:app`.
- **Não** deixar schema JSON fora do repo — versionar junto do bump.
- **Não** escrever em `dao/` lógica de negócio (mapeamentos ficam em `RemoteDataSource` ou `Repository`).
