package com.example.conectamobile;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast; // Importar Toast
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton; // Importar FAB para el chat global
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private UserAdapter adapter;
    private List<User> userList;
    private DatabaseReference mDatabase;
    private String myUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Verificación de seguridad: Si no hay usuario, volver al login
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            goToLogin();
            return;
        }

        myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        mDatabase = FirebaseDatabase.getInstance().getReference("users");

        recyclerView = findViewById(R.id.recyclerViewUsers);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        userList = new ArrayList<>();
        adapter = new UserAdapter(this, userList);
        recyclerView.setAdapter(adapter);

        // --- LÓGICA NUEVA PARA EL CHAT GLOBAL ---
        FloatingActionButton fabGlobal = findViewById(R.id.fabGlobalChat);
        fabGlobal.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ChatActivity.class);
            // Usamos códigos especiales para indicar que es el chat público
            intent.putExtra("targetUid", "GLOBAL_CHAT_ID");
            intent.putExtra("targetName", "Canal Público (MyMQTT)");
            startActivity(intent);
            Toast.makeText(this, "Entrando a Chat Público...", Toast.LENGTH_SHORT).show();
        });
        // ----------------------------------------

        loadUsers();
    }

    private void loadUsers() {
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                userList.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    User user = data.getValue(User.class);
                    // Mostrar a todos menos a mí mismo
                    if (user != null && user.uid != null && !user.uid.equals(myUid)) {
                        userList.add(user);
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    // --- CÓDIGO PARA EL MENÚ DE LOGOUT ---

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId(); // Obtener ID pulsado

        if (id == R.id.action_logout) {
            FirebaseAuth.getInstance().signOut();
            goToLogin();
            return true;
        }
        // --- NUEVO: Ir a Perfil ---
        else if (id == R.id.action_profile) {
            startActivity(new Intent(this, ProfileActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void goToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        // Esto limpia el historial para que no pueda volver atrás con el botón "Back"
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}