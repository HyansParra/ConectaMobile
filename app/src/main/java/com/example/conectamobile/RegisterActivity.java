package com.example.conectamobile;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

/**
 * Controlador de Registro de Usuarios.
 * Maneja una transacción compleja de tres pasos:
 * 1. Crear cuenta en Firebase Authentication.
 * 2. Subir foto de perfil a Firebase Storage (Blob).
 * 3. Guardar metadatos (URL foto, nombre) en Realtime Database.
 */
public class RegisterActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private StorageReference mStorage; // Referencia al almacenamiento de archivos

    private EditText etName, etEmail, etPassword;
    private ImageView ivProfile;
    private Uri imageUri; // URI local de la imagen seleccionada

    // Contrato para obtener resultados de la Galería de imágenes
    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    imageUri = result.getData().getData();
                    ivProfile.setImageURI(imageUri); // Previsualización local
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mStorage = FirebaseStorage.getInstance().getReference();

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        ivProfile = findViewById(R.id.ivProfile);
        Button btnRegister = findViewById(R.id.btnRegister);

        // Selector de imagen
        ivProfile.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            galleryLauncher.launch(intent);
        });

        btnRegister.setOnClickListener(v -> register());
    }

    private void register() {
        String name = etName.getText().toString();
        String email = etEmail.getText().toString();
        String password = etPassword.getText().toString();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Registrando...", Toast.LENGTH_SHORT).show();

        // Paso 1: Crear Usuario
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        String uid = firebaseUser.getUid();

                        // Paso 2: Subir Imagen (si existe)
                        if (imageUri != null) {
                            uploadImageAndSaveUser(uid, email, name);
                        } else {
                            saveUserToDb(uid, email, name, ""); // Registrar sin foto
                        }
                    } else {
                        Toast.makeText(this, "Error Auth: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void uploadImageAndSaveUser(String uid, String email, String name) {
        // Definimos ruta en la nube: profile_images/{UID}.jpg
        StorageReference fileRef = mStorage.child("profile_images").child(uid + ".jpg");

        fileRef.putFile(imageUri).addOnSuccessListener(taskSnapshot -> {
            // Obtener URL pública de descarga
            fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                saveUserToDb(uid, email, name, uri.toString());
            });
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Error subiendo foto", Toast.LENGTH_SHORT).show();
            saveUserToDb(uid, email, name, ""); // Fallback si falla la imagen
        });
    }

    // Paso 3: Guardar en Base de Datos NoSQL
    private void saveUserToDb(String uid, String email, String name, String photoUrl) {
        User user = new User(uid, email, name, photoUrl);
        mDatabase.child("users").child(uid).setValue(user)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Registro exitoso", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
    }
}