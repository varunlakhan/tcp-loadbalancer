# Layer 4 TCP Load Balancer

This is a simple software-based load balancer that works at Layer 4 (TCP level). I built it using Spring Boot and Java NIO to distribute incoming TCP connections across multiple backend servers. It automatically detects when backends go down and stops sending traffic to them.

## What You Need

- Java 17 or newer
- Maven 4.0 or newer

## Building It

Just run:

```bash
mvn clean package
```

## Running It

```bash
mvn spring-boot:run
```
Use *start-test-servers.sh* script to start test servers locally.

## Configuration

All the configuration is in `src/main/resources/application.yml`. Here's what you can tweak:

- **Port**: Which port should the load balancer listen on?
- **Algorithm**: Pick `round-robin` or `least-connections`
- **Backends**: List of your backend servers (host and port for each)
- **Health Check Settings**: How often to check backends and how long to wait before giving up

## Running Tests

To run all the tests:

```bash
mvn test
```

The test suite has:
- **Unit Tests**: Test individual pieces in isolation
- **Integration Tests**: Test how pieces work together
- **End-to-End Tests**: Test the whole thing with real TCP connections and mock backend servers

## Monitoring

You can check on the load balancer using Spring Boot Actuator endpoints:

- Health status: `http://localhost:8081/actuator/health`
- Metrics: `http://localhost:8081/actuator/metrics`
- General info: `http://localhost:8081/actuator/info`

