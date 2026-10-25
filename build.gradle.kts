// João Pedro G M Silva - PUC Goiás ADS - 20251012000740

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    // `kotlin-android` removido: AGP 9 ativa built-in Kotlin automaticamente
    // para os módulos Android. `kotlin-jvm` permanece para :core:domain.
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.detekt) apply false
    // Kover: cobertura de código + verificação de bound mínimo (E4.7).
    // Aplicado em :core:domain e :core:data (alvo do critério de 60%).
    alias(libs.plugins.kover) apply false
    // ktlint é aplicado a cada subprojeto pelo bloco `subprojects { }`
    // mais abaixo — o plugin NÃO se propaga automaticamente.
    alias(libs.plugins.ktlint)
    // Plugin JaCoCo compartilhado entre subprojetos para reportar
    // cobertura. Plugin core do Gradle 8 — sem versão aqui.
    id("jacoco")
}

// Detekt é aplicado por módulo (no próprio build.gradle.kts do módulo) com
// configuração centralizada em config/detekt/detekt.yml. Evita conflitos de
// classpath quando aplicado via apply(from = ...) em subprojetos.
//
// ktlint é aplicado pelo bloco `subprojects { }` mais abaixo, que o
// registra em cada um dos 8 módulos. Sobrescritas por módulo
// (reporters extras, filtros, etc.) são feitas configurando a extensão
// `ktlint` nesse mesmo bloco quando necessário.

// Garante que os testes do módulo puro Kotlin `:core:domain` rodem sempre
// que algum módulo Android executar seus testes unitários. CI invoca
// `./gradlew testDebugUnitTest`, que por padrão só cobre os módulos
// Android; este hook garante que o domínio também seja exercitado.
gradle.projectsEvaluated {
    val domainTest = project(":core:domain").tasks.findByName("test")
    if (domainTest == null) {
        logger.warn("Não foi possível localizar :core:domain:test para wire-up")
    } else {
        rootProject.subprojects.forEach { sub ->
            sub.tasks
                .matching { it.name.startsWith("test") && it.name.endsWith("UnitTest") }
                .configureEach {
                    dependsOn(domainTest)
                }
        }
        logger.lifecycle("Wire-up aplicado: test*UnitTest -> :core:domain:test")
    }
}

// ---- Kover (E4.7) -----------------------------------------------------------
// Configuração compartilhada dos módulos de cobertura: :core:domain (Kotlin
// JVM puro) e :core:data (Android library). Aplica o plugin, define o bound
// mínimo de 60% de cobertura de linhas e registra os filtros de classes
// geradas (Room/Hilt/KSP) que não fazem parte da lógica testável.
//
// O bound é checado por `koverVerify` em cada módulo; o relatório HTML
// conjunto das duas camadas é gerado pela task `koverMergedHtmlReport`
// registrada abaixo (publicada como artifact do CI).
subprojects {
    if (path in setOf(":core:domain", ":core:data")) {
        apply(plugin = "org.jetbrains.kotlinx.kover")

        // Fora do denominador da cobertura fica só o que é realmente gerado
        // por ferramenta ou não tem lógica — nunca uma classe de produção
        // exercitada por teste (DEF-14).
        //
        // - *Dao_Impl / *Dao_Impl$* e *Database_Impl / *Database_Impl$*:
        //   implementações **geradas pelo Room** (UserDao_Impl,
        //   BrainOutDatabase_Impl e suas classes internas). O padrão
        //   antigo "*_Impl" (qualquer nome terminado em _Impl) foi
        //   estreitado em #215 (DEF-64): ele não pegava as
        //   implementações de produção — `ProjectRepositoryImpl` e
        //   `PasswordHasherImpl` nunca tiveram sublinhado e sempre
        //   ficaram no denominador — mas passaria a capturar de
        //   surpresa qualquer classe renomeada para `Algo_Impl`. O
        //   verificador `.github/scripts/verify_coverage_scope.py`
        //   impede a volta do padrão amplo.
        // - *_Factory / Hilt_* / dagger.hilt.* / *_HiltModules: artefatos
        //   do Hilt/Dagger
        // - *.di.*: wiring de DI (DataModule.kt tem 19 métodos @Provides que
        //   só constroem e devolvem singletons/binds, zero lógica de
        //   domínio). A exclusão é por "wiring, não lógica" — e não por
        //   pacote: qualquer classe com comportamento que apareça depois em
        //   `*.di.*` deve entrar no denominador.
        // - *.BuildConfig / *.PackageMarker: constantes geradas
        val koverExclusions =
            listOf(
                "*Dao_Impl",
                "*Dao_Impl\$*",
                "*Database_Impl",
                "*Database_Impl\$*",
                "*_Factory",
                "*_Factory\$*",
                "*_*Factory",
                "*_*Factory\$*",
                "*.Hilt_*",
                "*_HiltModules",
                "*_HiltModules\$*",
                "dagger.hilt.*",
                "hilt_aggregated_deps.*",
                "*.di.*",
                "*.BuildConfig",
                "*.PackageMarker",
            )

        the<kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension>().apply {
            reports {
                filters {
                    excludes {
                        classes(koverExclusions)
                    }
                }
                verify {
                    rule {
                        bound {
                            // Critério E4.7: cobertura mínima de 60%.
                            minValue = 60
                        }
                    }
                }
            }
        }

        logger.lifecycle("Kover aplicado em $path (bound 60% + filtros de classes geradas)")
    }
}

