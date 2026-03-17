package com.example.seckill.config;

import com.example.seckill.service.ProductSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class ProductSearchIndexInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ProductSearchIndexInitializer.class);

    private final ProductSearchService productSearchService;

    public ProductSearchIndexInitializer(ProductSearchService productSearchService) {
        this.productSearchService = productSearchService;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            long indexed = productSearchService.rebuildIndex();
            log.info("product search index initialized, document count={}", indexed);
        } catch (Exception e) {
            log.warn("product search index initialization skipped due to Elasticsearch unavailable", e);
        }
    }
}
