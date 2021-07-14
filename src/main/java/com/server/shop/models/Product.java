package com.server.shop.models;

public class Product {
    public String name;
    public String id;
    public int shelf;
    public int quantity;

    public Product(String name, String id, int shelf, int quantity) {
        this.name = name;
        this.id = id;
        this.shelf = shelf;
        this.quantity = quantity;
    }
}
