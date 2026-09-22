// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Módulo :app — entry point do BrainOut (Application + MainActivity + NavHost).

import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    // `kotlin-android` removido: AGP 9 ativa built-in Kotlin automaticamente.
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.detekt)
}

// Assinatura de release (E3.7): lê o keystore de variáveis de ambiente
// (BRAINOUT_KEYSTORE_PATH, BRAINOUT_KEYSTORE_PASSWORD, BRAINOUT_KEY_ALIAS,
// BRAINOUT_KEY_PASSWORD). Sem as variáveis, o build de release fica
// não-assinado e o CI comum (ktlint/detekt/testes) não quebra.
// O keystore físico NUNCA entra no repositório (ver .gitignore).
val brainoutStoreFile: String? = System.getenv("BRAINOUT_KEYSTORE_PATH")
val brainoutStorePassword: String? = System.getenv("BRAINOUT_KEYSTORE_PASSWORD")
val brainoutKeyAlias: String? = System.getenv("BRAINOUT_KEY_ALIAS")
val brainoutKeyPassword: String? = System.getenv("BRAINOUT_KEY_PASSWORD")
val hasReleaseSigning: Boolean = !brainoutStoreFile.isNullOrBlank() &&
    !brainoutStorePassword.isNullOrBlank() &&
    !brainoutKeyAlias.isNullOrBlank() &&
    !brainoutKeyPassword.isNullOrBlank()

// Carrega APP_ENV e a URL base por flavor do `local.properties` para expor
// via BuildConfig. `local.properties` é ignorado pelo controle de versão
// (ver .gitignore).
val localProperties = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) {
        f.inputStream().use { load(it) }
    }
}
val appEnv: String = localProperties.getProperty("APP_ENV", "dev")

android {
    namespace = "pucgo.joaopedrogmsilva.brainout"
    compileSdk = 35

    defaultConfig {
        applicationId = "pucgo.joaopedrogmsilva.brainout"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0-alpha01"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }

        buildConfigField("String", "APP_ENV", "\"$appEnv\"")
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(brainoutStoreFile!!)
                storePassword = brainoutStorePassword
                keyAlias = brainoutKeyAlias
                keyPassword = brainoutKeyPassword
            }
        }
    }

    // Flavors de ambiente (E3.2): alinhados com :core:data. A URL base do
    // serviço de retaguarda é injetada por flavor em BuildConfig — dev
    // aponta para o backend-stub FastAPI no host do emulador (10.0.2.2),
    // prod permanece placeholder até a hospedagem definitiva (E3.1).
    // Override por desenvolvedor: brainout.baseUrl.dev em local.properties.
    flavorDimensions += "environment"
    productFlavors {
        create("dev") {
            dimension = "environment"
            buildConfigField(
                "String",
                "BASE_URL",
                "\"${localProperties.getProperty("brainout.baseUrl.dev", "http://10.0.2.2:8000/")}\"",
            )
        }
        create("prod") {
            dimension = "environment"
            buildConfigField(
                "String",
                "BASE_URL",
                "\"https://TBD/\"",
            )
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            // Sem keystore: sai não-assinado (não quebra builds de CI).
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        // `coreLibraryDesugaring` (desugar_jdk_libs 2.1.5) é necessário
        // porque `:app` usa `java.time.Instant#toEpochMilli` (API 26)
        // em `WorkManagerDeadlineScheduler` e a minSdk do projeto é 24.
        // Sem o desugaring, lint NewApi falha e o bytecode não roda em
        // dispositivos 24/25.
        isCoreLibraryDesugaringEnabled = true
    }
    // AGP 9 built-in Kotlin: `kotlinOptions` foi removido; configuramos
    // o JVM target dentro do bloco `kotlin {}` (extensão do plugin Compose
    // Compiler injeta a DSL `kotlin { compilerOptions {} }`).
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    // E3.6 — testes Robolectric do canal de notificações precisam dos
    // recursos Android (strings.xml) mesclados no classpath de teste.
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    // E4.6 — regressão de tradução: chave presente em `values/` sem
    // tradução em `values-en/` é erro fatal (`MissingTranslation`), então
    // `./gradlew :app:lintDebug` falha quando uma string nova não é
    // traduzida. O escopo é o res/ dos nossos módulos — o lint já ignora
    // `MissingTranslation` de bibliotecas externas por padrão.
    lint {
        abortOnError = true
        error += "MissingTranslation"
        checkReleaseBuilds = false
    }

    // Robolectric (E4.6): o smoke test de i18n resolve R.string, o que
    // exige que os recursos mergeados do app estejam disponíveis nos
    // testes unitários JVM.
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:data"))
    implementation(project(":core:ui"))
    implementation(project(":feature:auth"))
    implementation(project(":feature:projects"))
    implementation(project(":feature:tasks"))
    implementation(project(":feature:settings"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // WorkManager + Hilt-Work (preparado para E2.x).
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    // Core library desugaring — ver `compileOptions` acima.
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.kotlinx.coroutines.test)
    // E3.6 — testes do CompleteTaskWorker (Robolectric + ListenableWorker)
    // e do WorkManagerDeadlineScheduler (WorkManager de teste).
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.work.testing)
    testImplementation(libs.mockk)
    // E3.3 — SyncWorkerTest: banco Room em memória + MockWebServer.
    testImplementation(libs.androidx.room.runtime)
    testImplementation(libs.okhttp.mockwebserver)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Robolectric (E4.6 — smoke test de i18n: R.string não vazio em pt e en).
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
}

detekt {
    config.setFrom(rootProject.files("config/detekt/detekt.yml"))
    buildUponDefaultConfig = true
    autoCorrect = false
}

