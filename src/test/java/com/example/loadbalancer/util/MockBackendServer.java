package com.example.loadbalancer.util;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class MockBackendServer {

    private final int port;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private ServerSocket serverSocket;
    private ExecutorService executorService;
    private int connectionCount = 0;

    public MockBackendServer(int port) {
        this.port = port;
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        executorService = Executors.newCachedThreadPool();
        running.set(true);

        executorService.submit(() -> {
            while (running.get()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    connectionCount++;
                    executorService.submit(() -> handleClient(clientSocket));
                } catch (IOException e) {
                    if (running.get()) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private void handleClient(Socket clientSocket) {
        try {
            byte[] buffer = new byte[1024];
            while (running.get() && !clientSocket.isClosed()) {
                int bytesRead = clientSocket.getInputStream().read(buffer);
                if (bytesRead == -1) {
                    break;
                }
                if (bytesRead > 0) {
                    clientSocket.getOutputStream().write(buffer, 0, bytesRead);
                }
            }
        } catch (IOException e) {
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
            }
        }
    }

    public void stop() {
        running.set(false);
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
            }
        }
        if (executorService != null) {
            executorService.shutdown();
        }
    }

    public int getPort() {
        return port;
    }

    public int getConnectionCount() {
        return connectionCount;
    }

    public boolean isRunning() {
        return running.get();
    }
}




