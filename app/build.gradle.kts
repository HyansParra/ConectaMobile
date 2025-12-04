plugins {
    id("com.android.application")
    id("com.google.gms.google-services") // Plugin obligatorio para conectar con Firebase Console
}

android {
    namespace = "com.example.conectamobile"
    compileSdk = 34 // Compilamos para Android 14 (UpsideDownCake)

    defaultConfig {
        applicationId = "com.example.conectamobile"

        // CRÍTICO: MinSdk 26 (Android 8.0)
        // Se define este mínimo para soportar iconos adaptativos y canales de notificación modernos.
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
        // CRÍTICO: Java 17
        // Requerido por las versiones recientes del plugin de Android y la librería Hannesa2.
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    // Componentes de UI estándar de Android
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // --- FIREBASE (Arquitectura Serverless) ---
    // Usamos BOM (Bill of Materials) v32.7.2 para asegurar compatibilidad entre módulos.
    implementation(platform("com.google.firebase:firebase-bom:32.7.2"))
    implementation("com.google.firebase:firebase-auth")     // Gestión de Identidad
    implementation("com.google.firebase:firebase-database") // Base de Datos NoSQL en tiempo real

    // --- MQTT (Comunicación Tiempo Real) ---
    // SELECCIÓN DE LIBRERÍA: Hannesa2 v4.3.beta1
    // Justificación: La librería estándar 'org.eclipse.paho' falla en Android 12+.
    // Esta versión utiliza 'WorkManager' para mantener la conexión estable en segundo plano
    // respetando las restricciones de batería de Android 14.
    implementation("com.github.hannesa2:paho.mqtt.android:4.3.beta1")
    implementation("androidx.work:work-runtime:2.9.0")      // Dependencia transitiva requerida por Hannesa2
    implementation("androidx.legacy:legacy-support-v4:1.0.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    // --- GESTIÓN DE MULTIMEDIA ---
    // Firebase Storage: Para subir las fotos de perfil (blobs).
    implementation("com.google.firebase:firebase-storage")

    // Glide v4.16.0: Librería de carga de imágenes.
    // Justificación: Maneja caché y redimensionamiento automático, evitando 'OutOfMemoryError'
    // al cargar fotos grandes en los RecyclerViews.
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")
}