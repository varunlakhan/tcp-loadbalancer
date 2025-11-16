package com.example.loadbalancer.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.loadbalancer.core.Backend;
import com.example.loadbalancer.core.BackendPool;

import static org.junit.jupiter.api.Assertions.*;

class BackendPoolTest {

    private BackendPool pool;

    @BeforeEach
    void setUp() {
        pool = new BackendPool();
    }

    @Test
    void testAddBackend() {
        Backend backend = new Backend("localhost", 9001);
        pool.addBackend(backend);

        assertEquals(1, pool.getTotalBackends());
        assertEquals(1, pool.getHealthyBackendsCount());
        assertTrue(pool.getAllBackends().contains(backend));
    }

    @Test
    void testAddDuplicateBackend() {
        Backend backend1 = new Backend("localhost", 9001);
        Backend backend2 = new Backend("localhost", 9001);

        pool.addBackend(backend1);
        pool.addBackend(backend2);

        assertEquals(1, pool.getTotalBackends());
    }

    @Test
    void testRemoveBackend() {
        Backend backend = new Backend("localhost", 9001);
        pool.addBackend(backend);
        pool.removeBackend(backend);

        assertEquals(0, pool.getTotalBackends());
        assertFalse(pool.getAllBackends().contains(backend));
    }

    @Test
    void testGetHealthyBackends() {
        Backend healthy1 = new Backend("localhost", 9001);
        Backend healthy2 = new Backend("localhost", 9002);
        Backend unhealthy = new Backend("localhost", 9003);
        unhealthy.setHealthy(false);

        pool.addBackend(healthy1);
        pool.addBackend(healthy2);
        pool.addBackend(unhealthy);

        assertEquals(3, pool.getTotalBackends());
        assertEquals(2, pool.getHealthyBackendsCount());
        assertEquals(2, pool.getHealthyBackends().size());
    }

    @Test
    void testFindBackend() {
        Backend backend = new Backend("localhost", 9001);
        pool.addBackend(backend);

        Backend found = pool.findBackend("localhost", 9001);
        assertNotNull(found);
        assertEquals(backend, found);

        Backend notFound = pool.findBackend("localhost", 9999);
        assertNull(notFound);
    }
}




