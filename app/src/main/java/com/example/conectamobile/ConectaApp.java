package com.example.conectamobile;

import android.app.Application;
import com.google.firebase.database.FirebaseDatabase;

public class ConectaApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Activa la persistencia: los chats se guardan en el celular si no hay internet
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
    }
}