package com.server.shop;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface IHandler {
    void handle(InputStream req, OutputStream res) throws IOException, ClassNotFoundException, InterruptedException;
}
