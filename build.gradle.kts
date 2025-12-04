// Archivo: build.gradle.kts (Project / Raíz)
plugins {
    // Estas líneas probablemente ya las tienes (no las borres):
    alias(libs.plugins.android.application) apply false

    // AGREGA ESTA LÍNEA OBLIGATORIAMENTE:
    id("com.google.gms.google-services") version "4.4.1" apply false
}