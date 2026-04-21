package com.example.seckill.service;

import com.example.seckill.constant.OrderStatus;
import com.example.seckill.entity.Order;
import com.example.seckill.entity.PaymentTxn;
import com.example.seckill.mq.message.PaymentResultMessage;
import com.example.seckill.mq.producer.SeckillOrderProducer;
import com.example.seckill.repository.OrderRepository;
import com.example.seckill.repository.PaymentTxnRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class PaymentService {

    private final OrderRepository orderRepository;
    private final PaymentTxnRepository paymentTxnRepository;
    private final SeckillOrderProducer seckillOrderProducer;

    public PaymentService(OrderRepository orderRepository,
                          PaymentTxnRepository paymentTxnRepository,
                          SeckillOrderProducer seckillOrderProducer) {
        this.orderRepository = orderRepository;
        this.paymentTxnRepository = paymentTxnRepository;
        this.seckillOrderProducer = seckillOrderProducer;
    }

    @Transactional
    public PaymentTxn submitPayment(Long orderId, boolean success) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("order not found: " + orderId));

        if (order.getStatus() == OrderStatus.PAID) {
            throw new IllegalStateException("order already paid");
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalStateException("order already cancelled");
        }
        if (order.getStatus() != OrderStatus.UNPAID && order.getStatus() != OrderStatus.PAYING) {
            throw new IllegalStateException("order is not ready for payment");
        }

        int movedToPaying = 0;
        if (order.getStatus() == OrderStatus.UNPAID) {
            movedToPaying = orderRepository.moveStatus(orderId, order.getUserId(), OrderStatus.UNPAID, OrderStatus.PAYING);
        } else if (order.getStatus() == OrderStatus.PAYING) {
            movedToPaying = 1;
        }
        if (movedToPaying == 0) {
            throw new IllegalStateException("order status changed, please retry");
        }

        PaymentTxn txn = paymentTxnRepository.findByOrderId(orderId).orElseGet(PaymentTxn::new);
        txn.setOrderId(orderId);
        txn.setStatus("PROCESSING");
        PaymentTxn saved = paymentTxnRepository.save(txn);

        PaymentResultMessage message = new PaymentResultMessage();
        message.setMessageId(UUID.randomUUID().toString());
        message.setOrderId(orderId);
        message.setSuccess(success);
        message.setReason(success ? "payment success" : "payment failed");
        seckillOrderProducer.sendPaymentResult(message);

        return saved;
    }
}
