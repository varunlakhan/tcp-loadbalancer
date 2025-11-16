package com.example.loadbalancer.actuator;

import com.example.loadbalancer.core.BackendPool;
import com.example.loadbalancer.server.TcpLoadBalancerServer;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class LoadBalancerHealthIndicator implements HealthIndicator {

    private final BackendPool backendPool;
    private final TcpLoadBalancerServer server;

    public LoadBalancerHealthIndicator(BackendPool backendPool, TcpLoadBalancerServer server) {
        this.backendPool = backendPool;
        this.server = server;
    }

    @Override
    public Health health() {
        int totalBackends = backendPool.getTotalBackends();
        int healthyBackends = backendPool.getHealthyBackendsCount();
        int activeConnections = server.getActiveConnectionsCount();

        Health.Builder builder = totalBackends > 0 && healthyBackends > 0
                ? Health.up()
                : Health.down();

        return builder
                .withDetail("totalBackends", totalBackends)
                .withDetail("healthyBackends", healthyBackends)
                .withDetail("activeConnections", activeConnections)
                .build();
    }
}
