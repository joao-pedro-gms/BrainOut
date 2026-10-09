// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Módulo :feature:auth — telas e fluxo de login/cadastro.

plugins {
    alias(libs.plugins.android.library)
    // `kotlin-android` removido: AGP 9 ativa built-in Kotlin automaticamente.
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.detekt)
}

android {
    namespace = "pucgo.joaopedrogmsilva.brainout.feature.auth"
    compileSdk = 37

    defaultConfig {
        // E3.2: :core:data ganhou flavors de ambiente (dev/prod). Estes
        // módulos não têm flavors próprios; fixam a dimensão `environment`
        // no `dev` para o match de variantes do Gradle.
        missingDimensionStrategy("environment", "dev")

        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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

    testOptions {
        unitTests {
            // E2.7 — testes Compose do Login/Register via Robolectric
            // (createComposeRule roda em JVM com os resources mesclados).
            // É o único caminho para `@Config(qualifiers = ...)` de AC-1.1:
            // o `androidTest` do módulo roda só com emulador, e o CI não
            // o executa — um teste de landscape lá seria teste morto.
            isIncludeAndroidResources = true
            // ViewModels logam via android.util.Log; sem isso os testes
            // JVM puros falham com "Method d in android.util.Log not mocked".
            isReturnDefaultValues = true
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

    packaging {
        resources {
            excludes +=
                setOf(
                    "/META-INF/{AL2.0,LGPL2.1}",
                    "META-INF/LICENSE.md",
                    "META-INF/LICENSE-notice.md",
                )
            pickFirsts +=
                setOf(
                    "META-INF/AL2.0",
                    "META-INF/LGPL2.1",
                )
        }
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

    // RESP-1 — AC-1.1: Login/Register em paisagem com o teclado aberto.
    // `@Config(qualifiers = "w800dp-h400dp")` só existe no Robolectric
    // (src/test), não no androidTest.
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.espresso.core)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.mockk.android)
    androidTestImplementation(libs.truth)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

detekt {
    config.setFrom(rootProject.files("config/detekt/detekt.yml"))
    buildUponDefaultConfig = true
    autoCorrect = false
}
