package com.example.seckill.mq.consumer;

import com.example.seckill.entity.Order;
import com.example.seckill.mq.message.SeckillOrderMessage;
import com.example.seckill.repository.OrderRepository;
import com.example.seckill.repository.ProductRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SeckillOrderConsumer {

    private static final Logger log = LoggerFactory.getLogger(SeckillOrderConsumer.class);

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final ObjectMapper objectMapper;

    public SeckillOrderConsumer(OrderRepository orderRepository,
                               ProductRepository productRepository,
                               ObjectMapper objectMapper) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "seckill-orders", groupId = "seckill-group")
    @Transactional
    public void consume(String message) {
        try {
            SeckillOrderMessage orderMessage = objectMapper.readValue(message, SeckillOrderMessage.class);
            log.info("Consume seckill order message: {}", orderMessage);

            // Create order record
            Order order = new Order();
            order.setId(orderMessage.getOrderId());
            order.setUserId(orderMessage.getUserId());
            order.setProductId(orderMessage.getProductId());
            order.setQuantity(orderMessage.getQuantity());
            order.setPrice(orderMessage.getPrice());
            order.setStatus(1); // success

            orderRepository.save(order);
            log.info("Order created successfully: orderId={}", orderMessage.getOrderId());

        } catch (Exception e) {
            log.error("Failed to process seckill order message: {}", message, e);
        }
    }
}
