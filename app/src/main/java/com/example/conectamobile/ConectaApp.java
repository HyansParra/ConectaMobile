package com.example.conectamobile;

import android.app.Application;
import com.google.firebase.database.FirebaseDatabase;

/**
 * Clase de Aplicación Global.
 * Se ejecuta una única vez al iniciar la aplicación, antes de cualquier Activity.
 *
 * Objetivo: Configurar la persistencia de datos offline.
 */
public class ConectaApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // Habilitamos la persistencia en disco de Firebase.
        // Esto permite que la aplicación:
        // 1. Cargue el chat y contactos sin conexión a internet.
        // 2. Encole mensajes enviados en "Modo Avión" y los envíe al recuperar red.
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
    }
}