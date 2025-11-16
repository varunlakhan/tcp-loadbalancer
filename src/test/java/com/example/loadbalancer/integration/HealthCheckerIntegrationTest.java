package com.example.loadbalancer.integration;

import com.example.loadbalancer.core.Backend;
import com.example.loadbalancer.core.BackendPool;
import com.example.loadbalancer.health.HealthChecker;
import com.example.loadbalancer.util.MockBackendServer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

class HealthCheckerIntegrationTest {

    private BackendPool pool;
    private HealthChecker healthChecker;
    private MockBackendServer mockServer;

    @BeforeEach
    void setUp() {
        pool = new BackendPool();
        healthChecker = new HealthChecker(pool);
    }

    @AfterEach
    void tearDown() {
        if (healthChecker != null) {
            healthChecker.stop();
        }
        if (mockServer != null) {
            mockServer.stop();
        }
    }

    @Test
    void testHealthCheck_HealthyBackend() throws Exception {
        mockServer = new MockBackendServer(19001);
        mockServer.start();

        Backend backend = new Backend("localhost", 19001);
        backend.setHealthy(false);
        pool.addBackend(backend);

        healthChecker.start(1, 1);

        await().atMost(5, java.util.concurrent.TimeUnit.SECONDS)
                .until(() -> backend.isHealthy());

        assertTrue(backend.isHealthy());
    }

    @Test
    void testHealthCheck_UnhealthyBackend() throws Exception {
        Backend backend = new Backend("localhost", 19999);
        backend.setHealthy(true);
        pool.addBackend(backend);

        healthChecker.start(1, 1);

        await().atMost(5, java.util.concurrent.TimeUnit.SECONDS)
                .until(() -> !backend.isHealthy());

        assertFalse(backend.isHealthy());
    }

    @Test
    void testHealthCheck_BackendGoesDown() throws Exception {
        mockServer = new MockBackendServer(19002);
        mockServer.start();

        Backend backend = new Backend("localhost", 19002);
        pool.addBackend(backend);

        healthChecker.start(1, 1);

        await().atMost(5, java.util.concurrent.TimeUnit.SECONDS)
                .until(() -> backend.isHealthy());

        assertTrue(backend.isHealthy());

        mockServer.stop();

        await().atMost(5, java.util.concurrent.TimeUnit.SECONDS)
                .until(() -> !backend.isHealthy());

        assertFalse(backend.isHealthy());
    }
}




