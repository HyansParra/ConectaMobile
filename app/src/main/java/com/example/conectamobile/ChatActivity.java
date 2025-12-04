package com.example.conectamobile;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import info.mqtt.android.service.Ack;
import info.mqtt.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    private MqttAndroidClient mqttClient;
    private String topic;
    private String myUid, targetUid;
    private DatabaseReference chatRef;

    private RecyclerView recyclerView;
    private ChatAdapter adapter;
    private List<Message> messageList;
    private EditText etMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        try {
            // 1. Evitar crash si los datos vienen vacíos
            targetUid = getIntent().getStringExtra("targetUid");
            if (FirebaseAuth.getInstance().getCurrentUser() == null || targetUid == null) {
                Toast.makeText(this, "Error: Usuario no identificado", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

            // 2. Configurar Firebase
            String chatId = (myUid.compareTo(targetUid) < 0) ? myUid + "_" + targetUid : targetUid + "_" + myUid;
            topic = "conectamobile/chat/" + chatId;
            chatRef = FirebaseDatabase.getInstance().getReference("chats").child(chatId);

            // 3. Configurar UI
            etMessage = findViewById(R.id.etMessage);
            Button btnSend = findViewById(R.id.btnSend);
            recyclerView = findViewById(R.id.recyclerChat);

            // Layout Manager seguro
            LinearLayoutManager layoutManager = new LinearLayoutManager(this);
            layoutManager.setStackFromEnd(true); // Para que empiece desde abajo
            recyclerView.setLayoutManager(layoutManager);

            messageList = new ArrayList<>();
            adapter = new ChatAdapter(this, messageList);
            recyclerView.setAdapter(adapter);

            loadHistory();

            // Iniciamos MQTT con un pequeño retraso para asegurar que la UI esté lista
            setupMqtt();

            btnSend.setOnClickListener(v -> sendMessage());

        } catch (Exception e) {
            Log.e("ChatActivity", "Error crítico en onCreate", e);
            finish();
        }
    }

    private void setupMqtt() {
        try {
            // CORRECCIÓN: Usamos UUID de Java para generar un ID único y evitar el error
            String clientId = java.util.UUID.randomUUID().toString();

            mqttClient = new MqttAndroidClient(this.getApplicationContext(), "tcp://broker.hivemq.com:1883", clientId, Ack.AUTO_ACK);

            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) { Log.d("MQTT", "Conexión perdida"); }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    // No hacemos nada aquí, Firebase maneja la UI
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) { }
            });

            IMqttToken token = mqttClient.connect();
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.d("MQTT", "Conectado");
                    subscribeToTopic();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.e("MQTT", "Fallo al conectar", exception);
                }
            });

        } catch (Exception e) {
            Log.e("MQTT", "Error iniciando MQTT", e);
        }
    }

    private void subscribeToTopic() {
        try {
            if (mqttClient != null) {
                mqttClient.subscribe(topic, 0);
            }
        } catch (Exception e) {
            Log.e("MQTT", "Error suscribiendo", e);
        }
    }

    private void sendMessage() {
        String text = etMessage.getText().toString();
        if (text.isEmpty()) return;

        // 1. Enviar a Firebase (Prioridad: Esto asegura que el mensaje se guarde)
        Message msg = new Message(myUid, text, System.currentTimeMillis());
        chatRef.push().setValue(msg);
        etMessage.setText("");

        // 2. Intentar enviar por MQTT (Si falla, no pasa nada)
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.publish(topic, new MqttMessage(text.getBytes(StandardCharsets.UTF_8)));
            }
        } catch (Exception e) {
            Log.e("MQTT", "Error enviando mensaje MQTT", e);
        }
    }

    private void loadHistory() {
        chatRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                try {
                    messageList.clear();
                    for (DataSnapshot snap : snapshot.getChildren()) {
                        Message msg = snap.getValue(Message.class);
                        if (msg != null) messageList.add(msg);
                    }
                    adapter.notifyDataSetChanged();
                    if (!messageList.isEmpty()) {
                        recyclerView.scrollToPosition(messageList.size() - 1);
                    }
                } catch (Exception e) {
                    Log.e("Firebase", "Error procesando datos", e);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // --- EL SALVAVIDAS: Evita el crash al salir ---
    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (mqttClient != null) {
                mqttClient.unregisterResources();
                mqttClient.close();
                mqttClient.disconnect();
            }
        } catch (Exception e) {
            // Ignoramos errores al cerrar, lo importante es que no crashee
        }
    }
}