// ---- ktlint (correção de escopo) ------------------------------------------------
// O plugin ktlint NÃO se propaga automaticamente para subprojetos: aplicado
// apenas na raiz, `./gradlew ktlintCheck` inspecionava somente os 2 arquivos
// de build da raiz e ZERO dos 221 arquivos `.kt` versionados — confirmado em
// disco: só existia `build/reports/ktlint/` na raiz. Aqui ele é aplicado a
// cada subprojeto, com `android = true` para o analisador entender código
// Android/Compose.
//
// A engine 1.x do ktlint 14 sinalizava violações cosméticas de estilo
// (indent/quebra de chamada) que o código carregava há 129 commits.
// Elas estavam registradas em `docs/DEFEITOS.md` como DEF-10, com o
// `ignoreFailures = true` como contorno deliberado para que o gate
// passasse a produzir relatório sem bloquear todo PR.
//
// O DEF-10 foi fechado: `ktlintFormat` nos 8 subprojetos (commit
// `b59318e`) mais o acerto das 9 violações não auto-corrigíveis
// (commit `b6db6e0`) zeraram o contador. O `ignoreFailures` volta
// para `false` — o gate agora bloqueia de verdade, que era o ponto
// do DEF-09 ao alcançar os subprojetos. Violação nova quebra o PR,
// que é o comportamento pretendido para um gate de estilo.
subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    extensions.configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        android.set(true)
        ignoreFailures.set(false)
    }
}

// ---- SDK: uma versão só para o projeto inteiro ---------------------------
// `compileSdk` e `minSdk` estavam declarados nos sete módulos Android —
// sete cópias do mesmo número. O SDK alvo é uma decisão do **projeto**:
// a AGP exige coerência entre os módulos do mesmo build (o metadado do
// AAR), então divergir era um erro silencioso esperando acontecer.
//
// O `targetSdk` fica só no `:app`, onde ele faz sentido (é o que a
// loja lê), e o alvo do bytecode fica por módulo — o `kotlin { }` do
// AGP 9 fica dentro de `android { }` e não é alcançável pela API
// pública da AGP.
subprojects {
    plugins.withId("com.android.application") {
        extensions.configure<com.android.build.api.dsl.ApplicationExtension> {
            compileSdk = 37
            defaultConfig { minSdk = 24 }
        }
    }
    plugins.withId("com.android.library") {
        extensions.configure<com.android.build.api.dsl.LibraryExtension> {
            compileSdk = 37
            defaultConfig { minSdk = 24 }
        }
    }
}

// ---- Testes unitários: os recursos Android entram no classpath --------
// DEF-23b — os testes de UI Compose (`createComposeRule` roda em JVM via
// Robolectric) precisam dos recursos Android mesclados no classpath de
// teste, senão `stringResource` devolve vazio e toda asserção sobre
// texto/`contentDescription` passa a mentir.
//
// A flag estava declarada em seis módulos; aqui ela é uma regra só.
//
// A exceção é o `:core:data`, que **opta por fora**: no modo legado de
// recursos do Robolectric, os testes de DataStore dele quebram com
// "Failed to interact with raw FileDescriptor internals" — e eles não
// precisam dos recursos Android.
//
// O `isReturnDefaultValues` **não** vem para a raiz de propósito: ele
// faz o Android devolver 0/null em vez de estourar "not mocked", o que
// **esconde** um teste que chama API Android sem querer. Fica por módulo,
// onde a decisão é consciente.
subprojects {
    plugins.withId("com.android.application") {
        extensions.configure<com.android.build.api.dsl.ApplicationExtension> {
            testOptions.unitTests.isIncludeAndroidResources = true
        }
    }
    plugins.withId("com.android.library") {
        extensions.configure<com.android.build.api.dsl.LibraryExtension> {
            testOptions.unitTests.isIncludeAndroidResources = true
        }
    }
}

