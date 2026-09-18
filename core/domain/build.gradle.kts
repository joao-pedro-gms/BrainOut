// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Módulo :core:domain — entidades, regras de domínio e use cases (Kotlin puro).
// Não depende de Android: máxima portabilidade e testabilidade (R12).

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.detekt)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    // Intencionalmente vazio neste marco (E1.1). Entidades e use cases
    // virão em E1.4 e E1.7. Mantemos o módulo compilando para validar o
    // esqueleto multi-módulo end-to-end.
    testImplementation(libs.junit)
    testImplementation(libs.truth)
}

detekt {
    config.setFrom(rootProject.files("config/detekt/detekt.yml"))
    buildUponDefaultConfig = true
    autoCorrect = false
}

