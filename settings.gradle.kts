// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "BrainOut"

include(":app")
include(":core:domain")
include(":core:data")
include(":core:ui")
include(":feature:auth")
include(":feature:projects")
include(":feature:tasks")
include(":feature:settings")
