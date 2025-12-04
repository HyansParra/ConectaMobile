package com.example.conectamobile;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.annotation.NonNull;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

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
            // 1. Validar Datos Iniciales
            targetUid = getIntent().getStringExtra("targetUid");

            // Validación de seguridad de sesión
            if (FirebaseAuth.getInstance().getCurrentUser() == null) {
                showErrorAndExit("Error: No hay sesión activa");
                return;
            }
            // Validación de destino
            if (targetUid == null) {
                showErrorAndExit("Error: El usuario destino no tiene ID (UID nulo)");
                return;
            }

            myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

            // 2. Configurar Referencias (Lógica de Tópico Global vs Privado)
            if ("GLOBAL_CHAT_ID".equals(targetUid)) {
                // CASO 1: CHAT PÚBLICO (Para probar con MyMQTT)
                // Usamos un tópico simple y fijo que cualquiera puede escribir
                topic = "conectamobile/global";
                // Guardamos en un nodo especial de Firebase también para historial
                chatRef = FirebaseDatabase.getInstance().getReference("chats").child("global_chat");
                setTitle("Canal Público"); // Cambiar título de la ventana
            } else {
                // CASO 2: CHAT PRIVADO (El normal de siempre)
                String chatId = (myUid.compareTo(targetUid) < 0) ? myUid + "_" + targetUid : targetUid + "_" + myUid;
                topic = "conectamobile/chat/" + chatId;
                chatRef = FirebaseDatabase.getInstance().getReference("chats").child(chatId);
            }

            // 3. Configurar UI
            etMessage = findViewById(R.id.etMessage);
            Button btnSend = findViewById(R.id.btnSend);
            recyclerView = findViewById(R.id.recyclerChat);

            LinearLayoutManager layoutManager = new LinearLayoutManager(this);
            layoutManager.setStackFromEnd(true);
            recyclerView.setLayoutManager(layoutManager);

            messageList = new ArrayList<>();
            adapter = new ChatAdapter(this, messageList);
            recyclerView.setAdapter(adapter);

            loadHistory();

            // 4. Iniciar MQTT
            setupMqtt();

            btnSend.setOnClickListener(v -> sendMessage());

        } catch (Exception e) {
            Log.e("ChatActivity", "Error crítico UI", e);
            showErrorAndExit("Error iniciando chat: " + e.getMessage());
        }
    }

    private void showErrorAndExit(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        // Le damos 2 segundos al usuario para leer el error antes de cerrar
        new android.os.Handler().postDelayed(this::finish, 2000);
    }

    private void setupMqtt() {
        try {
            // Usamos UUID para evitar problemas de ID duplicado
            String clientId = java.util.UUID.randomUUID().toString();

            // CÓDIGO CORREGIDO (Versión 4.3):
            mqttClient = new MqttAndroidClient(this.getApplicationContext(), "tcp://broker.hivemq.com:1883", clientId);

            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) { }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    // Procesamos lo que llega de MyMQTT
                    String payload = new String(message.getPayload());

                    // Usamos runOnUiThread porque MQTT llega en otro hilo y no puede tocar la UI directo
                    runOnUiThread(() -> {
                        // Creamos un mensaje "falso" que viene de fuera
                        // Usamos un ID "externo" para que se vea a la izquierda (gris)
                        Message mqttMsg = new Message("usuario_externo_mymqtt", payload, System.currentTimeMillis());

                        // Pequeño truco para evitar ver mis propios mensajes duplicados (Eco)
                        // Si el último mensaje es igual al que acaba de llegar, lo ignoramos
                        if (!messageList.isEmpty()) {
                            Message lastMsg = messageList.get(messageList.size() - 1);
                            if (lastMsg.text.equals(payload)) {
                                return; // Es mi propio mensaje que volvió, no lo mostramos de nuevo
                            }
                        }

                        // Agregamos a la lista y actualizamos
                        messageList.add(mqttMsg);
                        adapter.notifyItemInserted(messageList.size() - 1);
                        recyclerView.scrollToPosition(messageList.size() - 1);
                    });
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
                    // Solo log, no cerramos la app si falla MQTT
                    Log.e("MQTT", "Fallo conexión", exception);
                    Toast.makeText(ChatActivity.this, "Aviso: Chat en modo solo Firebase (MQTT falló)", Toast.LENGTH_SHORT).show();
                }
            });

        } catch (Exception e) {
            Log.e("MQTT", "Error configuración", e);
            // No cerramos la actividad, permitimos que funcione solo con Firebase
        }
    }

    private void subscribeToTopic() {
        try {
            if (mqttClient != null) mqttClient.subscribe(topic, 0);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void sendMessage() {
        String text = etMessage.getText().toString();
        if (text.isEmpty()) return;

        Message msg = new Message(myUid, text, System.currentTimeMillis());

        // Firebase primero (garantía de guardado)
        chatRef.push().setValue(msg);
        etMessage.setText("");

        // MQTT intento (best effort)
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.publish(topic, new MqttMessage(text.getBytes(StandardCharsets.UTF_8)));
            }
        } catch (Exception e) { e.printStackTrace(); }
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
                    if (!messageList.isEmpty()) recyclerView.scrollToPosition(messageList.size() - 1);
                } catch (Exception e) { Log.e("Firebase", "Error datos", e); }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (mqttClient != null) {
                // En la versión 4.3, a veces solo desconectar es suficiente y más seguro
                if (mqttClient.isConnected()) {
                    mqttClient.disconnect();
                }
                // mqttClient.close(); // Comenta esto si da error, a veces no es necesario
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}