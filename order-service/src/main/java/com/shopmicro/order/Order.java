package com.shopmicro.order;

public record Order(long id, long productId, String productName, int quantity, double total) {
}