// ---- Lint: o gate de i18n é do projeto inteiro ----------------------------
// O bloco `lint { }` estava nos OITO módulos Android. A política é uma:
//
// - o lint **aborta** (`abortOnError = true`);
// - falta de tradução é erro (`MissingTranslation`).
//
// E4.6 — regressão de tradução: chave presente em `values/` sem tradução
// em `values-en/` é erro fatal, então `./gradlew :app:lintDebug` falha
// quando uma string nova não é traduzida. O escopo é o `res/` dos
// nossos módulos — o lint já ignora `MissingTranslation` de bibliotecas
// externas por padrão.
//
// A promoção vale para **todo** módulo Android, inclusive os que não têm
// string de interface — para eles ela é inócua. Assim um módulo NOVO
// com strings entra no gate sem que ninguém precise lembrar. `:core:data`
// é a única exceção, e ela opta por fora (ver o `build.gradle.kts` dele).
subprojects {
    plugins.withId("com.android.application") {
        extensions.configure<com.android.build.api.dsl.ApplicationExtension> {
            lint.apply {
                abortOnError = true
                error += "MissingTranslation"
                checkReleaseBuilds = false
            }
        }
    }
    plugins.withId("com.android.library") {
        extensions.configure<com.android.build.api.dsl.LibraryExtension> {
            lint.apply {
                abortOnError = true
                error += "MissingTranslation"
                checkReleaseBuilds = false
            }
        }
    }
}

// ---- Detekt: uma config para o projeto inteiro ----------------------------
// O bloco `detekt { }` estava nos OITO módulos, com o mesmo conteúdo —
// uma mudança de regra na config seria oito edições, e um módulo novo
// podia ficar com a config divergente. A config é única
// (`config/detekt/detekt.yml`) e vale para todos.
subprojects {
    plugins.withId("io.gitlab.arturbosch.detekt") {
        extensions.configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
            config.setFrom(rootProject.files("config/detekt/detekt.yml"))
            buildUponDefaultConfig = true
            autoCorrect = false
        }
    }
}

// ---- Toolchain de teste comum -----------------------------------------------
// A base de teste de todo o projeto é a mesma: JUnit 4, Truth (as
// asserções) e kotlinx-coroutines-test (runTest). Ela estava declarada
// nos OITO módulos — oito cópias da mesma decisão, que um módulo novo
// podia esquecer. Aqui ela é escrita uma vez.
//
// O que fica no módulo é só o que é específico dele: Turbine e MockK
// (usados pelas features), Robolectric e `androidx.test.core` (nos que
// rodam Room/compose em JVM), o `ui-test-manifest` e as dependências
// instrumentadas.
//
// O gatilho são os três ids de plugin que os módulos do repo usam:
// AGP para biblioteca (`:core:*` e `:feature:*`), AGP para aplicação
// (`:app`) e Kotlin JVM (`:core:domain`). O `java` não serve: o AGP 9
// não o aplica, e o accessor `testImplementation(...)` também não
// existe na raiz — por isso o `add("testImplementation", …)`.
subprojects {
    val project = this
    listOf("com.android.application", "com.android.library", "org.jetbrains.kotlin.jvm").forEach { pluginId ->
        plugins.withId(pluginId) {
            project.dependencies.add("testImplementation", libs.junit)
            project.dependencies.add("testImplementation", libs.truth)
            project.dependencies.add("testImplementation", libs.kotlinx.coroutines.test)
        }
    }
}

// Relatório HTML conjunto das duas camadas (:core:domain + :core:data),
// publicado como artifact do CI (critério E4.7).
tasks.register("koverMergedHtmlReport") {
    group = "verification"
    description = "Gera o relatório HTML de cobertura combinado de :core:domain e :core:data."
    dependsOn(":core:domain:koverHtmlReport", ":core:data:koverHtmlReport")
}
