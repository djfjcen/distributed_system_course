package com.example.seckill.service;

import com.example.seckill.datasource.ReadOnlyDataSource;
import com.example.seckill.entity.Order;
import com.example.seckill.entity.Product;
import com.example.seckill.mq.message.SeckillOrderMessage;
import com.example.seckill.mq.producer.SeckillOrderProducer;
import com.example.seckill.repository.OrderRepository;
import com.example.seckill.repository.ProductRepository;
import com.example.seckill.util.SnowflakeIdGenerator;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private static final String ORDER_IDEMPOTENT_KEY_PREFIX = "order:idempotent:";
    private static final long IDEMPOTENT_EXPIRE_SECONDS = 24 * 3600; // 24 hours
    private static final String INVENTORY_CACHE_KEY_PREFIX = "inventory:";

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final SeckillOrderProducer seckillOrderProducer;
    private final StringRedisTemplate stringRedisTemplate;
    private final SnowflakeIdGenerator snowflakeIdGenerator;

    public OrderService(OrderRepository orderRepository,
                        ProductRepository productRepository,
                        SeckillOrderProducer seckillOrderProducer,
                        StringRedisTemplate stringRedisTemplate) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.seckillOrderProducer = seckillOrderProducer;
        this.stringRedisTemplate = stringRedisTemplate;
        // workerId: 1, datacenterId: 1 (can be configurable)
        this.snowflakeIdGenerator = new SnowflakeIdGenerator(1, 1);
    }

    @Transactional
    public Order seckillOrder(Long userId, Long productId, Integer quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be greater than 0");
        }

        // 1. Check idempotency: same user + same product = only one order allowed
        String idempotentKey = ORDER_IDEMPOTENT_KEY_PREFIX + userId + ":" + productId;
        Boolean isFirstRequest = stringRedisTemplate.opsForValue()
                .setIfAbsent(idempotentKey, "1", IDEMPOTENT_EXPIRE_SECONDS, TimeUnit.SECONDS);

        if (Boolean.FALSE.equals(isFirstRequest)) {
            // Already has order for this user + product combination
            Order existingOrder = orderRepository.findByUserIdAndProductId(userId, productId)
                    .orElseThrow(() -> new IllegalStateException("Duplicate order request detected"));
            log.warn("Duplicate seckill order detected: userId={}, productId={}", userId, productId);
            return existingOrder;
        }

        // 2. Check product existence
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("product not found: " + productId));

        // 3. Check inventory cache (real-time from Redis + DB)
        String inventoryCacheKey = INVENTORY_CACHE_KEY_PREFIX + productId;
        String cachedInventory = stringRedisTemplate.opsForValue().get(inventoryCacheKey);
        
        int currentInventory;
        if (cachedInventory != null) {
            currentInventory = Integer.parseInt(cachedInventory);
        } else {
            Product currentProduct = productRepository.findById(productId)
                    .orElseThrow(() -> new EntityNotFoundException("product not found: " + productId));
            currentInventory = currentProduct.getStock();
            stringRedisTemplate.opsForValue().set(inventoryCacheKey, String.valueOf(currentInventory), 300, TimeUnit.SECONDS);
        }

        if (currentInventory < quantity) {
            // Clear idempotent key to allow retry if inventory becomes available
            stringRedisTemplate.delete(idempotentKey);
            throw new IllegalStateException("stock not enough: available=" + currentInventory + ", requested=" + quantity);
        }

        // 4. Deduct from Redis cache first (for fast response)
        Long deducted = stringRedisTemplate.opsForValue().decrement(inventoryCacheKey, quantity);
        if (deducted < 0) {
            // Revert the decrement
            stringRedisTemplate.opsForValue().increment(inventoryCacheKey, quantity);
            stringRedisTemplate.delete(idempotentKey);
            throw new IllegalStateException("stock not enough");
        }

        // 5. Create order with snowflake ID
        Long orderId = snowflakeIdGenerator.nextId();
        Order order = new Order();
        order.setId(orderId);
        order.setUserId(userId);
        order.setProductId(productId);
        order.setQuantity(quantity);
        order.setPrice(product.getPrice());
        order.setStatus(0); // pending status initially

        // 6. Save to local DB first (ensures record exists even if Kafka fails)
        Order savedOrder = orderRepository.save(order);
        log.info("Order saved locally: orderId={}, userId={}, productId={}, quantity={}", 
                orderId, userId, productId, quantity);

        // 7. Send to Kafka for async processing
        SeckillOrderMessage message = new SeckillOrderMessage(orderId, userId, productId, quantity, product.getPrice());
        try {
            seckillOrderProducer.sendOrderMessage(message);
        } catch (Exception e) {
            // Even if Kafka fails, order is already saved locally
            log.error("Failed to send order to Kafka, but order is saved: orderId={}", orderId, e);
        }

        // 8. Update actual DB inventory (deduct from master DB)
        product.setStock(product.getStock() - quantity);
        productRepository.save(product);

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
}
