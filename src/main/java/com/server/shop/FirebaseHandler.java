package com.server.shop;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.server.shop.models.Product;
import com.server.shop.models.User;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class FirebaseHandler implements IHandler {
    private static final String DATABASE_BLANK_MESSAGE = "No existing products.";
    private final String UID = "shop-server";
    private final String DATABASE_URL = "https://shop-manager-e603d-default-rtdb.firebaseio.com";
    private final FirebaseDatabase database;

    public FirebaseHandler() {
        Map<String, Object> auth = new HashMap<>();
        auth.put("uid", UID);

        try {
            FileInputStream serviceAccount = new FileInputStream("src/main/resources/shop-manager-e603d-firebase-adminsdk-ifukw-49aa40034b.json");
            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setDatabaseUrl(DATABASE_URL)
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
        //Client request type
        switch (objectInputStream.readObject().toString()) {
            //Add account to database.
            case "addAccount" -> {
                String email = objectInputStream.readObject().toString();
                String shop = objectInputStream.readObject().toString();
                String uid = objectInputStream.readObject().toString();
                User user = new User(email, shop, uid);
                DatabaseReference ref = database.getReference("users");
                ref.child(uid).setValueAsync(user);
            }
            //Add product
            case "addProduct" -> {
                JsonObject productObject = JsonParser.parseString(objectInputStream.readObject().toString()).getAsJsonObject();
                String uid = objectInputStream.readObject().toString();

                String id = productObject.get("id").getAsString();
                String name = productObject.get("name").getAsString();
                int quantity = productObject.get("quantity").getAsInt();
                int shelf = productObject.get("shelf").getAsInt();
                Product product = new Product(name, id, quantity, shelf);
                database.getReference("users").child(uid).child("products").child(id).setValueAsync(product);
            }

            //Get products and send to client.
            case "getProducts" -> {
                String uid = objectInputStream.readObject().toString();
                StringBuilder responseBuilder = getFromFireBaseHttp("https://shop-manager-e603d-default-rtdb.firebaseio.com/users/" + uid + "/products.json");
                //If no products.
                String productsString = responseBuilder.toString();
                JsonObject productsJSON = null;
                try {
                    productsJSON = JsonParser.parseString(productsString).getAsJsonObject();
                } catch (IllegalStateException e) {
                    System.out.println(DATABASE_BLANK_MESSAGE);
                }
                if (productsJSON == null) {
                    objectOutputStream.writeObject(null);
                    return;
                }
                Map<String, Map<String, String>> productsMap = new HashMap<>();
                JsonObject finalProductsJSON = productsJSON;
                productsJSON.keySet().forEach(key -> {
                    Map<String, String> productDetails = new HashMap<>();
                    JsonObject productDetailsObj = finalProductsJSON.getAsJsonObject(key);
                    productDetailsObj.keySet().forEach(pKey -> productDetails.put(pKey, productDetailsObj.get(pKey).getAsString()));
                    productsMap.put(key, productDetails);
                });
                objectOutputStream.writeObject(productsMap);
            }
            //Delete product by product id.
            case "deleteProduct" -> {
                String id = objectInputStream.readObject().toString();
                String uid = objectInputStream.readObject().toString();
                database.getReference("users").child(uid).child("products").child(id).removeValueAsync();
            }
            //update product by product id.
            case "updateProduct" -> {
                JsonObject productObject = JsonParser.parseString(objectInputStream.readObject().toString()).getAsJsonObject();
                String uid = objectInputStream.readObject().toString();
                String id = productObject.get("id").getAsString();
                String name = productObject.get("name").getAsString();
                int quantity = productObject.get("quantity").getAsInt();
                int shelf = productObject.get("shelf").getAsInt();
                database.getReference("users").child(uid).child("products").child(id).setValueAsync(new Product(name, id, shelf, quantity));
            }
            case "getIds" -> {
                String uid = objectInputStream.readObject().toString();
                StringBuilder responseBuilder = getFromFireBaseHttp("https://shop-manager-e603d-default-rtdb.firebaseio.com/users/" + uid + "/products.json");
                JsonObject checkObj = null;
                try {
                    checkObj = JsonParser.parseString(responseBuilder.toString()).getAsJsonObject();
                } catch (IllegalStateException e) {
                    System.out.println(DATABASE_BLANK_MESSAGE);
                }
                ArrayList<String> keys = new ArrayList<>();
                if (checkObj != null)
                    keys = new ArrayList<>(Objects.requireNonNull(checkObj).keySet());
                objectOutputStream.writeObject(keys);
            }
        }
    }

    //Get firebase data with classic HTTP request instead of the firebase functions.
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
