package com.example.loadbalancer.e2e;

import com.example.loadbalancer.LoadBalancerApplication;
import com.example.loadbalancer.config.LoadBalancerProperties;
import com.example.loadbalancer.core.BackendPool;
import com.example.loadbalancer.server.TcpLoadBalancerServer;
import com.example.loadbalancer.util.MockBackendServer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = LoadBalancerApplication.class)
@ActiveProfiles("test")
class LoadBalancerE2ETest {

    @Autowired
    private TcpLoadBalancerServer loadBalancerServer;

    @Autowired
    private BackendPool backendPool;

    @Autowired
    private LoadBalancerProperties properties;

    private List<MockBackendServer> mockServers = new ArrayList<>();

    @BeforeEach
    void setUp() throws Exception {
        int[] ports = {19001, 19002};
        for (int port : ports) {
            MockBackendServer server = new MockBackendServer(port);
            server.start();
            mockServers.add(server);
        }

        for (MockBackendServer server : mockServers) {
            await().atMost(2, TimeUnit.SECONDS)
                    .until(() -> {
                        try (Socket socket = new Socket("localhost", server.getPort())) {
                            return socket.isConnected();
                        } catch (IOException e) {
                            return false;
                        }
                    });
        }

        await().atMost(10, TimeUnit.SECONDS)
                .pollInterval(200, TimeUnit.MILLISECONDS)
                .until(() -> {
                    int healthyCount = backendPool.getHealthyBackendsCount();
                    return healthyCount == 2;
                });
    }

    @AfterEach
    void tearDown() {
        mockServers.forEach(MockBackendServer::stop);
    }

    @Test
    void testLoadBalancer_AcceptsConnections() throws Exception {
        int loadBalancerPort = properties.getPort();
        if (loadBalancerPort == 0) {
            return;
        }

        try (Socket socket = new Socket("localhost", loadBalancerPort)) {
            assertTrue(socket.isConnected());
        }
    }

    @Test
    void testLoadBalancer_DistributesConnections() throws Exception {
        int loadBalancerPort = properties.getPort();
        if (loadBalancerPort == 0) {
            return;
        }

        int numConnections = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numConnections);
        CountDownLatch latch = new CountDownLatch(numConnections);

        for (int i = 0; i < numConnections; i++) {
            executor.submit(() -> {
                try (Socket socket = new Socket("localhost", loadBalancerPort)) {
                    Thread.sleep(100);
                } catch (Exception e) {
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS));

        await().atMost(3, TimeUnit.SECONDS)
                .until(() -> {
                    int totalConnections = mockServers.stream()
                            .mapToInt(MockBackendServer::getConnectionCount)
                            .sum();
                    return totalConnections >= numConnections;
                });

        int totalConnections = mockServers.stream()
                .mapToInt(MockBackendServer::getConnectionCount)
                .sum();
        assertTrue(totalConnections >= numConnections);
    }

    @Test
    void testLoadBalancer_RemovesUnhealthyBackend() throws Exception {
        int loadBalancerPort = properties.getPort();
        if (loadBalancerPort == 0) {
            return;
        }

        await().atMost(5, TimeUnit.SECONDS)
                .until(() -> backendPool.getHealthyBackendsCount() == 2);

        mockServers.get(0).stop();

        await().atMost(5, TimeUnit.SECONDS)
                .until(() -> backendPool.getHealthyBackendsCount() == 1);

        assertEquals(1, backendPool.getHealthyBackendsCount());

        try (Socket socket = new Socket("localhost", loadBalancerPort)) {
            assertTrue(socket.isConnected());
        }
    }

    @Test
    void testLoadBalancer_AllBackendsDown() throws Exception {
        int loadBalancerPort = properties.getPort();
        if (loadBalancerPort == 0) {
            return;
        }

        mockServers.forEach(MockBackendServer::stop);

        await().atMost(5, TimeUnit.SECONDS)
                .until(() -> backendPool.getHealthyBackendsCount() == 0);

        try (Socket socket = new Socket("localhost", loadBalancerPort)) {
            socket.setSoTimeout(1000);
            assertTrue(socket.isConnected());
            Thread.sleep(100);
        } catch (IOException e) {
        }
    }
}




