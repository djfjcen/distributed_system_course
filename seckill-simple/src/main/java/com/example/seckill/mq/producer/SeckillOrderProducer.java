package com.example.seckill.mq.producer;

import com.example.seckill.mq.message.InventoryResultMessage;
import com.example.seckill.mq.message.OrderCreatedMessage;
import com.example.seckill.mq.message.PaymentResultMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class SeckillOrderProducer {

    public static final String TOPIC_ORDER_CREATED = "order-created";
    public static final String TOPIC_INVENTORY_RESULT = "inventory-result";
    public static final String TOPIC_PAYMENT_RESULT = "payment-result";

    private static final Logger log = LoggerFactory.getLogger(SeckillOrderProducer.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public SeckillOrderProducer(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void sendOrderCreated(OrderCreatedMessage message) {
        send(TOPIC_ORDER_CREATED, message.getOrderId().toString(), message);
    }

    public void sendInventoryResult(InventoryResultMessage message) {
        send(TOPIC_INVENTORY_RESULT, message.getOrderId().toString(), message);
    }

    public void sendPaymentResult(PaymentResultMessage message) {
        send(TOPIC_PAYMENT_RESULT, message.getOrderId().toString(), message);
    }

    private void send(String topic, String key, Object message) {
        try {
            String payload = objectMapper.writeValueAsString(message);
            kafkaTemplate.send(topic, key, payload);
            log.info("send topic={}, key={}, payload={}", topic, key, payload);
        } catch (Exception e) {
            throw new RuntimeException("failed to send kafka message", e);
        }
    }
}
