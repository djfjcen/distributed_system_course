package com.example.seckill.service;

import com.example.seckill.entity.Product;
import com.example.seckill.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class InventoryService {

    private final ProductRepository productRepository;

    public InventoryService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<Product> listProducts() {
        return productRepository.findAll();
    }

    @Transactional
    public Product seckill(Long productId, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be greater than 0");
        }

        Product product = productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new EntityNotFoundException("product not found: " + productId));

        if (product.getStock() < quantity) {
            throw new IllegalStateException("stock not enough");
        }

        product.setStock(product.getStock() - quantity);
        return productRepository.save(product);
    }
}
