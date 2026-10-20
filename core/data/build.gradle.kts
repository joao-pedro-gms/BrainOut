// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Módulo :core:data — Room, DataStore, calendário local e implementações de repositório.

plugins {
    alias(libs.plugins.android.library)
    // `kotlin-android` removido: AGP 9 ativa built-in Kotlin automaticamente.
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.detekt)
    // Kover (E4.7): cobertura de código para a camada de dados.
    alias(libs.plugins.kover)
}

android {
    namespace = "pucgo.joaopedrogmsilva.brainout.core.data"
    compileSdk = 37

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

    buildFeatures {
        buildConfig = true
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
        // Issue #201 (DEF-63): `java.time` (Instant/LocalDate) e
        // `java.util.Base64` são usados neste módulo e exigem API 26
        // — sem desugaring, o app quebra em API 24/25 e o lint
        // reportaria NewApi em cada uso. O mesmo mecanismo já é usado
        // em `:app`; aqui ele fecha a dívida registrada no módulo.
        isCoreLibraryDesugaringEnabled = true
    }
    // AGP 9 built-in Kotlin: `kotlinOptions` foi removido; usamos o bloco
    // `kotlin { compilerOptions {} }` introduzido pelo plugin Compose
    // Compiler (também presente em :core:data mesmo sem Compose, via
    // dependência transitiva do build classpath).
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    // Issue #201 (DEF-63): com o `coreLibraryDesugaring` habilitado
    // (ver `compileOptions`), o módulo não tem mais nenhum erro
    // [NewApi] — o gate de lint volta a ser rígido, como nos demais
    // módulos. A dívida de `java.time`/`java.util.Base64` sem
    // desugaring (que mantinha `abortOnError = false`) está fechada.
    //
    // `MissingTranslation` continua NÃO promovido: este módulo não
    // tem strings de interface (E4.6 trata apenas os módulos que
    // têm `values/` e `values-en/`).
    lint {
        abortOnError = true
        checkReleaseBuilds = false
    }
}

androidComponents {
    onVariants(selector().all()) { variant ->
        variant.androidTest
            ?.sources
            ?.assets
            ?.addStaticSourceDirectory("$projectDir/schemas")
    }
}

dependencies {
    implementation(project(":core:domain"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)

    // Issue #201 (DEF-63) — desugaring de `java.time`/`java.util.Base64`
    // para API 24/25 (ver `compileOptions` acima).
    coreLibraryDesugaring(libs.desugar.jdk.libs)

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

    // Deps dos testes INSTRUMENTADOS. `MigrationTest` e
    // `UserDaoInstrumentedTest` importam Truth e coroutines-test, mas
    // ambos vivem no bloco `testImplementation` — sem estas duas linhas
    // o `compileDevDebugAndroidTestKotlin` falhava com ~40 erros
    // `Unresolved reference` e a suíte instrumentada nunca compilou
    // (nem local, nem no CI, que também não invoca
    // `connectedAndroidTest`).
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.truth)
    androidTestImplementation(libs.kotlinx.coroutines.test)
}

detekt {
    config.setFrom(rootProject.files("config/detekt/detekt.yml"))
    buildUponDefaultConfig = true
    autoCorrect = false
}
