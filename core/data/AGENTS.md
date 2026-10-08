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
    │   ├── repository/ *RepositoryImpl (User/Project/Task/Tag/Holiday/ListingPreferences)
    │   ├── security/   PasswordHasherImpl, PepperProvider (+ @Module object Factory)
    │   ├── session/    SessionStore, ActiveUserProvider
    │   ├── sync/       SyncDispatcher, BrainOutSyncDispatcher, PendingSyncMonitor, ConnectivityObserver
    │   └── preferences/ ListingPreferencesRepositoryImpl
    ├── test/           # JVM + Robolectric — 19 arquivos, inclui os 4 *RepositoryImplTest
    └── androidTest/    # UserDaoInstrumentedTest, MigrationTest (compilam; rodam em `connectedDevDebugAndroidTest`)
```

## WHERE TO LOOK
| Tarefa | Onde |
|--------|------|
| Novo DAO/entity | `local/dao/` + `local/entity/` + exportar schema (commit em `schemas/...`) |
| Nova migration | `local/Migrations.kt` (manual; bump `@Database(version=…)`) + teste em `androidTest/.../MigrationTest.kt` |
| Novo endpoint `/v1/*` | `remote/BrainOutApi.kt` + `remote/RemoteDataSource.kt` + DTOs em `RemoteDtos.kt` |
| Feriado (BrasilAPI) | `remote/HolidayApi.kt` + `HolidayRemoteDataSource.kt` (cache em memória + mutex) + `repository/HolidayRepositoryImpl.kt` |
| Preferences de listagem | `preferences/ListingPreferencesRepositoryImpl.kt` (chave canônica = `SortOrder.toStorageKey()`, ver root) |
| Sync/fila offline | `sync/BrainOutSyncDispatcher.kt` consumindo `PendingOpDao` (queue `pending_ops` INSERT/UPDATE/DELETE, last-writer-wins por `updated_at`) |
| Wiring Hilt | `di/DataModule.kt` (bind portas de `:core:domain` → `*Impl`) **+** `security/PepperProvider.kt` (`PepperProvider.Factory`, linhas 77-83) |

> **A segunda fonte de DI.** Não é só o `DataModule`: `PepperProvider.Factory` é um `@Module @InstallIn(SingletonComponent::class)` próprio, dentro do arquivo de `security/`, que fornece o pepper ao `PasswordHasher`. Ao adicionar binding, decida conscientemente entre os dois (o `DataModule` para portas/DAOs/infra; um `@Module` dedicado para componente autocontido).

## CONVENTIONS
- Room: `room.schemaLocation` em `core/data/schemas` (JSONs 1–4 commitados; revisar diff em PR).
- Migrations: escritas à mão em `Migrations.kt` (`MIGRATION_1_2`, `MIGRATION_2_3`, `MIGRATION_3_4`); bump de versão sincroniza JSON exportado.
- DTOs Retrofit: campos em `snake_case` via `@SerialName("…")`; conversão para modelo de domínio acontece no `RemoteDataSource`, **nunca** no DAO/entity.
- `Instant` ↔ coluna `TEXT`: via `local/converter/InstantConverter.kt` (Kotlinx Serialization `Instant.parse`).
- URL base: **somente** via `BuildConfig.BASE_URL` / `BuildConfig.HOLIDAYS_BASE_URL` (BrasilAPI); override por `local.properties` (`brainout.baseUrl.dev=…`). Nada hard-coded.
- Identidade de registros: **UUID cliente-supplied** (R6). Upsert idempotente em `BrainOutApi`.
- Sync: `pending_ops` armazena `op` (INSERT/UPDATE/DELETE), `entity`, `payloadJson`, `updated_at`; conflito resolvido por **last-writer-wins** no servidor.
- **Dispatchers:** os repositórios **não** usam `withContext(Dispatchers.IO)` — `grep 'Dispatchers.IO' core/data/src/main` retorna zero. Os DAOs `suspend` do Room já rodam fora da main thread. Não introduza troca de dispatcher explícita aqui.
- Header `// João Pedro G M Silva - PUC Goiás ADS - 20251012000740` em todo `.kt`.
- Pacotes minúsculos (regra detekt).

## TESTES
- **JVM (`src/test/`, 19 arquivos)** — Robolectric para DAOs com `Instant`/`TEXT`; MockWebServer para Retrofit (`BrainOutApi`, `HolidayApi`).
- **androidTest (`src/androidTest/`, 2 arquivos)** — `UserDaoInstrumentedTest` (CRUD real) e `MigrationTest`, que cobre as **3 migrations individualmente (1→2, 2→3, 3→4) e a cadeia completa 1→4** via `MigrationTestHelper.runMigrationsAndValidate`. Ambas compilam (`:core:data:assembleDevDebugAndroidTest`); executar exige emulador (`./gradlew connectedDevDebugAndroidTest`).
- **Gate Kover 60%** verificado via `./gradlew :core:data:koverVerify` (exclusions no root: `*_Impl`, `Hilt_*`, `*.remote.*`, `*.di.*`, `*.PackageMarker`).
- **Repositories já têm testes diretos.** `TaskRepositoryImplTest.kt`, `ProjectRepositoryImplTest.kt`, `TagRepositoryImplTest.kt` e `UserRepositoryImplTest.kt` existem em `src/test/.../repository/`, com o padrão `Fake*Dao`. A lacuna real é de **caso**, não de arquivo: `HolidayRepositoryImpl` só aparece via `remote/HolidayRepositoryTest.kt`, e `ListingPreferencesRepositoryTest` cobre o caminho feliz (11 casos, incluindo isolamento por usuário) mas não a leitura de uma chave gravada por versão anterior — a compatibilidade da grafia legada é testada no domínio, em `SortOrderStorageKeyTest`.

## ANTI-PATTERNS
- **Não** usar `fallbackToDestructiveMigration` em release — escrever `Migration` à mão.
- **Não** hard-code host (`10.0.2.2`, IP LAN, `localhost`) fora de `BuildConfig`.
- **Não** acoplar `:core:domain` a Room/Retrofit/DataStore — `repository/*Impl` bind em portas de domínio; inversão de dependência.
- **Não** importar `*Impl` de fora de `:core:data` — consumir via porta (`@Inject constructor` em `Impl` + `bind` no `DataModule`).
- **Não** mover WorkManager/sync Workers para `feature/*` — glue fica em `:app` (`app/src/main/.../sync/`). Aqui mora só a fila (`SyncDispatcher` + `BrainOutSyncDispatcher`).
- **Não** deixar schema JSON fora do repo — versionar junto do bump.
- **Não** escrever em `dao/` lógica de negócio (mapeamentos ficam em `RemoteDataSource` ou `Repository`).
- **Não** adicionar `withContext(Dispatchers.IO)` nos repositórios — os DAOs `suspend` do Room já saem da main thread.
- **Não** tratar `lint { abortOnError = false }` como configuração sadia: é dívida conhecida dos 8 erros `[NewApi]` por `java.time`/`java.util.Base64` com `minSdk 24`, aguardando `coreLibraryDesugaring` neste módulo (hoje ligado apenas em `:app`).