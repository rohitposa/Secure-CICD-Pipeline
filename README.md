# shop-microservices

A small e-commerce backend made of two Spring Boot microservices.

| Service | Port | What it does |
|---|---|---|
| `product-service` | 8081 | Product catalogue (in-memory data) |
| `order-service` | 8082 | Creates orders; calls `product-service` to validate the product and price |

- Java 17, Spring Boot 3.3.5, Maven multi-module build
- No database (data is in memory and resets on restart)
- Unit/integration tests with JUnit 5 and MockMvc; JaCoCo coverage is enabled

## Prerequisites

- JDK 17 or newer
- Maven 3.8+

## Build and test

```
mvn clean verify
```

This runs the tests of both modules and produces:

- `product-service/target/product-service-1.0.0-SNAPSHOT.jar`
- `order-service/target/order-service-1.0.0-SNAPSHOT.jar`

JaCoCo reports: `<module>/target/site/jacoco/index.html` and `<module>/target/jacoco.exec`.

## Run locally

Start product-service first, then order-service (separate terminals):

```
java -jar product-service/target/product-service-1.0.0-SNAPSHOT.jar
java -jar order-service/target/order-service-1.0.0-SNAPSHOT.jar
```

## API

**product-service (8081)**

| Method | Path | Description |
|---|---|---|
| GET | `/api/products` | List products |
| GET | `/api/products/{id}` | Get one product (404 if missing) |
| GET | `/api/version` | Service name and version |

**order-service (8082)**

| Method | Path | Description |
|---|---|---|
| POST | `/api/orders` | Create order. Body: `{"productId":1,"quantity":2}` |
| GET | `/api/orders` | List orders |
| GET | `/api/orders/{id}` | Get one order (404 if missing) |
| GET | `/api/version` | Service name and version |

Example:

```
curl localhost:8081/api/products
curl -X POST localhost:8082/api/orders -H "Content-Type: application/json" \
     -d '{"productId":1,"quantity":2}'
```

Error behaviour of `POST /api/orders`: 404 unknown product, 400 quantity <= 0,
503 if product-service is not reachable.

## Configuration (environment variables)

| Variable | Service | Default | Meaning |
|---|---|---|---|
| `SERVER_PORT` | both | 8081 / 8082 | HTTP port |
| `PRODUCT_SERVICE_URL` | order | `http://localhost:8081` | Where product-service is reachable |
| `APP_VERSION` | both | `v1` | Value returned by `/api/version` |
| `APP_FAIL` | both | `false` | When `true`, the service simulates a broken release (see below) |

## Health and metrics

Spring Boot Actuator is enabled on each service:

- `/actuator/health` overall health
- `/actuator/health/liveness` liveness probe
- `/actuator/health/readiness` readiness probe
- `/actuator/prometheus` Prometheus metrics

## Simulating a broken release

Start a service with `APP_FAIL=true`. Then:

- `/actuator/health` and `/actuator/health/readiness` return `503` (DOWN)
- `/actuator/health/liveness` stays UP (the process itself is alive)
- business endpoints under `/api/...` return `500`

This is meant for testing deployment verification and rollback.

## Not included

Dockerfile, CI pipeline and deployment manifests are owned by the DevOps/platform side and are not part of this repo.
