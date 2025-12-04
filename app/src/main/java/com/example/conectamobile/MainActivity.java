package com.example.conectamobile;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;

/**
 * Controlador de la Pantalla Principal.
 * Responsabilidades:
 * 1. Listar contactos registrados en Firebase (excepto el usuario actual).
 * 2. Gestionar la navegación hacia Chats Privados o el Perfil.
 * 3. Proveer acceso rápido al Canal Global (Interoperabilidad).
 */
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

        // Seguridad: Validar sesión
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            goToLogin();
            return;
        }

        myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        mDatabase = FirebaseDatabase.getInstance().getReference("users");

        // Configuración de Lista (RecyclerView)
        recyclerView = findViewById(R.id.recyclerViewUsers);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        userList = new ArrayList<>();
        adapter = new UserAdapter(this, userList);
        recyclerView.setAdapter(adapter);

        // Botón Flotante (FAB) para Chat Global
        // Permite probar la comunicación MQTT con clientes externos
        FloatingActionButton fabGlobal = findViewById(R.id.fabGlobalChat);
        fabGlobal.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ChatActivity.class);
            // Flags especiales para indicar modo "Canal Público"
            intent.putExtra("targetUid", "GLOBAL_CHAT_ID");
            intent.putExtra("targetName", "Canal Público (MyMQTT)");
            startActivity(intent);
            Toast.makeText(this, "Entrando a Chat Público...", Toast.LENGTH_SHORT).show();
        });

        // Cargar usuarios en tiempo real
        loadUsers();
    }

    private void loadUsers() {
        // Listener que se mantiene activo para detectar nuevos usuarios automáticamente
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                userList.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    User user = data.getValue(User.class);
                    // Filtrar: No mostrarme a mí mismo en la lista de contactos
                    if (user != null && user.uid != null && !user.uid.equals(myUid)) {
                        userList.add(user);
                    }
                }
                adapter.notifyDataSetChanged(); // Refrescar UI
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    // Menú de Opciones (Top Bar)
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_logout) {
            FirebaseAuth.getInstance().signOut();
            goToLogin();
            return true;
        } else if (id == R.id.action_profile) {
            startActivity(new Intent(this, ProfileActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void goToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}