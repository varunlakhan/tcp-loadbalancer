package com.example.loadbalancer.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.loadbalancer.core.Backend;
import com.example.loadbalancer.core.LeastConnectionsAlgorithm;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LeastConnectionsAlgorithmTest {

    private LeastConnectionsAlgorithm algorithm;
    private List<Backend> backends;

    @BeforeEach
    void setUp() {
        algorithm = new LeastConnectionsAlgorithm();
        backends = new ArrayList<>();
        backends.add(new Backend("localhost", 9001));
        backends.add(new Backend("localhost", 9002));
        backends.add(new Backend("localhost", 9003));
    }

    @Test
    void testSelectBackend_LeastConnections() {
        backends.get(0).incrementConnections();
        backends.get(0).incrementConnections();
        backends.get(1).incrementConnections();

        Backend selected = algorithm.selectBackend(backends);
        assertNotNull(selected);
        assertEquals(9003, selected.getPort());
        assertEquals(0, selected.getActiveConnections());
    }

    @Test
    void testSelectBackend_EqualConnections() {
        Backend selected1 = algorithm.selectBackend(backends);
        assertNotNull(selected1);
        assertTrue(backends.contains(selected1));
    }

    @Test
    void testSelectBackend_OnlyHealthy() {
        backends.get(0).setHealthy(false);
        backends.get(2).incrementConnections();

        Backend selected = algorithm.selectBackend(backends);
        assertNotNull(selected);
        assertEquals(9002, selected.getPort());
        assertTrue(selected.isHealthy());
    }

    @Test
    void testSelectBackend_AllUnhealthy() {
        backends.forEach(b -> b.setHealthy(false));
        Backend selected = algorithm.selectBackend(backends);
        assertNull(selected);
    }

    @Test
    void testSelectBackend_EmptyList() {
        Backend selected = algorithm.selectBackend(new ArrayList<>());
        assertNull(selected);
    }

    @Test
    void testGetName() {
        assertEquals("least-connections", algorithm.getName());
    }
}




