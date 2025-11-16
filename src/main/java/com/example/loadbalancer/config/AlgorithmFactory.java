package com.example.loadbalancer.config;

import com.example.loadbalancer.core.LoadBalancingAlgorithm;
import com.example.loadbalancer.core.RoundRobinAlgorithm;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Configuration
public class AlgorithmFactory {

    @Bean
    @Primary
    public LoadBalancingAlgorithm loadBalancingAlgorithm(
            List<LoadBalancingAlgorithm> algorithms,
            LoadBalancerProperties properties) {
        Map<String, LoadBalancingAlgorithm> algorithmMap = algorithms.stream()
                .collect(Collectors.toMap(LoadBalancingAlgorithm::getName, Function.identity()));

        String algorithmName = properties.getAlgorithm();
        LoadBalancingAlgorithm algorithm = algorithmMap.get(algorithmName);

        if (algorithm == null) {
            return new RoundRobinAlgorithm();
        }

        return algorithm;
    }
}
