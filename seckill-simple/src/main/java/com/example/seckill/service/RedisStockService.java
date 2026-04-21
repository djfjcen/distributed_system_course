package com.example.seckill.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class RedisStockService {

    private static final String STOCK_KEY_PREFIX = "seckill:stock:";
    private static final String LIMIT_KEY_PREFIX = "seckill:limit:";
    private static final String TOKEN_KEY_PREFIX = "seckill:token:";
    private static final long TOKEN_EXPIRE_SECONDS = 3600;

    private static final DefaultRedisScript<Long> PRE_DEDUCT_SCRIPT = new DefaultRedisScript<>(
            "local stock = tonumber(redis.call('GET', KEYS[1]) or '-1') " +
                    "if stock < 0 then return -3 end " +
                    "local qty = tonumber(ARGV[1]) " +
                    "if stock < qty then return -1 end " +
                    "local current = tonumber(redis.call('GET', KEYS[2]) or '0') " +
                    "local limit = tonumber(ARGV[2]) " +
                    "if current + qty > limit then return -2 end " +
                    "redis.call('DECRBY', KEYS[1], qty) " +
                    "redis.call('INCRBY', KEYS[2], qty) " +
                    "redis.call('SETEX', KEYS[3], ARGV[3], qty) " +
                    "return 1", Long.class);

    private final StringRedisTemplate stringRedisTemplate;

    public RedisStockService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public void prepareStockCache(Long productId, int stock) {
        String key = stockKey(productId);
        stringRedisTemplate.opsForValue().setIfAbsent(key, String.valueOf(stock), 2, TimeUnit.HOURS);
    }

    public long preDeduct(Long userId, Long productId, Long orderId, int quantity, int limit) {
        Long result = stringRedisTemplate.execute(
                PRE_DEDUCT_SCRIPT,
                List.of(stockKey(productId), userLimitKey(userId, productId), tokenKey(orderId)),
                String.valueOf(quantity),
                String.valueOf(limit),
                String.valueOf(TOKEN_EXPIRE_SECONDS)
        );
        return result == null ? -9 : result;
    }

    public void rollbackPreDeduct(Long userId, Long productId, Long orderId, int quantity) {
        String token = stringRedisTemplate.opsForValue().get(tokenKey(orderId));
        if (token == null) {
            return;
        }
        stringRedisTemplate.opsForValue().increment(stockKey(productId), quantity);
        stringRedisTemplate.opsForValue().decrement(userLimitKey(userId, productId), quantity);
        stringRedisTemplate.delete(tokenKey(orderId));
    }

    private String stockKey(Long productId) {
        return STOCK_KEY_PREFIX + productId;
    }

    private String userLimitKey(Long userId, Long productId) {
        return LIMIT_KEY_PREFIX + userId + ":" + productId;
    }

    private String tokenKey(Long orderId) {
        return TOKEN_KEY_PREFIX + orderId;
    }
}
