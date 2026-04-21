package com.example.seckill.service;

import com.example.seckill.constant.OrderStatus;
import com.example.seckill.datasource.ReadOnlyDataSource;
import com.example.seckill.entity.Order;
import com.example.seckill.entity.Product;
import com.example.seckill.mq.message.OrderCreatedMessage;
import com.example.seckill.mq.producer.SeckillOrderProducer;
import com.example.seckill.repository.OrderRepository;
import com.example.seckill.repository.ProductRepository;
import com.example.seckill.util.SnowflakeIdGenerator;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class OrderService {

    private static final int PER_USER_LIMIT = 1;

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final RedisStockService redisStockService;
    private final SeckillOrderProducer seckillOrderProducer;
    private final SnowflakeIdGenerator snowflakeIdGenerator = new SnowflakeIdGenerator(1, 1);

    public OrderService(OrderRepository orderRepository,
                        ProductRepository productRepository,
                        RedisStockService redisStockService,
                        SeckillOrderProducer seckillOrderProducer) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.redisStockService = redisStockService;
        this.seckillOrderProducer = seckillOrderProducer;
    }

    @Transactional
    public Order seckillOrder(Long userId, Long productId, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("quantity must be greater than 0");
        }

        Optional<Order> existing = orderRepository.findByUserIdAndProductId(userId, productId);
        if (existing.isPresent()) {
            return existing.get();
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("product not found: " + productId));

        Long orderId = snowflakeIdGenerator.nextId();

        redisStockService.prepareStockCache(productId, product.getStock());
        long preDeductResult = redisStockService.preDeduct(userId, productId, orderId, quantity, PER_USER_LIMIT);
        if (preDeductResult == -1) {
            throw new IllegalStateException("stock not enough");
        }
        if (preDeductResult == -2) {
            throw new IllegalStateException("purchase limit exceeded");
        }
        if (preDeductResult == -3) {
            throw new IllegalStateException("stock cache not ready");
        }
        if (preDeductResult != 1) {
            throw new IllegalStateException("unknown redis pre-deduct error");
        }

        Order order = new Order();
        order.setId(orderId);
        order.setUserId(userId);
        order.setProductId(productId);
        order.setQuantity(quantity);
        order.setPrice(product.getPrice());
        order.setStatus(OrderStatus.INIT);
        Order savedOrder = orderRepository.save(order);

        OrderCreatedMessage event = new OrderCreatedMessage();
        event.setMessageId(UUID.randomUUID().toString());
        event.setOrderId(savedOrder.getId());
        event.setUserId(savedOrder.getUserId());
        event.setProductId(savedOrder.getProductId());
        event.setQuantity(savedOrder.getQuantity());
        event.setPrice(savedOrder.getPrice());
        seckillOrderProducer.sendOrderCreated(event);

        return savedOrder;
    }

    @ReadOnlyDataSource
    public Order getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("order not found: " + orderId));
    }

    @ReadOnlyDataSource
    public List<Order> getOrdersByUserId(Long userId) {
        return orderRepository.findByUserId(userId);
    }

    @ReadOnlyDataSource
    public List<Order> getOrdersByProductId(Long productId) {
        return orderRepository.findByProductId(productId);
    }

    @Transactional
    public void markCancelledAndRollbackRedis(Order order, String reason) {
        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.PAID) {
            return;
        }
        int moved = orderRepository.moveStatus(order.getId(), order.getUserId(), order.getStatus(), OrderStatus.CANCELLED);
        if (moved > 0) {
            redisStockService.rollbackPreDeduct(order.getUserId(), order.getProductId(), order.getId(), order.getQuantity());
        }
    }
}
