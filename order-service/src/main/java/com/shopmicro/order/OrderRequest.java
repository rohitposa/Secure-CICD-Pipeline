package com.shopmicro.order;

public record OrderRequest(long productId, int quantity) {
}
