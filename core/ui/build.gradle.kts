// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Módulo :core:ui — tema Material 3, tokens e componentes Compose compartilhados.
// Depende apenas de Compose (sem Hilt/Room/DataStore).

plugins {
    alias(libs.plugins.android.library)
    // `kotlin-android` removido: AGP 9 ativa built-in Kotlin automaticamente.
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.detekt)
}

android {
    namespace = "pucgo.joaopedrogmsilva.brainout.core.ui"
    compileSdk = 35

    defaultConfig {
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

    // E4.5 — o teste Robolectric de seleção de ColorScheme precisa dos
    // recursos Android mesclados no classpath de teste para instanciar
    // ColorScheme/Color (Compose depende de Resources).
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.ui.graphics)
    api(libs.androidx.compose.ui.tooling.preview)
    api(libs.androidx.compose.material3)
    debugApi(libs.androidx.compose.ui.tooling)

    // E4.5 — teste unitário/Robolectric que valida o critério "alternância
    // segue a configuração do sistema" via resolveBrainOutStaticColorScheme.
    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
}

detekt {
    config.setFrom(rootProject.files("config/detekt/detekt.yml"))
    buildUponDefaultConfig = true
    autoCorrect = false
}

