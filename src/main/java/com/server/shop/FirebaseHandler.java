package com.server.shop;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

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
                User user = new User(email, shop);
                DatabaseReference ref = database.getReference("users");
                ref.child(uid).setValueAsync(user);
            }
        }
    }
}
