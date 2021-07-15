package com.server.shop;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class TcpServer {
    //Port number
    private final int port;
    //Whether to stop the server
    private boolean stopServer;

    public TcpServer(int port) {
        this.port = port;
    }

    //Use the function for each handler request. Every handler does something else.
    public void handleClient(IHandler requestHandler) {
        //Threadpool assignment
        //ThreadPool for assigning a limited amount of threads
        ThreadPoolExecutor threadPool = new ThreadPoolExecutor(3, 5, 10, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
        try {
            //Open socket on port
            ServerSocket serverSocket = new ServerSocket(port);
            //Wait for client requests
            while (!stopServer) {
                //If client connects, it will assign the connection. If not, it will throw an exception to the catcher and attempt another connection.
                Socket clientConnection = serverSocket.accept();
                //Handle the connection with the handler.
                Runnable clientHandle = () -> {
                    System.out.println("New client request");
                    try {
                        requestHandler.handle(clientConnection.getInputStream(), clientConnection.getOutputStream());
                        clientConnection.close();
                    } catch (IOException | ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                };
                threadPool.execute(clientHandle);
            }
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*public void stop() {
        if (!stopServer) {
            try {
                //Let other threads close
                Thread.sleep(20000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            stopServer = true;
            threadPool.shutdown();
        }
    }*/

}
