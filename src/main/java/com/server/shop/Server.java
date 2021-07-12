package com.server.shop;

import org.apache.log4j.BasicConfigurator;

public class Server {
    public static void main(String[] args) {
        BasicConfigurator.configure();
        TcpServer server = new TcpServer(3000);
        server.handleClient(new FirebaseHandler());
    }
}
