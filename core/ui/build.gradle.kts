// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Módulo :core:ui — tema Material 3, tokens e componentes Compose compartilhados.
// Depende apenas de Compose (sem Hilt/Room/DataStore).

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
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
    kotlinOptions {
        jvmTarget = "17"
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

    // Testes unitários do tema (E4.4 — contraste WCAG AA via algoritmo
    // sRGB sobre os tokens em `Color.kt`).
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

