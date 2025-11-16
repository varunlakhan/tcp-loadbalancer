package com.example.loadbalancer.core;

import java.util.List;

public interface LoadBalancingAlgorithm {
    Backend selectBackend(List<Backend> backends);
    String getName();
}
