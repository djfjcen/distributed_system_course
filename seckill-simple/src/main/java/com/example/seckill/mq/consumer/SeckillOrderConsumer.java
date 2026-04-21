package com.example.seckill.mq.consumer;

import com.example.seckill.constant.ReservationStatus;
import com.example.seckill.entity.Product;
import com.example.seckill.entity.StockReservation;
import com.example.seckill.mq.message.InventoryResultMessage;
import com.example.seckill.mq.message.OrderCreatedMessage;
import com.example.seckill.mq.producer.SeckillOrderProducer;
import com.example.seckill.repository.ProductRepository;
import com.example.seckill.repository.StockReservationRepository;
import com.example.seckill.service.ProcessedMessageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
public class SeckillOrderConsumer {

    private static final String CONSUMER_NAME = "inventory-order-created-consumer";

    private final ProductRepository productRepository;
    private final StockReservationRepository stockReservationRepository;
    private final ProcessedMessageService processedMessageService;
    private final SeckillOrderProducer seckillOrderProducer;
    private final ObjectMapper objectMapper;

    public SeckillOrderConsumer(ProductRepository productRepository,
                                StockReservationRepository stockReservationRepository,
                                ProcessedMessageService processedMessageService,
                                SeckillOrderProducer seckillOrderProducer,
                                ObjectMapper objectMapper) {
        this.productRepository = productRepository;
        this.stockReservationRepository = stockReservationRepository;
        this.processedMessageService = processedMessageService;
        this.seckillOrderProducer = seckillOrderProducer;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = SeckillOrderProducer.TOPIC_ORDER_CREATED, groupId = "seckill-group")
    @Transactional
    public void consume(String payload) throws Exception {
        OrderCreatedMessage message = objectMapper.readValue(payload, OrderCreatedMessage.class);
        if (processedMessageService.alreadyProcessed(CONSUMER_NAME, message.getMessageId())) {
            return;
        }

        InventoryResultMessage result = new InventoryResultMessage();
        result.setMessageId(UUID.randomUUID().toString());
        result.setOrderId(message.getOrderId());
        result.setUserId(message.getUserId());
        result.setProductId(message.getProductId());
        result.setQuantity(message.getQuantity());

        Product product = productRepository.findByIdForUpdate(message.getProductId()).orElse(null);
        if (product == null || product.getStock() < message.getQuantity()) {
            result.setSuccess(false);
            result.setReason("stock not enough in db");
            seckillOrderProducer.sendInventoryResult(result);
            processedMessageService.markProcessed(CONSUMER_NAME, message.getMessageId());
            return;
        }

        product.setStock(product.getStock() - message.getQuantity());
        productRepository.save(product);

        StockReservation reservation = new StockReservation();
        reservation.setOrderId(message.getOrderId());
        reservation.setUserId(message.getUserId());
        reservation.setProductId(message.getProductId());
        reservation.setQuantity(message.getQuantity());
        reservation.setStatus(ReservationStatus.RESERVED);
        stockReservationRepository.save(reservation);

        result.setSuccess(true);
        result.setReason("reserved");
        seckillOrderProducer.sendInventoryResult(result);

        processedMessageService.markProcessed(CONSUMER_NAME, message.getMessageId());
    }
}
