package com.example.conectamobile;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

/**
 * Controlador de Perfil de Usuario.
 * Implementa operaciones CRUD (Read, Update, Delete) sobre la foto de perfil.
 * Utiliza Glide para la carga eficiente de la imagen remota.
 */
public class ProfileActivity extends AppCompatActivity {

    private ImageView ivProfile;
    private TextView tvName, tvEmail;
    private DatabaseReference mDatabase;
    private StorageReference mStorage;
    private String myUid;

    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    uploadImage(imageUri); // Subida automática al seleccionar
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            finish();
            return;
        }
        myUid = user.getUid();
        mDatabase = FirebaseDatabase.getInstance().getReference("users").child(myUid);
        mStorage = FirebaseStorage.getInstance().getReference();

        ivProfile = findViewById(R.id.ivProfileCurrent);
        tvName = findViewById(R.id.tvProfileName);
        tvEmail = findViewById(R.id.tvProfileEmail);
        Button btnChange = findViewById(R.id.btnChangePhoto);
        Button btnDelete = findViewById(R.id.btnDeletePhoto);

        loadUserData();

        btnChange.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            galleryLauncher.launch(intent);
        });

        // Opción de eliminar (Pone la URL en blanco)
        btnDelete.setOnClickListener(v -> deletePhoto());
    }

    private void loadUserData() {
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (user != null) {
                    tvName.setText(user.name);
                    tvEmail.setText(user.email);

                    // Carga asíncrona con Glide
                    if (user.photoUrl != null && !user.photoUrl.isEmpty()) {
                        Glide.with(ProfileActivity.this)
                                .load(user.photoUrl)
                                .circleCrop() // Recorte circular automático
                                .into(ivProfile);
                    } else {
                        ivProfile.setImageResource(R.mipmap.ic_launcher_round);
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void uploadImage(Uri imageUri) {
        Toast.makeText(this, "Subiendo foto...", Toast.LENGTH_SHORT).show();

        // Sobreescribir archivo existente para ahorrar espacio
        StorageReference fileRef = mStorage.child("profile_images").child(myUid + ".jpg");

        fileRef.putFile(imageUri).addOnSuccessListener(taskSnapshot -> {
            fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                // Actualizar referencia en la base de datos
                mDatabase.child("photoUrl").setValue(uri.toString());
                Toast.makeText(this, "Foto actualizada", Toast.LENGTH_SHORT).show();
            });
        }).addOnFailureListener(e ->
                Toast.makeText(this, "Error al subir", Toast.LENGTH_SHORT).show()
        );
    }

    private void deletePhoto() {
        // Borrado lógico: Se elimina la referencia URL, volviendo al icono por defecto
        mDatabase.child("photoUrl").setValue("").addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "Foto eliminada", Toast.LENGTH_SHORT).show();
                ivProfile.setImageResource(R.mipmap.ic_launcher_round);
            }
        });
    }
}