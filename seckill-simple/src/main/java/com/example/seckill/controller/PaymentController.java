package com.example.seckill.controller;

import com.example.seckill.dto.order.PaymentRequest;
import com.example.seckill.entity.PaymentTxn;
import com.example.seckill.service.PaymentService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> pay(@Valid @RequestBody PaymentRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        try {
            PaymentTxn txn = paymentService.submitPayment(request.getOrderId(), Boolean.TRUE.equals(request.getSuccess()));
            body.put("success", true);
            body.put("message", "payment submitted");
            body.put("data", txn);
            return ResponseEntity.ok(body);
        } catch (EntityNotFoundException e) {
            body.put("success", false);
            body.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
        } catch (IllegalStateException e) {
            body.put("success", false);
            body.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(body);
        }
    }
}
