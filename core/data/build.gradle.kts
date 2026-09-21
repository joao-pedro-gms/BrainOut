// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Módulo :core:data — Room, DataStore, fontes remotas e implementações de repositório.

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.detekt)
    // Kover (E4.7): cobertura de código para a camada de dados.
    alias(libs.plugins.kover)
}

import java.util.Properties

// Override opcional da URL base por flavor (E3.2): lê `local.properties`
// (não versionado). Sem o arquivo, vale o default do flavor.
val localProperties = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) {
        f.inputStream().use { load(it) }
    }
}

android {
    namespace = "pucgo.joaopedrogmsilva.brainout.core.data"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Exporta o schema Room versionado para o diretório versionado
        // abaixo. Permite gerar migrations e auditar diffs entre
        // versões do banco ao revisar PRs.
        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
            arg("room.incremental", "true")
        }
    }

    // Flavors de ambiente (E3.2): a URL base do serviço de retaguarda é
    // injetada em BuildConfig por flavor. O host do flavor dev aponta para o
    // backend-stub FastAPI no host do emulador (10.0.2.2); o de prod
    // permanece placeholder até a hospedagem definitiva (decisão E3.1).
    // O override por desenvolvedor vai em `local.properties`
    // (brainout.baseUrl.dev — ver `local.properties.example`).
    //
    // E3.5: adiciona `HOLIDAYS_BASE_URL` para o serviço externo público
    // de feriados nacionais (BrasilAPI). Por ser público e sem chave,
    // não há override por local.properties — apenas o default do flavor.
    flavorDimensions += "environment"
    productFlavors {
        create("dev") {
            dimension = "environment"
            buildConfigField(
                "String",
                "BASE_URL",
                "\"${localProperties.getProperty("brainout.baseUrl.dev", "http://10.0.2.2:8000/")}\"",
            )
            buildConfigField(
                "String",
                "HOLIDAYS_BASE_URL",
                "\"https://brasilapi.com.br/\"",
            )
        }
        create("prod") {
            dimension = "environment"
            buildConfigField(
                "String",
                "BASE_URL",
                "\"https://TBD/\"",
            )
            buildConfigField(
                "String",
                "HOLIDAYS_BASE_URL",
                "\"https://brasilapi.com.br/\"",
            )
        }
    }

    buildFeatures {
        buildConfig = true
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
}

dependencies {
    implementation(project(":core:domain"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)

    // Rede remota (E3.2): Retrofit + OkHttp + kotlinx-serialization.
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization.converter)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.kotlinx.serialization.json)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // DataStore Preferences
    implementation(libs.androidx.datastore.preferences)

    // Security — MasterKey para pepper do PasswordHasher (Tink).
    implementation(libs.androidx.security.crypto)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.robolectric)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.okhttp.mockwebserver)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.room.testing)
}

detekt {
    config.setFrom(rootProject.files("config/detekt/detekt.yml"))
    buildUponDefaultConfig = true
    autoCorrect = false
}

