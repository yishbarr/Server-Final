package com.server.shop;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.*;
import com.server.shop.models.Product;
import com.server.shop.models.User;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class FirebaseHandler implements IHandler {
    private final String UID = "shop-server";
    private final String DATABASE_ERROR_MESSAGE = "Error reading database";
    private final int SUCCESS = 0;
    private final int DATABASE_FAILURE = 1;
    private final FirebaseDatabase database;

    public FirebaseHandler() {
        Map<String, Object> auth = new HashMap<>();
        auth.put("uid", UID);

        try {
            FileInputStream serviceAccount = new FileInputStream("src/main/resources/shop-manager-e603d-firebase-adminsdk-x699q-54761739e9.json");
            FirebaseOptions options = new FirebaseOptions.Builder()
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
            case "addAccount" -> {
                String email = objectInputStream.readObject().toString();
                String shop = objectInputStream.readObject().toString();
                String uid = objectInputStream.readObject().toString();
                User user = new User(email, shop, uid);
                DatabaseReference ref = database.getReference("users");
                ref.child(uid).setValueAsync(user);
            }
            case "addProduct" -> {
                //Response codes for client.
                final int ID_EXISTS = 2;

                String name = objectInputStream.readObject().toString();
                String id = objectInputStream.readObject().toString();
                int quantity = objectInputStream.readInt();
                int shelf = objectInputStream.readInt();
                String uid = objectInputStream.readObject().toString();
                Product product = new Product(name, id, quantity, shelf);
                new Thread(() -> {
                    DatabaseReference reference = database.getReference("users").child(uid).child("products");
                    reference.orderByKey().addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot snapshot) {
                            boolean idExists = false;
                            for (DataSnapshot c : snapshot.getChildren()) {
                                if (c.getKey().contentEquals(id + "")) {
                                    System.out.println("ID already exists");
                                    idExists = true;
                                    break;
                                }
                            }
                            if (!idExists)
                                reference.child(id).setValue(product, (error, ref) -> {
                                    try {
                                        if (error != null)
                                            objectOutputStream.writeInt(SUCCESS);
                                        else objectOutputStream.writeInt(DATABASE_FAILURE);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                });
                            else {
                                try {
                                    objectOutputStream.writeInt(ID_EXISTS);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError error) {
                            System.out.println(DATABASE_ERROR_MESSAGE);
                            try {
                                objectOutputStream.writeInt(DATABASE_FAILURE);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }).start();
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            case "getProducts" -> {
                String uid = objectInputStream.readObject().toString();
                database.getReference("users").child(uid).child("products").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        Map<String, Map<String, String>> products = new HashMap<>();
                        snapshot.getChildren().forEach(c -> {
                            Map<String, String> productDetails = new HashMap<>();
                            c.getChildren().forEach(d -> productDetails.put(d.getKey(), d.getValue().toString()));
                            products.put(c.getKey(), productDetails);
                        });
                        try {
                            objectOutputStream.writeObject(products);
                            objectOutputStream.writeInt(SUCCESS);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        System.out.println(DATABASE_ERROR_MESSAGE);
                        try {
                            objectOutputStream.writeInt(DATABASE_FAILURE);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });

            }
        }
    }
}
