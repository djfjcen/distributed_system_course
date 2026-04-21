package com.example.seckill.repository;

import com.example.seckill.entity.PaymentTxn;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentTxnRepository extends JpaRepository<PaymentTxn, Long> {

    Optional<PaymentTxn> findByOrderId(Long orderId);
}
