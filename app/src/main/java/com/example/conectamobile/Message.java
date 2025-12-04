package com.example.conectamobile;

/**
 * Modelo de Datos: Mensaje.
 * Estructura para el intercambio de información entre clientes.
 * Se utiliza tanto para serialización MQTT (JSON/Bytes) como para Firebase.
 */
public class Message {
    public String senderId;
    public String text;
    public long timestamp; // Marca de tiempo para ordenamiento

    // Constructor vacío OBLIGATORIO para Firebase
    public Message() { }

    public Message(String senderId, String text, long timestamp) {
        this.senderId = senderId;
        this.text = text;
        this.timestamp = timestamp;
    }
}