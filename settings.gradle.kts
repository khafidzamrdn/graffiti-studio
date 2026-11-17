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

rootProject.name = "GraffitiStudio"

include(":app")
include(":core:domain")
include(":core:designsystem")
include(":core:storage")
include(":core:data")
include(":engine:rendering")
include(":engine:drawing")
include(":feature:home")
include(":feature:workspace")
