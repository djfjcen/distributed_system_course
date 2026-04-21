package com.example.seckill.dto.order;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class SeckillOrderRequest {

    @NotNull(message = "productId can not be null")
    private Long productId;

    @NotNull(message = "quantity can not be null")
    @Min(value = 1, message = "quantity must be >= 1")
    private Integer quantity;

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
