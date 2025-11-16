package com.example.loadbalancer.core;

import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class LeastConnectionsAlgorithm implements LoadBalancingAlgorithm {

    @Override
    public Backend selectBackend(List<Backend> backends) {
        if (backends == null || backends.isEmpty()) {
            return null;
        }

        return backends.stream()
                .filter(Backend::isHealthy)
                .min(Comparator.comparingInt(Backend::getActiveConnections))
                .orElse(null);
    }

    @Override
    public String getName() {
        return "least-connections";
    }
}
