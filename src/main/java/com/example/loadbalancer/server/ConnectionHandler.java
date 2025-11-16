package com.example.loadbalancer.server;

import com.example.loadbalancer.core.Backend;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.function.Consumer;

class ConnectionHandler implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionHandler.class);
    private static final int BUFFER_SIZE = 8192;

    private final SocketChannel clientChannel;
    private final Backend backend;
    private final Consumer<SocketChannel> onClose;
    private SocketChannel backendChannel;
    private volatile boolean running = true;

    public ConnectionHandler(SocketChannel clientChannel, Backend backend, Consumer<SocketChannel> onClose) {
        this.clientChannel = clientChannel;
        this.backend = backend;
        this.onClose = onClose;
    }

    @Override
    public void run() {
        try {
            backendChannel = SocketChannel.open(backend.getAddress());
            backendChannel.configureBlocking(false);

            Thread clientToBackend = new Thread(() -> forward(clientChannel, backendChannel, "client->backend"));
            Thread backendToClient = new Thread(() -> forward(backendChannel, clientChannel, "backend->client"));

            clientToBackend.start();
            backendToClient.start();

            clientToBackend.join();
            backendToClient.join();

        } catch (IOException e) {
            logger.error("Error establishing connection to backend {}", backend, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Connection handler interrupted");
        } finally {
            close();
        }
    }

    private void forward(SocketChannel source, SocketChannel destination, String direction) {
        ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
        try {
            while (running) {
                buffer.clear();
                int bytesRead = source.read(buffer);

                if (bytesRead == -1) {
                    break;
                }

                if (bytesRead > 0) {
                    buffer.flip();
                    while (buffer.hasRemaining()) {
                        destination.write(buffer);
                    }
                }

                Thread.sleep(1);
            }
        } catch (IOException e) {
            if (running) {
                logger.debug("Error forwarding data {}: {}", direction, e.getMessage());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void close() {
        running = false;
        closeChannel(clientChannel);
        closeChannel(backendChannel);
        onClose.accept(clientChannel);
    }

    private void closeChannel(SocketChannel channel) {
        if (channel != null && channel.isOpen()) {
            try {
                channel.close();
            } catch (IOException e) {
                logger.debug("Error closing channel", e);
            }
        }
    }

    public Backend getBackend() {
        return backend;
    }
}
