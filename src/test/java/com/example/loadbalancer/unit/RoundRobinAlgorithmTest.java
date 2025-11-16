package com.example.loadbalancer.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.loadbalancer.core.Backend;
import com.example.loadbalancer.core.RoundRobinAlgorithm;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RoundRobinAlgorithmTest {

    private RoundRobinAlgorithm algorithm;
    private List<Backend> backends;

    @BeforeEach
    void setUp() {
        algorithm = new RoundRobinAlgorithm();
        backends = new ArrayList<>();
        backends.add(new Backend("localhost", 9001));
        backends.add(new Backend("localhost", 9002));
        backends.add(new Backend("localhost", 9003));
    }

    @Test
    void testSelectBackend_RoundRobin() {
        Set<Backend> selected = new HashSet<>();
        for (int i = 0; i < 10; i++) {
            Backend selectedBackend = algorithm.selectBackend(backends);
            assertNotNull(selectedBackend);
            selected.add(selectedBackend);
        }
        assertEquals(3, selected.size());
    }

    @Test
    void testSelectBackend_OnlyHealthy() {
        backends.get(0).setHealthy(false);
        backends.get(2).setHealthy(false);

        for (int i = 0; i < 5; i++) {
            Backend selected = algorithm.selectBackend(backends);
            assertNotNull(selected);
            assertEquals(9002, selected.getPort());
            assertTrue(selected.isHealthy());
        }
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
    void testSelectBackend_NullList() {
        Backend selected = algorithm.selectBackend(null);
        assertNull(selected);
    }

    @Test
    void testGetName() {
        assertEquals("round-robin", algorithm.getName());
    }
}




