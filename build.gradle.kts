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
}

// Detekt é aplicado por módulo (no próprio build.gradle.kts do módulo) com
// configuração centralizada em config/detekt/detekt.yml. Evita conflitos de
// classpath quando aplicado via apply(from = ...) em subprojetos.
//
// ktlint roda no root e cobre todos os subprojetos automaticamente.
// Sobrescritas por módulo (reporters extras, filtros, etc.) são feitas
// configurando a extensão `ktlint` no `subprojects {}` quando necessário.
