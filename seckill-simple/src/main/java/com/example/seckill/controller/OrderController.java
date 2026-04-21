package com.example.seckill.controller;

import com.example.seckill.dto.order.SeckillOrderRequest;
import com.example.seckill.entity.Order;
import com.example.seckill.service.OrderService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * Seckill order: POST /api/orders/seckill
     * Requires user login session or userId query param for testing
     */
    @PostMapping("/seckill")
    public ResponseEntity<Map<String, Object>> seckillOrder(
            @Valid @RequestBody SeckillOrderRequest request,
            HttpSession session,
            @RequestParam(value = "userId", required = false) Long userIdParam) {
        
        Map<String, Object> body = new LinkedHashMap<>();
        
        try {
            // Get userId from session (preferred) or from param (for testing)
            Long userId = (Long) session.getAttribute("userId");
            if (userId == null && userIdParam != null) {
                userId = userIdParam;
            }
            if (userId == null) {
                body.put("success", false);
                body.put("message", "user not logged in");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
            }

            Order order = orderService.seckillOrder(userId, request.getProductId(), request.getQuantity());
            body.put("success", true);
            body.put("message", "seckill order created");
            body.put("data", order);
            return ResponseEntity.status(HttpStatus.CREATED).body(body);

        } catch (IllegalStateException e) {
            body.put("success", false);
            body.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
        } catch (EntityNotFoundException e) {
            body.put("success", false);
            body.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
        } catch (Exception e) {
            body.put("success", false);
            body.put("message", "internal error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
        }
    }

    /**
     * Query order by ID: GET /api/orders/{orderId}
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<Map<String, Object>> getOrderById(@PathVariable Long orderId) {
        Map<String, Object> body = new LinkedHashMap<>();
        try {
            Order order = orderService.getOrderById(orderId);
            body.put("success", true);
            body.put("data", order);
            return ResponseEntity.ok(body);
        } catch (EntityNotFoundException e) {
            body.put("success", false);
            body.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
        }
    }

    /**
     * Query orders by user ID: GET /api/orders/user/{userId}
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<Map<String, Object>> getOrdersByUserId(@PathVariable Long userId) {
        Map<String, Object> body = new LinkedHashMap<>();
        try {
            List<Order> orders = orderService.getOrdersByUserId(userId);
            body.put("success", true);
            body.put("count", orders.size());
            body.put("data", orders);
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            body.put("success", false);
            body.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
        }
    }

    /**
     * Query orders by product ID: GET /api/orders/product/{productId}
     */
    @GetMapping("/product/{productId}")
    public ResponseEntity<Map<String, Object>> getOrdersByProductId(@PathVariable Long productId) {
        Map<String, Object> body = new LinkedHashMap<>();
        try {
            List<Order> orders = orderService.getOrdersByProductId(productId);
            body.put("success", true);
            body.put("count", orders.size());
            body.put("data", orders);
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            body.put("success", false);
            body.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
        }
    }
}
