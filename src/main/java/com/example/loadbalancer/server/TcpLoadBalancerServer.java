package com.example.loadbalancer.server;

import com.example.loadbalancer.config.LoadBalancerProperties;
import com.example.loadbalancer.core.Backend;
import com.example.loadbalancer.core.BackendPool;
import com.example.loadbalancer.core.LoadBalancingAlgorithm;
import com.example.loadbalancer.health.HealthChecker;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class TcpLoadBalancerServer {

    private static final Logger logger = LoggerFactory.getLogger(TcpLoadBalancerServer.class);
    private final LoadBalancerProperties properties;
    private final BackendPool backendPool;
    private final LoadBalancingAlgorithm algorithm;
    private final HealthChecker healthChecker;
    private ServerSocketChannel serverChannel;
    private Selector selector;
    private ExecutorService executorService;
    private volatile boolean running = false;
    private final Map<SocketChannel, ConnectionHandler> activeConnections = new ConcurrentHashMap<>();

    public TcpLoadBalancerServer(
            LoadBalancerProperties properties,
            BackendPool backendPool,
            LoadBalancingAlgorithm algorithm,
            HealthChecker healthChecker) {
        this.properties = properties;
        this.backendPool = backendPool;
        this.algorithm = algorithm;
        this.healthChecker = healthChecker;
    }

    @PostConstruct
    public void start() throws IOException {
        initializeBackends();
        startServer();
        healthChecker.start(
                properties.getHealthCheckIntervalSeconds(),
                properties.getHealthCheckTimeoutSeconds()
        );
        logger.info("Load balancer started on port {}", properties.getPort());
    }

    @PreDestroy
    public void stop() {
        running = false;
        healthChecker.stop();

        if (executorService != null) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        activeConnections.values().forEach(ConnectionHandler::close);
        activeConnections.clear();

        if (selector != null && selector.isOpen()) {
            try {
                selector.close();
            } catch (IOException e) {
                logger.error("Error closing selector", e);
            }
        }

        if (serverChannel != null && serverChannel.isOpen()) {
            try {
                serverChannel.close();
            } catch (IOException e) {
                logger.error("Error closing server channel", e);
            }
        }

        logger.info("Load balancer stopped");
    }

    private void initializeBackends() {
        for (LoadBalancerProperties.BackendConfig config : properties.getBackends()) {
            Backend backend = new Backend(config.getHost(), config.getPort());
            backendPool.addBackend(backend);
            logger.info("Added backend: {}", backend);
        }
    }

    private void startServer() throws IOException {
        serverChannel = ServerSocketChannel.open();
        serverChannel.bind(new InetSocketAddress(properties.getPort()));
        serverChannel.configureBlocking(false);

        selector = Selector.open();
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        executorService = Executors.newFixedThreadPool(10);
        running = true;

        executorService.submit(this::acceptConnections);
    }

    private void acceptConnections() {
        while (running) {
            try {
                int readyChannels = selector.select(1000);
                if (readyChannels == 0) {
                    continue;
                }

                Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
                while (keys.hasNext()) {
                    SelectionKey key = keys.next();
                    keys.remove();

                    if (!key.isValid()) {
                        continue;
                    }

                    if (key.isAcceptable()) {
                        handleAccept(key);
                    }
                }
            } catch (IOException e) {
                if (running) {
                    logger.error("Error in accept loop", e);
                }
            }
        }
    }

    private void handleAccept(SelectionKey key) {
        try {
            ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
            SocketChannel clientChannel = serverSocketChannel.accept();

            if (clientChannel != null) {
                clientChannel.configureBlocking(false);
                Backend backend = algorithm.selectBackend(backendPool.getAllBackends());

                if (backend == null) {
                    logger.warn("No healthy backend available, rejecting connection");
                    clientChannel.close();
                    return;
                }

                ConnectionHandler handler = new ConnectionHandler(clientChannel, backend, this::onConnectionClosed);
                activeConnections.put(clientChannel, handler);
                backend.incrementConnections();
                executorService.submit(handler);
                logger.debug("Accepted connection, routing to backend: {}", backend);
            }
        } catch (IOException e) {
            logger.error("Error accepting connection", e);
        }
    }

    private void onConnectionClosed(SocketChannel clientChannel) {
        ConnectionHandler handler = activeConnections.remove(clientChannel);
        if (handler != null && handler.getBackend() != null) {
            handler.getBackend().decrementConnections();
        }
    }

    public int getActiveConnectionsCount() {
        return activeConnections.size();
    }
}
