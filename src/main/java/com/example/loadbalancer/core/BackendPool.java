package com.example.loadbalancer.core;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class BackendPool {

    private final List<Backend> backends = new CopyOnWriteArrayList<>();

    public void addBackend(Backend backend) {
        if (!backends.contains(backend)) {
            backends.add(backend);
        }
    }

    public void removeBackend(Backend backend) {
        backends.remove(backend);
    }

    public List<Backend> getAllBackends() {
        return new ArrayList<>(backends);
    }

    public List<Backend> getHealthyBackends() {
        return backends.stream()
                .filter(Backend::isHealthy)
                .toList();
    }

    public int getTotalBackends() {
        return backends.size();
    }

    public int getHealthyBackendsCount() {
        return (int) backends.stream()
                .filter(Backend::isHealthy)
                .count();
    }

    public Backend findBackend(String host, int port) {
        return backends.stream()
                .filter(b -> b.getHost().equals(host) && b.getPort() == port)
                .findFirst()
                .orElse(null);
    }
}
