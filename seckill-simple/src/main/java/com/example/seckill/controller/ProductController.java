package com.example.seckill.controller;

import com.example.seckill.dto.product.ProductCreateRequest;
import com.example.seckill.dto.product.ProductUpdateRequest;
import com.example.seckill.entity.Product;
import com.example.seckill.service.InventoryService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.LinkedHashMap;
import java.util.Map;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final InventoryService inventoryService;

    public ProductController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public List<Product> list() {
        return inventoryService.listProducts();
    }

    @GetMapping("/{productId}")
    public ResponseEntity<Map<String, Object>> detail(@PathVariable Long productId) {
        Map<String, Object> body = new LinkedHashMap<>();
        try {
            Product product = inventoryService.getProductDetail(productId);
            body.put("success", true);
            body.put("data", product);
            body.put("message", "product detail from cache/db");
            return ResponseEntity.ok(body);
        } catch (EntityNotFoundException e) {
            body.put("success", false);
            body.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
        }
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@Valid @RequestBody ProductCreateRequest request) {
        Product created = inventoryService.createProduct(
                request.getName(),
                request.getStock(),
                request.getPrice()
        );

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", true);
        body.put("message", "product created");
        body.put("data", created);
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @PutMapping("/{productId}")
    public ResponseEntity<Map<String, Object>> update(
            @PathVariable Long productId,
            @Valid @RequestBody ProductUpdateRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        try {
            Product updated = inventoryService.updateProduct(
                    productId,
                    request.getName(),
                    request.getStock(),
                    request.getPrice()
            );
            body.put("success", true);
            body.put("message", "product updated");
            body.put("data", updated);
            return ResponseEntity.ok(body);
        } catch (EntityNotFoundException e) {
            body.put("success", false);
            body.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
        }
    }
}
