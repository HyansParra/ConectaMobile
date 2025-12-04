package com.example.conectamobile;

public class User {
    public String uid;
    public String email;
    public String name;
    public String photoUrl; // Nuevo campo para la foto de perfil


    public User() { }

    public User(String uid, String email, String name, String photoUrl) {
        this.uid = uid;
        this.email = email;
        this.name = name;
        this.photoUrl = photoUrl;
    }
}