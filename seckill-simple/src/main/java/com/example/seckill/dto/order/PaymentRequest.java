package com.example.seckill.dto.order;

import jakarta.validation.constraints.NotNull;

public class PaymentRequest {

    @NotNull(message = "orderId can not be null")
    private Long orderId;

    private Boolean success = true;

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }
}
