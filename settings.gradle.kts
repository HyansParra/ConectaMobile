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
        // CRÍTICO: Esto permite descargar la librería MQTT arreglada
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "ConectaMobile"
include(":app")