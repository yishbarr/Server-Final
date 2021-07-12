package com.server.shop;

import java.io.*;

public class WriteReturn implements IHandler {
    @Override
    public void handle(InputStream req, OutputStream res) throws IOException, ClassNotFoundException {
        ObjectInputStream objectInputStream = new ObjectInputStream(req);
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(res);
        System.out.println(objectInputStream.readObject());
    }
}
