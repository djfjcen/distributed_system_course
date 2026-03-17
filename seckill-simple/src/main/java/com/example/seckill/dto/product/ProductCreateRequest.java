package com.example.seckill.dto.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class ProductCreateRequest {

    @NotBlank(message = "name can not be blank")
    @Size(max = 100, message = "name length must be <= 100")
    private String name;

    @NotNull(message = "stock can not be null")
    @Min(value = 0, message = "stock must be >= 0")
    private Integer stock;

    @NotNull(message = "price can not be null")
    @DecimalMin(value = "0.01", message = "price must be >= 0.01")
    private BigDecimal price;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }
}
