package com.example.conectamobile;

/**
 * Modelo de Datos: Usuario.
 * Clase POJO (Plain Old Java Object) requerida por Firebase.
 * Define la estructura de datos que se guarda en el nodo 'users'.
 */
public class User {
    public String uid;
    public String email;
    public String name;
    public String photoUrl;

    // Constructor vacío OBLIGATORIO para la deserialización de Firebase
    public User() { }

    public User(String uid, String email, String name, String photoUrl) {
        this.uid = uid;
        this.email = email;
        this.name = name;
        this.photoUrl = photoUrl;
    }
}