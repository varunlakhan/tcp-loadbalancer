package com.example.loadbalancer.health;

import com.example.loadbalancer.core.Backend;
import com.example.loadbalancer.core.BackendPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class HealthChecker {

    private static final Logger logger = LoggerFactory.getLogger(HealthChecker.class);
    private final BackendPool backendPool;
    private ScheduledExecutorService scheduler;
    private int healthCheckIntervalSeconds = 5;
    private int healthCheckTimeoutSeconds = 2;

    public HealthChecker(BackendPool backendPool) {
        this.backendPool = backendPool;
    }

    public void start(int intervalSeconds, int timeoutSeconds) {
        this.healthCheckIntervalSeconds = intervalSeconds;
        this.healthCheckTimeoutSeconds = timeoutSeconds;
        this.scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleWithFixedDelay(
                this::checkAllBackends,
                0,
                healthCheckIntervalSeconds,
                TimeUnit.SECONDS
        );
        logger.info("Health checker started with interval {}s and timeout {}s", 
                intervalSeconds, timeoutSeconds);
    }

    public void stop() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    private void checkAllBackends() {
        for (Backend backend : backendPool.getAllBackends()) {
            boolean isHealthy = checkBackendHealth(backend);
            boolean wasHealthy = backend.isHealthy();
            backend.setHealthy(isHealthy);

            if (wasHealthy != isHealthy) {
                if (isHealthy) {
                    logger.info("Backend {} is now healthy", backend);
                } else {
                    logger.warn("Backend {} is now unhealthy", backend);
                }
            }
        }
    }

    private boolean checkBackendHealth(Backend backend) {
        try (SocketChannel channel = SocketChannel.open()) {
            channel.configureBlocking(true);
            channel.socket().setSoTimeout(healthCheckTimeoutSeconds * 1000);
            boolean connected = channel.connect(backend.getAddress());
            return connected;
        } catch (IOException e) {
            return false;
        }
    }
}
