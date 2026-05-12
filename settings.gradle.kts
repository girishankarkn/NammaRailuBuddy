pluginManagement {
    repositories {
        // We use the full method to ensure 'google' is resolved correctly
        maven(url = "https://maven.google.com")
        google()
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

rootProject.name = "NammaRailuBuddy"
include(":app")