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

        // Classes geradas (Room/Hilt/KSP) e infra de wiring ficam fora do
        // denominador da cobertura: não são lógica testável.
        // - *_Impl / *_Impl$*: implementações geradas pelo Room
        // - *_Factory / Hilt_* / dagger.hilt.*: artefatos do Hilt/Dagger
        // - *.di.*: módulos de wiring DI
        // - *.BuildConfig / *.PackageMarker: classes utilitárias sem lógica
        // - *.remote.*: DTOs de rede (mapeamento puro, coberto via repositories)
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
                "*.di.*",
                "*.BuildConfig",
                "*.PackageMarker",
                "*.remote.*",
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
// A engine 1.x do ktlint 14 sinaliza violações cosméticas de estilo
// (indent/quebra de chamada) que o código já tem há 129 commits. Elas estão
// registradas em `docs/DEFEITOS.md` como DEF-10 e a reformatação fica para
// uma passada dedicada; `ignoreFailures` fica em `true` até lá para que
// o gate comece a fiscalizar de verdade sem bloquear todo PR.
subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    extensions.configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        android.set(true)
        ignoreFailures.set(true)
    }
}

// Relatório HTML conjunto das duas camadas (:core:domain + :core:data),
// publicado como artifact do CI (critério E4.7).
tasks.register("koverMergedHtmlReport") {
    group = "verification"
    description = "Gera o relatório HTML de cobertura combinado de :core:domain e :core:data."
    dependsOn(":core:domain:koverHtmlReport", ":core:data:koverHtmlReport")
}
