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

// Imports de MQTT (Hannesa2 + Paho)
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

        // Recuperar datos del intent
        targetUid = getIntent().getStringExtra("targetUid");
        // Evitar crash si no hay usuario logueado (por seguridad)
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        } else {
            finish();
            return;
        }

        // Generar ID único de chat (A_B o B_A)
        String chatId = (myUid.compareTo(targetUid) < 0) ? myUid + "_" + targetUid : targetUid + "_" + myUid;
        topic = "conectamobile/chat/" + chatId;
        chatRef = FirebaseDatabase.getInstance().getReference("chats").child(chatId);

        // Configurar UI
        etMessage = findViewById(R.id.etMessage);
        Button btnSend = findViewById(R.id.btnSend);
        recyclerView = findViewById(R.id.recyclerChat);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        messageList = new ArrayList<>();
        adapter = new ChatAdapter(this, messageList);
        recyclerView.setAdapter(adapter);

        // 1. Cargar historial de Firebase
        loadHistory();

        // 2. Conectar MQTT
        setupMqtt();

        btnSend.setOnClickListener(v -> sendMessage());
    }

    private void loadHistory() {
        chatRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                messageList.clear();
                for (DataSnapshot snap : snapshot.getChildren()) {
                    Message msg = snap.getValue(Message.class);
                    if (msg != null) messageList.add(msg);
                }
                adapter.notifyDataSetChanged();
                // Scroll al último mensaje
                if (!messageList.isEmpty()) {
                    recyclerView.scrollToPosition(messageList.size() - 1);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void setupMqtt() {
        // Usamos el constructor especial para Android 12+
        mqttClient = new MqttAndroidClient(this.getApplicationContext(), "tcp://broker.hivemq.com:1883", myUid, Ack.AUTO_ACK);

        // CAMBIO CLAVE: Sin try-catch porque esta librería no lanza checked exceptions aquí
        IMqttToken token = mqttClient.connect();
        token.setActionCallback(new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
                Log.d("MQTT", "Conectado exitosamente");
                subscribeToTopic();
            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                Log.e("MQTT", "Fallo conexión", exception);
                Toast.makeText(ChatActivity.this, "Error conectando a chat", Toast.LENGTH_SHORT).show();
            }
        });

        mqttClient.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                Log.d("MQTT", "Conexión perdida");
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                // Aquí llega el mensaje en tiempo real
                // Opcional: Mostrar notificación o actualizar UI si Firebase falla
                // Por ahora confiamos en el listener de Firebase para la UI
                Log.d("MQTT", "Mensaje recibido: " + new String(message.getPayload()));
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
            }
        });
    }

    private void subscribeToTopic() {
        // CAMBIO CLAVE: Sin try-catch
        mqttClient.subscribe(topic, 0);
    }

    private void sendMessage() {
        String text = etMessage.getText().toString();
        if (text.isEmpty()) return;

        Message msg = new Message(myUid, text, System.currentTimeMillis());

        // 1. Publicar en MQTT (Sin try-catch)
        if (mqttClient.isConnected()) {
            mqttClient.publish(topic, new MqttMessage(text.getBytes(StandardCharsets.UTF_8)));
        }

        // 2. Guardar en Firebase (Esto actualiza la UI automáticamente por el listener)
        chatRef.push().setValue(msg);

        etMessage.setText("");
    }
}