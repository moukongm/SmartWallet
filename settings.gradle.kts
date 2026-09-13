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
        maven("https://jitpack.io")
    }
}

rootProject.name = "SmartWallet"
include(":app")

//Foundation
include(":foundation")
include(":foundation:network")
include(":foundation:common")
include(":foundation:uikit")
include(":foundation:storage")
include(":foundation:webview")

// Business
include(":business")
include(":business:home:api")
include(":business:home:impl")
include(":business:bill:api")
include(":business:bill:impl")
include(":business:statistics:api")
include(":business:statistics:impl")
include(":business:budget:api")
include(":business:budget:impl")
include(":business:profile:api")
include(":business:profile:impl")
