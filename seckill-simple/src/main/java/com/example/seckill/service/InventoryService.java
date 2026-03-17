package com.example.seckill.service;

import com.example.seckill.datasource.ReadOnlyDataSource;
import com.example.seckill.entity.Product;
import com.example.seckill.repository.ProductRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Service
public class InventoryService {

    private static final String PRODUCT_CACHE_KEY_PREFIX = "product:detail:";
    private static final String PRODUCT_LOCK_KEY_PREFIX = "lock:product:detail:";
    private static final String NULL_CACHE_PLACEHOLDER = "__NULL__";
    private static final long PRODUCT_CACHE_BASE_SECONDS = 300;
    private static final long PRODUCT_CACHE_RANDOM_BOUND_SECONDS = 120;
    private static final long NULL_CACHE_SECONDS = 60;
    private static final long LOCK_EXPIRE_SECONDS = 10;
    private static final int MAX_RETRY_COUNT = 6;

    private final ProductRepository productRepository;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public InventoryService(ProductRepository productRepository,
                            StringRedisTemplate stringRedisTemplate,
                            ObjectMapper objectMapper) {
        this.productRepository = productRepository;
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    @ReadOnlyDataSource
    public List<Product> listProducts() {
        return productRepository.findAll();
    }

    @ReadOnlyDataSource
    public Product getProductDetail(Long productId) {
        String cacheKey = PRODUCT_CACHE_KEY_PREFIX + productId;
        String lockKey = PRODUCT_LOCK_KEY_PREFIX + productId;

        for (int i = 0; i < MAX_RETRY_COUNT; i++) {
            String cachedValue = stringRedisTemplate.opsForValue().get(cacheKey);
            if (cachedValue != null) {
                if (NULL_CACHE_PLACEHOLDER.equals(cachedValue)) {
                    throw new EntityNotFoundException("product not found: " + productId);
                }
                return deserializeProduct(cachedValue, productId);
            }

            Boolean lockSuccess = stringRedisTemplate.opsForValue()
                    .setIfAbsent(lockKey, "1", LOCK_EXPIRE_SECONDS, TimeUnit.SECONDS);

            if (Boolean.TRUE.equals(lockSuccess)) {
                try {
                    String secondReadValue = stringRedisTemplate.opsForValue().get(cacheKey);
                    if (secondReadValue != null) {
                        if (NULL_CACHE_PLACEHOLDER.equals(secondReadValue)) {
                            throw new EntityNotFoundException("product not found: " + productId);
                        }
                        return deserializeProduct(secondReadValue, productId);
                    }

                    Product product = productRepository.findById(productId).orElse(null);
                    if (product == null) {
                        stringRedisTemplate.opsForValue().set(cacheKey, NULL_CACHE_PLACEHOLDER, NULL_CACHE_SECONDS, TimeUnit.SECONDS);
                        throw new EntityNotFoundException("product not found: " + productId);
                    }

                    long ttl = PRODUCT_CACHE_BASE_SECONDS + ThreadLocalRandom.current().nextLong(PRODUCT_CACHE_RANDOM_BOUND_SECONDS + 1);
                    stringRedisTemplate.opsForValue().set(cacheKey, serializeProduct(product), ttl, TimeUnit.SECONDS);
                    return product;
                } finally {
                    stringRedisTemplate.delete(lockKey);
                }
            }

            sleepBeforeRetry();
        }

        Product fallbackProduct = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("product not found: " + productId));
        long ttl = PRODUCT_CACHE_BASE_SECONDS + ThreadLocalRandom.current().nextLong(PRODUCT_CACHE_RANDOM_BOUND_SECONDS + 1);
        stringRedisTemplate.opsForValue().set(cacheKey, serializeProduct(fallbackProduct), ttl, TimeUnit.SECONDS);
        return fallbackProduct;
    }

    @Transactional
    public Product seckill(Long productId, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be greater than 0");
        }

        Product product = productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new EntityNotFoundException("product not found: " + productId));

        if (product.getStock() < quantity) {
            throw new IllegalStateException("stock not enough");
        }

        product.setStock(product.getStock() - quantity);
        Product savedProduct = productRepository.save(product);
        stringRedisTemplate.delete(PRODUCT_CACHE_KEY_PREFIX + productId);
        return savedProduct;
    }

    private Product deserializeProduct(String json, Long productId) {
        try {
            return objectMapper.readValue(json, Product.class);
        } catch (JsonProcessingException e) {
            stringRedisTemplate.delete(PRODUCT_CACHE_KEY_PREFIX + productId);
            throw new IllegalStateException("invalid cached product json", e);
        }
    }

    private String serializeProduct(Product product) {
        try {
            return objectMapper.writeValueAsString(product);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("serialize product failed", e);
        }
    }

    private void sleepBeforeRetry() {
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
