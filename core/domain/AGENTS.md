# `:core:domain` — Regras de negócio (Kotlin JVM puro)

> Sub‑AGENTS do root. Não duplica toolchain, flavors, i18n nem comandos globais.

## OVERVIEW

Entidades, exceções, portas e use cases do BrainOut. Único módulo sem
Android: `kotlin-jvm` puro, JDK 17. Coberto por Kover com piso de 60%
de linhas (E4.7). Pacote raiz `pucgo.joaopedrogmsilva.brainout.core.domain`.

## STRUCTURE

```
src/main/kotlin/.../core/domain/
├── PackageMarker.kt         # marcador de convenção, não símbolo real
├── error/                   # DomainException (sealed, raiz única) + filhos + BusinessRuleException
├── model/                   # User (+UserRole), Project, Task, Tag, TaskStatus, TaskPriority, Permission, Holiday
├── notification/            # porta DeadlineNotificationScheduler (REMINDER_LEAD = 1h)
├── repository/              # portas: User/Project/Task/Tag/Holiday/ListingPreferences + PasswordHasher + DashboardStats
└── usecase/                 # 13 use cases + UseCaseConstants (REMINDER_OFFSET_HOURS etc.)

src/test/kotlin/.../core/domain/   # espelha main
├── CoreDomainSmokeTest.kt   # importa o marcador só pra validar o wiring
├── error/                   # BusinessRuleExceptionTest, TagNotFoundExceptionTest, DomainExceptionHierarchyTest
├── model/                   # Project, Task, Tag (+TagInvariant), TaskPriority (+TaskPriorityRules), User, PermissionMatrix
├── notification/            # DeadlineNotificationSchedulerContractTest
├── repository/              # SortOrderStorageKeyTest (contrato de persistência da chave de ordenação)
└── usecase/                 # 9 specs (ver GAPS abaixo)
```

## WHERE TO LOOK

| Tarefa | Local | Notas |
|--------|-------|-------|
| Novo use case | `usecase/` + espelho `src/test/.../usecase/` | `class XxxUseCase @Inject constructor(...) { suspend operator fun invoke(...): Resultado }`; cubra com JUnit + Mockk + `runTest` |
| Novo modelo de domínio | `model/` | `data class` imutável; invariantes em `init { require(...) }`; cubra com teste em `src/test/.../model/` |
| Nova porta de repositório | `repository/` | `interface XxxRepository { suspend fun ... }`; sem impl aqui (vai em `:core:data`) |
| Nova exceção de domínio | `error/` | **estender `DomainException`**, nunca `RuntimeException`/`Exception` crua |
| Constantes de prazo | `usecase/UseCaseConstants.kt` + `notification/DeadlineNotificationScheduler.kt` | `REMINDER_LEAD = 1h` mora na porta; use cases calculam `dueDate - REMINDER_LEAD` |
| Contrato de serialização | `repository/ListingPreferencesRepository.kt` (`SortOrder`) | `toStorageKey()` emite `snake_case` — a **mesma** grafia que o `ORDER BY ... CASE WHEN` do `ProjectDao` compara; `fromStorageKey` aceita também a forma concatenada legada |

## HIERARQUIA DE EXCEÇÕES

`DomainException` é a **raiz única** do pacote `error/` e estende
`IllegalArgumentException` (não `Exception`): os modelos e use cases usam
`kotlin.require()`, que lança `IllegalArgumentException`, e essa compatibilidade
é contrato testado.

```
IllegalArgumentException
└── DomainException            (sealed, raiz única)
    ├── InvalidModelException
    ├── DuplicateEmailException
    ├── InvalidCredentialsException
    ├── InvalidStateTransitionException
    ├── TaskNotFoundException
    ├── ProjectNotFoundException
    ├── TagOwnershipException
    ├── TagNotFoundException
    └── BusinessRuleException  # RN01/RN02/RN03 — deixa de ser raiz separada
```

`BusinessRuleException` passou a estender `DomainException`, então um
`catch (DomainException)` na UI/workmanager/retaguarda agora também captura
violações de regra de negócio (antes elas escapavam). Contrato fixado por
`DomainExceptionHierarchyTest`. Ao criar exceção nova: estenda `DomainException`
ou um dos filhos acima; `catch (DomainException)` é o ponto único de tradução
para mensagem localizada.

## CONVENTIONS

- **Sem Android (R12).** Só `kotlinx-coroutines-core` + `javax.inject`. Proibido importar `androidx.*`, `dagger.*`, `hilt.*`, `room.*` aqui.
- **Use cases suspend.** Sempre `suspend operator fun invoke(...)`; dispatcher decidido na camada de cima (ViewModel).
- **Use cases finos.** Validação + orquestração de portas. Sem I/O direto, sem `try/catch` amplo — propague `DomainException`.
- **Portas-first.** Repositórios e o scheduler são `interface`; nenhuma implementação concreta neste pacote.
- **PT-BR nos comentários** referenciando IDs (`R#`, `E#`).
- **PackageMarker.kt** permanece como sentinela do pacote (regra do root); não remova. (Ele existe aqui e em `:core:data` e nos 4 features; falta em `:app` e `:core:ui` — ver root.)
- **Header de autor** em todo `.kt` (regra do root).
- Strings não vivem aqui (camada de UI).

## ANTI-PATTERNS

- **Nada de Android/Room/Hilt.** Quebra R12 e invalida o motivo do módulo existir.
- **Não burlar cobertura.** Sem `@Generated`/excludes locais; exclusions do Kover ficam centralizadas no root.
- **Sem lógica espessa em use case.** Se passar de orquestração, mova regra para `model` (invariante) ou para um serviço de domínio em `core/domain`.
- **Sem implementação concreta de porta aqui.** Se precisar mockar nos testes, use `mockk` no `src/test`.
- **Sem `Exception`/`Throwable` crua** nas fronteiras; use a hierarquia em `error/`. **Em especial, não crie uma segunda raiz** ao lado de `DomainException` — foi o que `BusinessRuleException` fazia antes de passar a estendê-la.
- **Não introduza `Dispatchers.IO` aqui.** Os DAOs `suspend` do Room já rodam fora da main thread; quem escolhe dispatcher é a camada de cima.

## TESTES & COBERTURA

- Piso **Kover 60% linhas** (E4.7) verificado por `./gradlew :core:domain:koverVerify` (ver root para comando completo).
- Wire‑up de CI: `gradle.projectsEvaluated` no root amarra `test*UnitTest` a `:core:domain:test`, então `./gradlew testDebugUnitTest` cobre este módulo.
- Módulo tem **13 use cases**; **8 têm spec dedicada** em `src/test/.../usecase/` (`AuthenticateUser`, `CanPerformAction`, `ChangeTaskStatus`, `CheckDeadline`, `CreateTask`, `CreateUser`, `DeleteTask`, `UpdateTask`) — mais `ProjectCompletionTest.kt`, que cobre a transição de projeto concluído. Demais pastas (`error`, `model`, `notification`, `repository`) estão bem cobertas.
- **Gaps conhecidos (verificados em 2026‑10‑08):** 5 dos 13 use cases **sem teste dedicado** — `CreateProjectUseCase`, `CreateTagUseCase`, `UpdateProjectUseCase`, `DeleteProjectUseCase`, `DeleteTagUseCase`. Adicionar o spec antes de mexer nessas regras (todo use case novo entra nesta lista até vir com o teste junto).