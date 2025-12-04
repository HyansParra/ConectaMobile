package com.example.conectamobile;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;

/**
 * Controlador de Inicio de Sesión.
 * Actúa como punto de entrada (Launcher) y gestiona la autenticación.
 *
 * Tecnologías:
 * - Firebase Authentication: Valida credenciales contra la nube.
 * - Persistencia de Sesión: Verifica si ya existe un usuario logueado para saltar al Main.
 */
public class LoginActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private EditText etEmail, etPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Inicializamos la instancia de Autenticación
        mAuth = FirebaseAuth.getInstance();

        // Verificación de Sesión Activa:
        // Si getCurrentUser() no es null, el usuario ya entró antes y no cerró sesión.
        // Lo redirigimos directamente al Main para mejorar la experiencia de usuario (UX).
        if (mAuth.getCurrentUser() != null) {
            startActivity(new Intent(this, MainActivity.class));
            finish(); // Cerramos Login para que no pueda volver atrás con el botón 'Back'
        }

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        Button btnLogin = findViewById(R.id.btnLogin);
        TextView tvRegister = findViewById(R.id.tvRegister);

        btnLogin.setOnClickListener(v -> login());
        tvRegister.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
    }

    private void login() {
        String email = etEmail.getText().toString();
        String password = etPassword.getText().toString();

        if (email.isEmpty() || password.isEmpty()) return;

        // Autenticación asíncrona con Firebase
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Login correcto: Navegar a la pantalla principal
                        startActivity(new Intent(this, MainActivity.class));
                        finish();
                    } else {
                        Toast.makeText(this, "Error de autenticación", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}