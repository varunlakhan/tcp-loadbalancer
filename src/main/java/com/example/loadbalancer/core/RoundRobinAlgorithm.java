package com.example.loadbalancer.core;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RoundRobinAlgorithm implements LoadBalancingAlgorithm {

    private final AtomicInteger counter = new AtomicInteger(0);

    @Override
    public Backend selectBackend(List<Backend> backends) {
        if (backends == null || backends.isEmpty()) {
            return null;
        }

        List<Backend> healthyBackends = backends.stream()
                .filter(Backend::isHealthy)
                .toList();

        if (healthyBackends.isEmpty()) {
            return null;
        }

        int index = Math.abs(counter.getAndIncrement() % healthyBackends.size());
        return healthyBackends.get(index);
    }

    @Override
    public String getName() {
        return "round-robin";
    }
}
