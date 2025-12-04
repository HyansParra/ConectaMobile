package com.example.conectamobile;

public class User {
    public String uid;
    public String email;
    public String name;

    public User() { } // Constructor vacío requerido por Firebase

    public User(String uid, String email, String name) {
        this.uid = uid;
        this.email = email;
        this.name = name;
    }
}