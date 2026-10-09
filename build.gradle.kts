// João Pedro G M Silva - PUC Goiás ADS - 20251012000740

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    // `kotlin-android` removido: AGP 9 ativa built-in Kotlin automaticamente
    // para os módulos Android. `kotlin-jvm` permanece para :core:domain.
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.detekt) apply false
    // Kover: cobertura de código + verificação de bound mínimo (E4.7).
    // Aplicado em :core:domain e :core:data (alvo do critério de 60%).
    alias(libs.plugins.kover) apply false
    // ktlint aplica no root e cobre automaticamente todos os subprojetos.
    alias(libs.plugins.ktlint)
    // Plugin JaCoCo compartilhado entre subprojetos para reportar
    // cobertura. Plugin core do Gradle 8 — sem versão aqui.
    id("jacoco")
}

// Detekt é aplicado por módulo (no próprio build.gradle.kts do módulo) com
// configuração centralizada em config/detekt/detekt.yml. Evita conflitos de
// classpath quando aplicado via apply(from = ...) em subprojetos.
//
// ktlint roda no root e cobre todos os subprojetos automaticamente.
// Sobrescritas por módulo (reporters extras, filtros, etc.) são feitas
// configurando a extensão `ktlint` no `subprojects {}` quando necessário.

// Garante que os testes do módulo puro Kotlin `:core:domain` rodem sempre
// que algum módulo Android executar seus testes unitários. CI invoca
// `./gradlew testDebugUnitTest`, que por padrão só cobre os módulos
// Android; este hook garante que o domínio também seja exercitado.
gradle.projectsEvaluated {
    val domainTest = project(":core:domain").tasks.findByName("test")
    if (domainTest == null) {
        logger.warn("Não foi possível localizar :core:domain:test para wire-up")
    } else {
        rootProject.subprojects.forEach { sub ->
            sub.tasks
                .matching { it.name.startsWith("test") && it.name.endsWith("UnitTest") }
                .configureEach {
                    dependsOn(domainTest)
                }
        }
        logger.lifecycle("Wire-up aplicado: test*UnitTest -> :core:domain:test")
    }
}

// ---- Kover (E4.7) -----------------------------------------------------------
// Configuração compartilhada dos módulos de cobertura: :core:domain (Kotlin
// JVM puro) e :core:data (Android library). Aplica o plugin, define o bound
// mínimo de 60% de cobertura de linhas e registra os filtros de classes
// geradas (Room/Hilt/KSP) que não fazem parte da lógica testável.
//
// O bound é checado por `koverVerify` em cada módulo; o relatório HTML
// conjunto das duas camadas é gerado pela task `koverMergedHtmlReport`
// registrada abaixo (publicada como artifact do CI).
subprojects {
    if (path in setOf(":core:domain", ":core:data")) {
        apply(plugin = "org.jetbrains.kotlinx.kover")

        // Fora do denominador da cobertura fica só o que é realmente gerado
        // por ferramenta ou não tem lógica — nunca uma classe de produção
        // exercitada por teste (DEF-14).
        //
        // - *_Impl / *_Impl$*: implementações geradas pelo Room
        // - *_Factory / Hilt_* / dagger.hilt.* / *_HiltModules: artefatos
        //   do Hilt/Dagger
        // - *Dto / *Dto$*: data classes de contrato (RemoteDtos.kt,
        //   HolidayDto.kt). Sem lógica própria — só propriedades,
        //   componentN/copy/equals/hashCode gerados e o serializador
        //   `$$serializer` do kotlinx-serialization. O parsing de verdade é
        //   exercitado em RemoteDataSource, que fica no denominador.
        //   O padrão é `*Dto`, e NÃO `*.Dto`: o Kover converte `*` em `.*`
        //   e escapa o resto, então `*.Dto` vira `.*\.Dto` e jamais casaria
        //   com `...remote.ProjectDto` (o nome termina em `ProjectDto`, sem
        //   ponto antes de `Dto`). O `$*` cobre `ProjectDto$Companion`.
        // - *.di.*: wiring de DI (DataModule.kt tem 19 métodos @Provides que
        //   só constroem e devolvem singletons/binds, zero lógica de
        //   domínio). A exclusão é por "wiring, não lógica" — e não por
        //   pacote: qualquer classe com comportamento que apareça depois em
        //   `*.di.*` deve entrar no denominador.
        // - *.BuildConfig / *.PackageMarker: constantes geradas
        val koverExclusions =
            listOf(
                "*_Impl",
                "*_Impl\$*",
                "*_Factory",
                "*_Factory\$*",
                "*_*Factory",
                "*_*Factory\$*",
                "*.Hilt_*",
                "*_HiltModules",
                "*_HiltModules\$*",
                "dagger.hilt.*",
                "hilt_aggregated_deps.*",
                "*Dto",
                "*Dto\$*",
                "*.di.*",
                "*.BuildConfig",
                "*.PackageMarker",
            )

        the<kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension>().apply {
            reports {
                filters {
                    excludes {
                        classes(koverExclusions)
                    }
                }
                verify {
                    rule {
                        bound {
                            // Critério E4.7: cobertura mínima de 60%.
                            minValue = 60
                        }
                    }
                }
            }
        }

        logger.lifecycle("Kover aplicado em $path (bound 60% + filtros de classes geradas)")
    }
}

// ---- ktlint (correção de escopo) ------------------------------------------------
// O plugin ktlint NÃO se propaga automaticamente para subprojetos: aplicado
// apenas na raiz, `./gradlew ktlintCheck` inspecionava somente os 2 arquivos
// de build da raiz e ZERO dos 221 arquivos `.kt` versionados — confirmado em
// disco: só existia `build/reports/ktlint/` na raiz. Aqui ele é aplicado a
// cada subprojeto, com `android = true` para o analisador entender código
// Android/Compose.
//
// A engine 1.x do ktlint 14 sinalizava violações cosméticas de estilo
// (indent/quebra de chamada) que o código carregava há 129 commits.
// Elas estavam registradas em `docs/DEFEITOS.md` como DEF-10, com o
// `ignoreFailures = true` como contorno deliberado para que o gate
// passasse a produzir relatório sem bloquear todo PR.
//
// O DEF-10 foi fechado: `ktlintFormat` nos 9 subprojetos (commit
// `b59318e`) mais o acerto das 9 violações não auto-corrigíveis
// (commit `b6db6e0`) zeraram o contador. O `ignoreFailures` volta
// para `false` — o gate agora bloqueia de verdade, que era o ponto
// do DEF-09 ao alcançar os subprojetos. Violação nova quebra o PR,
// que é o comportamento pretendido para um gate de estilo.
subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    extensions.configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        android.set(true)
        ignoreFailures.set(false)
    }
}

// Relatório HTML conjunto das duas camadas (:core:domain + :core:data),
// publicado como artifact do CI (critério E4.7).
tasks.register("koverMergedHtmlReport") {
    group = "verification"
    description = "Gera o relatório HTML de cobertura combinado de :core:domain e :core:data."
    dependsOn(":core:domain:koverHtmlReport", ":core:data:koverHtmlReport")
}
