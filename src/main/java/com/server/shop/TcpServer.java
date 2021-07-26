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
    private boolean stopServer;

    //Whether to stop the server
    public TcpServer(int port) {
        this.port = port;
        stopServer = false;
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
                    } catch (IOException | ClassNotFoundException | InterruptedException e) {
                        e.printStackTrace();
                    }
                    finally {
                        try {
                            clientConnection.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                };
                threadPool.execute(clientHandle);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
