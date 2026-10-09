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
    compileSdk = 37

    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
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

    // D-05 — `androidx.window:window`. ÚNICA dependência nova da frente de
    // responsividade, e ela fica **confinada a :core:ui** por escolha de
    // escopo (`implementation`, não `api`): como `:core:ui` é consumido por
    // todos os módulos, `api` vazaria `androidx.window.*` para o compile
    // classpath de cada `feature/*` e tornaria a contenção uma convenção
    // (que alguém quebra sem querer) em vez de uma impossibilidade
    // estrutural. Nenhum módulo fora daqui enxerga `WindowLayoutInfo` nem
    // `FoldingFeature` — a dobra é lida dentro de `:core:ui` e chega ao
    // resto do app já classificada em `NeoWidth`/`NeoHeight`.
    implementation(libs.androidx.window)

    // Testes unitários do tema (E4.4 — contraste WCAG AA via algoritmo
    // sRGB sobre os tokens em `Color.kt`).
    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)

    // DEF-22 — `NeoInfoChip` é um Composable, então precisa de
    // createComposeRule (que roda em JVM via Robolectric) para
    // verificar que ele não se anuncia como botão.
    //
    // `ui-test-manifest` não é decoração: sem ele o Robolectric não
    // resolve `androidx.activity.ComponentActivity` no manifest de
    // teste e TODOS os testes falham com "Unable to resolve activity
    // for Intent". É `debugImplementation` (e não
    // `testImplementation`) porque o manifesto precisa entrar no APK
    // de teste que o Robolectric carrega.
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

detekt {
    config.setFrom(rootProject.files("config/detekt/detekt.yml"))
    buildUponDefaultConfig = true
    autoCorrect = false
}
