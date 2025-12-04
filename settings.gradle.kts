// Archivo: settings.gradle.kts
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
        // Esto permite descargar la librería MQTT
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "ConectaMobile"
include(":app")