package com.example.seckill.mq.producer;

import com.example.seckill.mq.message.SeckillOrderMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class SeckillOrderProducer {

    private static final Logger log = LoggerFactory.getLogger(SeckillOrderProducer.class);
    private static final String TOPIC = "seckill-orders";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public SeckillOrderProducer(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void sendOrderMessage(SeckillOrderMessage message) {
        try {
            String messageJson = objectMapper.writeValueAsString(message);
            String key = message.getUserId() + "_" + message.getProductId();
            kafkaTemplate.send(TOPIC, key, messageJson);
            log.info("Send seckill order message: orderId={}, userId={}, productId={}", 
                    message.getOrderId(), message.getUserId(), message.getProductId());
        } catch (Exception e) {
            log.error("Failed to send seckill order message", e);
            throw new RuntimeException("Failed to send order message", e);
        }
    }
}
