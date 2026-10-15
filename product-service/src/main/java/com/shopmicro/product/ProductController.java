package com.shopmicro.product;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class ProductController {

    private final List<Product> products = List.of(
            new Product(1, "Laptop", 55000),
            new Product(2, "Phone", 20000),
            new Product(3, "Headphones", 3000));

    @Value("${app.version:v1}")
    private String version;

    @Value("${app.fail:false}")
    private boolean fail;

    @GetMapping("/products")
    public List<Product> list() {
        failIfSwitchOn();
        return products;
    }

    @GetMapping("/products/{id}")
    public Product get(@PathVariable long id) {
        failIfSwitchOn();
        return products.stream()
                .filter(p -> p.id() == id)
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
    }

    @GetMapping("/version")
    public Map<String, String> version() {
        return Map.of("service", "product-service", "version", version);
    }

    private void failIfSwitchOn() {
        if (fail) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Simulated failure (app.fail=true)");
        }
    }
}
