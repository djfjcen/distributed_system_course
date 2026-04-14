package com.example.seckill.constant;

public final class OrderStatus {

    private OrderStatus() {
    }

    public static final int INIT = 0;
    public static final int UNPAID = 1;
    public static final int PAYING = 2;
    public static final int PAID = 3;
    public static final int CANCELLED = 4;
}
