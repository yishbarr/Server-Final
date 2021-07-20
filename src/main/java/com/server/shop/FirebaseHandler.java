package com.server.shop;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.*;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.server.shop.models.Product;
import com.server.shop.models.User;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class FirebaseHandler implements IHandler {
    private final String UID = "shop-server";
    private final String DATABASE_ERROR_MESSAGE = "Error reading database";
    private final int SUCCESS = 0;
    private final int DATABASE_FAILURE = 1;
    private final int ID_EXISTS = 2;
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
                JsonObject productObject = JsonParser.parseString(objectInputStream.readObject().toString()).getAsJsonObject();
                String uid = objectInputStream.readObject().toString();
                StringBuilder responseBuilder = getFromFireBaseHttp("https://shop-manager-e603d-default-rtdb.firebaseio.com/users/" + uid + "/products.json");
                String id = productObject.get("id").getAsString();
                JsonObject checkObj = JsonParser.parseString(responseBuilder.toString()).getAsJsonObject();
                JsonElement check = checkObj.get(id);
                objectOutputStream.writeInt(check != null ? ID_EXISTS : SUCCESS);
                String name = productObject.get("name").getAsString();
                int quantity = productObject.get("quantity").getAsInt();
                int shelf = productObject.get("shelf").getAsInt();
                Product product = new Product(name, id, quantity, shelf);
                DatabaseReference reference = database.getReference("users").child(uid).child("products");
                reference.orderByKey().addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        reference.child(id).setValueAsync(product);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        System.out.println(DATABASE_ERROR_MESSAGE);
                    }
                });
            }
            case "getProducts" -> {
                String uid = objectInputStream.readObject().toString();
                StringBuilder responseBuilder = getFromFireBaseHttp("https://shop-manager-e603d-default-rtdb.firebaseio.com/users/" + uid + "/products.json");
                //If no products.
                if (responseBuilder.length() == 0)
                    objectOutputStream.write(null);
                String productsString = responseBuilder.toString();
                JsonObject productsJSON = JsonParser.parseString(productsString).getAsJsonObject();
                Map<String, Map<String, String>> productsMap = new HashMap<>();
                productsJSON.keySet().forEach(key -> {
                    Map<String, String> productDetails = new HashMap<>();
                    JsonObject productDetailsObj = productsJSON.getAsJsonObject(key);
                    productDetailsObj.keySet().forEach(pKey -> productDetails.put(pKey, productDetailsObj.get(pKey).getAsString()));
                    productsMap.put(key, productDetails);
                });
                objectOutputStream.writeObject(productsMap);
            }
            case "deleteProduct" -> {
                String id = objectInputStream.readObject().toString();
                String uid = objectInputStream.readObject().toString();
                database.getReference("users").child(uid).child("products").child(id).removeValueAsync();
            }
        }
    }

    @org.jetbrains.annotations.NotNull
    private StringBuilder getFromFireBaseHttp(String urlString) throws IOException {
        HttpURLConnection connection;
        BufferedReader JsonDataLines;
        URL url = new URL(urlString);
        connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.connect();
        InputStream dataAsBytes = connection.getInputStream();
        InputStreamReader dataAsChars = new InputStreamReader(dataAsBytes);
        JsonDataLines = new BufferedReader(dataAsChars);
        StringBuilder responseBuilder = new StringBuilder();

        String oneLine;
        while ((oneLine = JsonDataLines.readLine()) != null) {
            responseBuilder.append(oneLine);
            responseBuilder.append("\n");
        }
        connection.disconnect();
        JsonDataLines.close();
        return responseBuilder;
    }
}
