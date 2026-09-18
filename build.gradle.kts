// João Pedro G M Silva - PUC Goiás ADS - 20251012000740

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.detekt) apply false
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
            sub.tasks.matching { it.name.startsWith("test") && it.name.endsWith("UnitTest") }
                .configureEach {
                    dependsOn(domainTest)
                }
        }
        logger.lifecycle("Wire-up aplicado: test*UnitTest -> :core:domain:test")
    }
}
