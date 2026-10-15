package com.shopmicro.order;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class OrderController {

    private final ProductClient productClient;
    private final Map<Long, Order> orders = new ConcurrentHashMap<>();
    private final AtomicLong idSequence = new AtomicLong(1);

    @Value("${app.version:v1}")
    private String version;

    @Value("${app.fail:false}")
    private boolean fail;

    public OrderController(ProductClient productClient) {
        this.productClient = productClient;
    }

    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public Order create(@RequestBody OrderRequest request) {
        failIfSwitchOn();
        if (request.quantity() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "quantity must be greater than 0");
        }

        ProductDto product;
        try {
            product = productClient.findById(request.productId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "product-service is not reachable");
        }

        long id = idSequence.getAndIncrement();
        Order order = new Order(id, product.id(), product.name(), request.quantity(),
                product.price() * request.quantity());
        orders.put(id, order);
        return order;
    }

    @GetMapping("/orders")
    public List<Order> list() {
        failIfSwitchOn();
        return new ArrayList<>(orders.values());
    }

    @GetMapping("/orders/{id}")
    public Order get(@PathVariable long id) {
        failIfSwitchOn();
        Order order = orders.get(id);
        if (order == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found");
        }
        return order;
    }

    @GetMapping("/version")
    public Map<String, String> version() {
        return Map.of("service", "order-service", "version", version);
    }

    private void failIfSwitchOn() {
        if (fail) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Simulated failure (app.fail=true)");
        }
    }
}
