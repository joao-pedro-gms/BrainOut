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
├── error/                   # DomainException (selada), BusinessRuleException, TagNotFoundException
├── model/                   # User (+UserRole), Project, Task, Tag, TaskStatus, TaskPriority, Permission, Holiday
├── notification/            # porta DeadlineNotificationScheduler (REMINDER_LEAD = 1h)
├── repository/              # portas: User/Project/Task/Tag/Holiday/ListingPreferences + PasswordHasher + DashboardStats
└── usecase/                 # 13 use cases + UseCaseConstants (REMINDER_OFFSET_HOURS etc.)

src/test/kotlin/.../core/domain/   # espelha main (sem `repository/`)
├── CoreDomainSmokeTest.kt   # importa o marcador só pra validar o wiring
└── {error,model,notification,usecase}/   # 19 specs hoje
```

## WHERE TO LOOK

| Tarefa | Local | Notas |
|--------|-------|-------|
| Novo use case | `usecase/` + espelho `src/test/.../usecase/` | `class XxxUseCase @Inject constructor(...) { suspend operator fun invoke(...): Resultado }`; cobre com JUnit + Mockk + `runTest` |
| Novo modelo de domínio | `model/` | `data class` imutável; invariantes em `init { require(...) }`; cubra com teste em `src/test/.../model/` |
| Nova porta de repositório | `repository/` | `interface XxxRepository { suspend fun ... }`; sem impl aqui (vai em `:core:data`) |
| Nova exceção de domínio | `error/` | estender `DomainException`; `BusinessRuleException` cobre violações de regra; nada de `Exception` crua |
| Constantes de prazo | `usecase/UseCaseConstants.kt` + `notification/DeadlineNotificationScheduler.kt` | `REMINDER_LEAD = 1h` mora na porta; use cases calculam `dueDate - REMINDER_LEAD` |

## CONVENTIONS

- **Sem Android (R12).** Só `kotlinx-coroutines-core` + `javax.inject`. Proibido importar `androidx.*`, `dagger.*`, `hilt.*`, `room.*` aqui.
- **Use cases suspend.** Sempre `suspend operator fun invoke(...)`; dispatcher decidido na camada de cima (ViewModel).
- **Use cases finos.** Validação + orquestração de portas. Sem I/O direto, sem `try/catch` amplo — propague `DomainException`.
- **Portas-first.** Repositórios e o scheduler são `interface`; nenhuma implementação concreta neste pacote.
- **PT-BR nos comentários** referenciando IDs (`R#`, `E#`).
- **PackageMarker.kt** permanece como sentinela do pacote (regra do root); não remova.
- **Header de autor** em todo `.kt` (regra do root).
- Strings não vivem aqui (camada de UI).

## ANTI-PATTERNS

- **Nada de Android/Room/Hilt.** Quebra R12 e invalida o motivo do módulo existir.
- **Não burlar cobertura.** Sem `@Generated`/excludes locais; exclusions do Kover ficam centralizadas no root.
- **Sem lógica espessa em use case.** Se passar de orquestração, mova regra para `model` (invariante) ou para um serviço de domínio em `core/domain`.
- **Sem implementação concreta de porta aqui.** Se precisar mockar nos testes, use `mockk` no `src/test`.
- **Sem `Exception`/`Throwable` crua** nas fronteiras; use a hierarquia em `error/`.

## TESTES & COBERTURA

- Piso **Kover 60% linhas** (E4.7) verificado por `./gradlew :core:domain:koverVerify` (ver root para comando completo).
- Wire‑up de CI: `gradle.projectsEvaluated` no root amarra `test*UnitTest` a `:core:domain:test`, então `./gradlew testDebugUnitTest` cobre este módulo.
- Gap conhecido (auditoria 2026‑09‑24): `CreateTagUseCase`, `UpdateProjectUseCase`, `DeleteProjectUseCase`, `DeleteTagUseCase` sem teste. Adicionar antes de mexer nessas regras.