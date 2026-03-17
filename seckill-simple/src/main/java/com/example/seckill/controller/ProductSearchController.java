package com.example.seckill.controller;

import com.example.seckill.search.document.ProductDocument;
import com.example.seckill.service.ProductSearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/search")
public class ProductSearchController {

    private final ProductSearchService productSearchService;

    public ProductSearchController(ProductSearchService productSearchService) {
        this.productSearchService = productSearchService;
    }

    @GetMapping("/products")
    public ResponseEntity<Map<String, Object>> searchProducts(@RequestParam(required = false) String keyword) {
        List<ProductDocument> data = productSearchService.searchProducts(keyword);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", true);
        body.put("keyword", keyword == null ? "" : keyword);
        body.put("count", data.size());
        body.put("data", data);
        return ResponseEntity.ok(body);
    }

    @PostMapping("/rebuild")
    public ResponseEntity<Map<String, Object>> rebuildIndex() {
        long indexed = productSearchService.rebuildIndex();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", true);
        body.put("message", "rebuild success");
        body.put("indexed", indexed);
        return ResponseEntity.ok(body);
    }
}
