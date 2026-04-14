package com.example.seckill.mq.consumer;

import com.example.seckill.constant.OrderStatus;
import com.example.seckill.entity.Order;
import com.example.seckill.mq.message.InventoryResultMessage;
import com.example.seckill.mq.producer.SeckillOrderProducer;
import com.example.seckill.repository.OrderRepository;
import com.example.seckill.service.OrderService;
import com.example.seckill.service.ProcessedMessageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class InventoryResultConsumer {

    private static final String CONSUMER_NAME = "order-inventory-result-consumer";

    private final ObjectMapper objectMapper;
    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final ProcessedMessageService processedMessageService;

    public InventoryResultConsumer(ObjectMapper objectMapper,
                                   OrderRepository orderRepository,
                                   OrderService orderService,
                                   ProcessedMessageService processedMessageService) {
        this.objectMapper = objectMapper;
        this.orderRepository = orderRepository;
        this.orderService = orderService;
        this.processedMessageService = processedMessageService;
    }

    @KafkaListener(topics = SeckillOrderProducer.TOPIC_INVENTORY_RESULT, groupId = "seckill-group")
    @Transactional
    public void consume(String payload) throws Exception {
        InventoryResultMessage message = objectMapper.readValue(payload, InventoryResultMessage.class);
        if (processedMessageService.alreadyProcessed(CONSUMER_NAME, message.getMessageId())) {
            return;
        }

        Order order = orderRepository.findByIdForUpdate(message.getOrderId()).orElse(null);
        if (order == null) {
            processedMessageService.markProcessed(CONSUMER_NAME, message.getMessageId());
            return;
        }

        if (message.isSuccess()) {
            if (order.getStatus() == OrderStatus.INIT) {
                orderRepository.moveStatus(order.getId(), order.getUserId(), OrderStatus.INIT, OrderStatus.UNPAID);
            }
        } else {
            if (order.getStatus() == OrderStatus.INIT) {
                orderService.markCancelledAndRollbackRedis(order, message.getReason());
            }
        }

        processedMessageService.markProcessed(CONSUMER_NAME, message.getMessageId());
    }
}
