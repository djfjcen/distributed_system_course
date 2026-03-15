package com.example.seckill.controller;

import com.example.seckill.entity.Product;
import com.example.seckill.service.InventoryService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/seckill")
@Validated
public class SeckillController {

    private final InventoryService inventoryService;

    public SeckillController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping("/{productId}")
    public ResponseEntity<Map<String, Object>> seckill(@PathVariable Long productId,
                                                       @RequestParam(defaultValue = "1") @Min(1) Integer quantity) {
        Map<String, Object> body = new LinkedHashMap<>();
        try {
            Product product = inventoryService.seckill(productId, quantity);
            body.put("success", true);
            body.put("message", "seckill success");
            body.put("productId", product.getId());
            body.put("leftStock", product.getStock());
            return ResponseEntity.ok(body);
        } catch (EntityNotFoundException e) {
            body.put("success", false);
            body.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
        } catch (IllegalArgumentException e) {
            body.put("success", false);
            body.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(body);
        } catch (IllegalStateException e) {
            body.put("success", false);
            body.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
        }
    }
}
