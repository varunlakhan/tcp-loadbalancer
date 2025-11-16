package com.example.loadbalancer.core;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class Backend {
    private final String host;
    private final int port;
    private volatile boolean healthy = true;
    private final AtomicInteger activeConnections = new AtomicInteger(0);
    private final AtomicLong lastHealthCheck = new AtomicLong(System.currentTimeMillis());

    public Backend(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public InetSocketAddress getAddress() {
        return new InetSocketAddress(host, port);
    }

    public boolean isHealthy() {
        return healthy;
    }

    public void setHealthy(boolean healthy) {
        this.healthy = healthy;
        this.lastHealthCheck.set(System.currentTimeMillis());
    }

    public int getActiveConnections() {
        return activeConnections.get();
    }

    public void incrementConnections() {
        activeConnections.incrementAndGet();
    }

    public void decrementConnections() {
        activeConnections.decrementAndGet();
    }

    public long getLastHealthCheck() {
        return lastHealthCheck.get();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Backend backend = (Backend) o;
        return port == backend.port && host.equals(backend.host);
    }

    @Override
    public int hashCode() {
        return host.hashCode() * 31 + port;
    }

    @Override
    public String toString() {
        return host + ":" + port;
    }
}
