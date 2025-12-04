package com.example.conectamobile;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

// Importamos la librería Hannesa2 (Fork compatible con Android 14)
import info.mqtt.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Clase controladora principal del sistema de mensajería.
 * Implementa una arquitectura híbrida:
 * 1. MQTT: Para la entrega inmediata de mensajes (Push).
 * 2. Firebase: Para la persistencia del historial y sincronización offline.
 */
public class ChatActivity extends AppCompatActivity {

    // Cliente MQTT asíncrono. Se usa el fork de Hannesa2 para soporte de WorkManager.
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
            // Recibimos el UID del destinatario desde el Intent
            targetUid = getIntent().getStringExtra("targetUid");

            // VALIDACIÓN DE SEGURIDAD:
            // Aseguramos que exista una sesión activa antes de proceder.
            if (FirebaseAuth.getInstance().getCurrentUser() == null) {
                showErrorAndExit("Error de seguridad: Sesión no válida");
                return;
            }
            if (targetUid == null) {
                showErrorAndExit("Error: Usuario destino no identificado");
                return;
            }

            myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

            // LÓGICA DE TÓPICOS (Interoperabilidad):
            // Determinamos si es un chat privado o el canal global para pruebas externas.
            if ("GLOBAL_CHAT_ID".equals(targetUid)) {
                // Caso Global: Tópico fijo accesible por clientes externos (ej. MyMQTT)
                topic = "conectamobile/global";
                chatRef = FirebaseDatabase.getInstance().getReference("chats").child("global_chat");
                setTitle("Canal Público (MQTT)");
            } else {
                // Caso Privado: Generamos un ID único ordenando los UIDs alfabéticamente.
                // Esto asegura que UsuarioA_UsuarioB sea el mismo ID que UsuarioB_UsuarioA.
                String chatId = (myUid.compareTo(targetUid) < 0) ? myUid + "_" + targetUid : targetUid + "_" + myUid;
                topic = "conectamobile/chat/" + chatId;
                chatRef = FirebaseDatabase.getInstance().getReference("chats").child(chatId);
            }

            // Inicialización de componentes de UI
            etMessage = findViewById(R.id.etMessage);
            Button btnSend = findViewById(R.id.btnSend);
            recyclerView = findViewById(R.id.recyclerChat);

            // Configuración del RecyclerView
            // setStackFromEnd(true) hace que la lista empiece desde abajo (estilo WhatsApp)
            LinearLayoutManager layoutManager = new LinearLayoutManager(this);
            layoutManager.setStackFromEnd(true);
            recyclerView.setLayoutManager(layoutManager);

            messageList = new ArrayList<>();
            adapter = new ChatAdapter(this, messageList);
            recyclerView.setAdapter(adapter);

            // 1. Cargar historial persistente (Funciona Offline)
            loadHistory();

            // 2. Iniciar conexión en tiempo real (Protocolo MQTT)
            setupMqtt();

            btnSend.setOnClickListener(v -> sendMessage());

        } catch (Exception e) {
            Log.e("ChatActivity", "Error crítico en inicialización", e);
            showErrorAndExit("Error iniciando chat: " + e.getMessage());
        }
    }

    private void showErrorAndExit(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        // Cerramos la actividad suavemente tras 2 segundos
        new android.os.Handler().postDelayed(this::finish, 2000);
    }

    /**
     * Configura e inicia el cliente MQTT.
     * Utiliza un ID de cliente aleatorio (UUID) para evitar desconexiones por conflicto en el broker.
     */
    private void setupMqtt() {
        try {
            // Generamos un ClientID único para evitar que el broker nos desconecte si abrimos la app en 2 dispositivos.
            String clientId = java.util.UUID.randomUUID().toString();

            // Constructor v4.3: Ya no requiere Ack.AUTO_ACK gracias a la implementación interna de WorkManager.
            mqttClient = new MqttAndroidClient(this.getApplicationContext(), "tcp://broker.hivemq.com:1883", clientId);

            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    Log.w("MQTT", "Conexión perdida", cause);
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    // LÓGICA DE RECEPCIÓN:
                    // Procesamos mensajes entrantes, útil para la interoperabilidad con MyMQTT.
                    String payload = new String(message.getPayload());

                    // runOnUiThread es obligatorio porque MQTT corre en un hilo secundario de red.
                    runOnUiThread(() -> {
                        // Evitamos duplicados visuales (Eco) verificando si el último mensaje es igual.
                        if (!messageList.isEmpty()) {
                            Message lastMsg = messageList.get(messageList.size() - 1);
                            if (lastMsg.text.equals(payload)) return;
                        }

                        // Agregamos visualmente el mensaje externo
                        Message externalMsg = new Message("externo", payload, System.currentTimeMillis());
                        messageList.add(externalMsg);
                        adapter.notifyItemInserted(messageList.size() - 1);
                        recyclerView.scrollToPosition(messageList.size() - 1);
                    });
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) { }
            });

            // Conectamos asíncronamente
            IMqttToken token = mqttClient.connect();
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.d("MQTT", "Conexión Exitosa (TCP 1883)");
                    subscribeToTopic();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.e("MQTT", "Fallo en conexión", exception);
                    Toast.makeText(ChatActivity.this, "Modo Offline (MQTT no conectado)", Toast.LENGTH_SHORT).show();
                }
            });

        } catch (Exception e) {
            Log.e("MQTT", "Error configuración", e);
        }
    }

    private void subscribeToTopic() {
        try {
            // Nos suscribimos con QoS 0 (At most once) para máxima velocidad
            if (mqttClient != null) mqttClient.subscribe(topic, 0);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void sendMessage() {
        String text = etMessage.getText().toString();
        if (text.isEmpty()) return;

        Message msg = new Message(myUid, text, System.currentTimeMillis());

        // 1. CAPA DE PERSISTENCIA (Firebase):
        // Garantiza que el mensaje se guarde aunque se pierda la conexión (sincronización tardía).
        chatRef.push().setValue(msg);
        etMessage.setText("");

        // 2. CAPA DE TIEMPO REAL (MQTT):
        // Intenta el envío inmediato al broker para notificar a otros clientes suscritos.
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.publish(topic, new MqttMessage(text.getBytes(StandardCharsets.UTF_8)));
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void loadHistory() {
        // Listener en tiempo real de Firebase.
        // Esto maneja la actualización de UI tanto para mensajes locales como remotos.
        chatRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@androidx.annotation.NonNull DataSnapshot snapshot) {
                try {
                    messageList.clear();
                    for (DataSnapshot snap : snapshot.getChildren()) {
                        Message msg = snap.getValue(Message.class);
                        if (msg != null) messageList.add(msg);
                    }
                    adapter.notifyDataSetChanged();
                    if (!messageList.isEmpty()) recyclerView.scrollToPosition(messageList.size() - 1);
                } catch (Exception e) { Log.e("Firebase", "Error parseando datos", e); }
            }
            @Override
            public void onCancelled(@androidx.annotation.NonNull DatabaseError error) {}
        });
    }

    /**
     * GESTIÓN DE RECURSOS:
     * Es crítico desconectar el cliente MQTT al destruir la actividad para evitar
     * fugas de memoria (Memory Leaks) y consumo de batería en segundo plano.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.disconnect();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}