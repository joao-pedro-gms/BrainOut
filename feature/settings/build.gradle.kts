// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Módulo :feature:settings — preferências do app, perfil, tema.

plugins {
    alias(libs.plugins.android.library)
    // `kotlin-android` removido: AGP 9 ativa built-in Kotlin automaticamente.
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.detekt)
}

android {
    namespace = "pucgo.joaopedrogmsilva.brainout.feature.settings"
    compileSdk = 35

    defaultConfig {
    // E3.2: :core:data ganhou flavors de ambiente (dev/prod). Estes
    // módulos não têm flavors próprios; fixam a dimensão `environment`
    // no `dev` para o match de variantes do Gradle.
    missingDimensionStrategy("environment", "dev")

        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    // AGP 9 built-in Kotlin: ver nota em :app/build.gradle.kts.
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        compose = true
    }

    // E4.6 — regressão de tradução: chave presente em `values/` sem
    // tradução em `values-en/` é erro fatal (`MissingTranslation`).
    lint {
        abortOnError = true
        error += "MissingTranslation"
        checkReleaseBuilds = false
    }
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:data"))
    implementation(project(":core:ui"))

    implementation(libs.androidx.core.ktx)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.mockk)
}

detekt {
    config.setFrom(rootProject.files("config/detekt/detekt.yml"))
    buildUponDefaultConfig = true
    autoCorrect = false
}

