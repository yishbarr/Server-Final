package com.server.shop.models;

public class User {
    public String email;
    public String shop;
    public String uid;

    public User(String email, String shop, String uid) {
        this.email = email;
        this.shop = shop;
        this.uid = uid;
    }
}
