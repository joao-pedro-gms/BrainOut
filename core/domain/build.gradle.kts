// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Módulo :core:domain — entidades, regras de domínio e use cases (Kotlin puro).
// Não depende de Android: máxima portabilidade e testabilidade (R12).

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.detekt)
    // Kover (E4.7): cobertura de código para o domínio puro Kotlin.
    alias(libs.plugins.kover)
    id("jacoco")
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
    // Use cases expõem funções `suspend` para que a camada superior
    // (ViewModels/repositórios) decida o dispatcher sem bloquear a UI.
    implementation(libs.kotlinx.coroutines.core)

    // `@Inject constructor` nos use cases (E1.6). Mantemos o módulo
    // `:core:domain` livre de Android/Hilt — apenas a anotação
    // `javax.inject.Inject` é necessária.
    implementation(libs.javax.inject)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
}

detekt {
    config.setFrom(rootProject.files("config/detekt/detekt.yml"))
    buildUponDefaultConfig = true
    autoCorrect = false
}

jacoco {
    toolVersion = libs.versions.jacoco.get()
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }
    sourceDirectories.setFrom(files("src/main/kotlin"))
}

