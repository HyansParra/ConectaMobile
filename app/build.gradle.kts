plugins {
    id("com.android.application")
    id("com.google.gms.google-services") // Plugin de Firebase
}

android {
    namespace = "com.example.conectamobile"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.conectamobile"
        // Cambiamos minSdk a 26 para soportar los iconos
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        // Java 17 para que funcionen las librerías
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // --- FIREBASE ---
    implementation(platform("com.google.firebase:firebase-bom:32.7.2"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-database")

    // --- MQTT (Versión corregida) ---
    implementation("com.github.hannesa2:paho.mqtt.android:4.3.beta1")
    implementation("androidx.work:work-runtime:2.9.0")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}