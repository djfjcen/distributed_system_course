package com.example.seckill.mq.consumer;

import com.example.seckill.constant.OrderStatus;
import com.example.seckill.constant.ReservationStatus;
import com.example.seckill.entity.Order;
import com.example.seckill.entity.PaymentTxn;
import com.example.seckill.entity.Product;
import com.example.seckill.entity.StockReservation;
import com.example.seckill.mq.message.PaymentResultMessage;
import com.example.seckill.mq.producer.SeckillOrderProducer;
import com.example.seckill.repository.OrderRepository;
import com.example.seckill.repository.PaymentTxnRepository;
import com.example.seckill.repository.ProductRepository;
import com.example.seckill.repository.StockReservationRepository;
import com.example.seckill.service.OrderService;
import com.example.seckill.service.ProcessedMessageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PaymentResultConsumer {

    private static final String CONSUMER_NAME = "order-payment-result-consumer";

    private final ObjectMapper objectMapper;
    private final OrderRepository orderRepository;
    private final PaymentTxnRepository paymentTxnRepository;
    private final StockReservationRepository stockReservationRepository;
    private final ProductRepository productRepository;
    private final OrderService orderService;
    private final ProcessedMessageService processedMessageService;

    public PaymentResultConsumer(ObjectMapper objectMapper,
                                 OrderRepository orderRepository,
                                 PaymentTxnRepository paymentTxnRepository,
                                 StockReservationRepository stockReservationRepository,
                                 ProductRepository productRepository,
                                 OrderService orderService,
                                 ProcessedMessageService processedMessageService) {
        this.objectMapper = objectMapper;
        this.orderRepository = orderRepository;
        this.paymentTxnRepository = paymentTxnRepository;
        this.stockReservationRepository = stockReservationRepository;
        this.productRepository = productRepository;
        this.orderService = orderService;
        this.processedMessageService = processedMessageService;
    }

    @KafkaListener(topics = SeckillOrderProducer.TOPIC_PAYMENT_RESULT, groupId = "seckill-group")
    @Transactional
    public void consume(String payload) throws Exception {
        PaymentResultMessage message = objectMapper.readValue(payload, PaymentResultMessage.class);
        if (processedMessageService.alreadyProcessed(CONSUMER_NAME, message.getMessageId())) {
            return;
        }

        Order order = orderRepository.findByIdForUpdate(message.getOrderId()).orElse(null);
        if (order == null) {
            processedMessageService.markProcessed(CONSUMER_NAME, message.getMessageId());
            return;
        }

        PaymentTxn txn = paymentTxnRepository.findByOrderId(order.getId()).orElseGet(PaymentTxn::new);
        txn.setOrderId(order.getId());

        StockReservation reservation = stockReservationRepository.findById(order.getId()).orElse(null);

        if (message.isSuccess()) {
            if (order.getStatus() == OrderStatus.UNPAID || order.getStatus() == OrderStatus.PAYING) {
                int moved = orderRepository.moveStatus(order.getId(), order.getUserId(), OrderStatus.UNPAID, OrderStatus.PAID);
                if (moved == 0) {
                    orderRepository.moveStatus(order.getId(), order.getUserId(), OrderStatus.PAYING, OrderStatus.PAID);
                }
            }
            txn.setStatus("SUCCESS");
            if (reservation != null && ReservationStatus.RESERVED.equals(reservation.getStatus())) {
                reservation.setStatus(ReservationStatus.CONFIRMED);
                stockReservationRepository.save(reservation);
            }
        } else {
            if (order.getStatus() == OrderStatus.UNPAID || order.getStatus() == OrderStatus.PAYING) {
                orderService.markCancelledAndRollbackRedis(order, message.getReason());
            }
            txn.setStatus("FAILED");
            if (reservation != null && ReservationStatus.RESERVED.equals(reservation.getStatus())) {
                Product product = productRepository.findByIdForUpdate(order.getProductId()).orElse(null);
                if (product != null) {
                    product.setStock(product.getStock() + reservation.getQuantity());
                    productRepository.save(product);
                }
                reservation.setStatus(ReservationStatus.RELEASED);
                stockReservationRepository.save(reservation);
            }
        }

        paymentTxnRepository.save(txn);
        processedMessageService.markProcessed(CONSUMER_NAME, message.getMessageId());
    }
}
