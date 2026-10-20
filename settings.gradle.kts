pluginManagement {
    repositories {
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

rootProject.name = "PixelQuest"
include(":app")
// Turns the Quran's source files into the app's data files (docs/quran-data.md). Not part of the app.
include(":tools:quran-build")
