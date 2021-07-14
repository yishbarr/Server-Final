package com.server.shop;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.server.shop.models.Product;
import com.server.shop.models.User;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class FirebaseHandler implements IHandler {
    private final String uid = "shop-server";
    private FileInputStream serviceAccount;
    private FirebaseOptions options;
    private FirebaseDatabase database;

    public FirebaseHandler() {
        Map<String, Object> auth = new HashMap<>();
        auth.put("uid", uid);

        try {
            serviceAccount = new FileInputStream(new File("src/main/resources/shop-manager-e603d-firebase-adminsdk-x699q-54761739e9.json"));
            options = new FirebaseOptions.Builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setDatabaseUrl("https://shop-manager-e603d-default-rtdb.firebaseio.com")
                    .setDatabaseAuthVariableOverride(auth)
                    .build();
            FirebaseApp.initializeApp(options);
        } catch (IOException e) {
            e.printStackTrace();
        }
        database = FirebaseDatabase.getInstance();
    }

    @Override
    public void handle(InputStream req, OutputStream res) throws IOException, ClassNotFoundException {
        ObjectInputStream objectInputStream = new ObjectInputStream(req);
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(res);

        switch (objectInputStream.readObject().toString()) {
            case "addAccount": {
                String email = objectInputStream.readObject().toString();
                String shop = objectInputStream.readObject().toString();
                String uid = objectInputStream.readObject().toString();
                User user = new User(email, shop, uid);
                DatabaseReference ref = database.getReference("users");
                ref.child(uid).setValueAsync(user);
                break;
            }
            case "addProduct": {
                String name = objectInputStream.readObject().toString();
                String id = objectInputStream.readObject().toString();
                int quantity = objectInputStream.readInt();
                int shelf = objectInputStream.readInt();
                String uid = objectInputStream.readObject().toString();
                Product product = new Product(name, id, quantity, shelf);
                new Thread(() -> database.getReference("users").child(uid).child("products").child(product.id).setValue(product, (error, ref) -> {
                    try {
                        if (error != null)
                            objectOutputStream.writeBoolean(false);
                        else objectOutputStream.writeBoolean(true);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                })).start();
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                break;
            }
        }
    }
}
