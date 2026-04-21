package com.example.seckill.service;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.example.seckill.datasource.ReadOnlyDataSource;
import com.example.seckill.entity.Product;
import com.example.seckill.repository.ProductRepository;
import com.example.seckill.search.document.ProductDocument;
import com.example.seckill.search.repository.ProductSearchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ProductSearchService {

    private static final Logger log = LoggerFactory.getLogger(ProductSearchService.class);

    private final ProductRepository productRepository;
    private final ProductSearchRepository productSearchRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    public ProductSearchService(ProductRepository productRepository,
                                ProductSearchRepository productSearchRepository,
                                ElasticsearchOperations elasticsearchOperations) {
        this.productRepository = productRepository;
        this.productSearchRepository = productSearchRepository;
        this.elasticsearchOperations = elasticsearchOperations;
    }

    @ReadOnlyDataSource
    public List<ProductDocument> searchProducts(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            List<ProductDocument> result = new ArrayList<>();
            productSearchRepository.findAll().forEach(result::add);
            return result;
        }

        String normalizedKeyword = keyword.trim();
        // 使用 wildcard 精确匹配子串，避免 match 对中文逐字分词导致的误命中
        Query query = Query.of(q -> q
                .wildcard(w -> w.field("name.keyword").value("*" + normalizedKeyword + "*"))
        );

        NativeQuery nativeQuery = NativeQuery.builder()
                .withQuery(query)
                .withPageable(PageRequest.of(0, 100))
                .build();

        List<ProductDocument> result = new ArrayList<>();
        for (SearchHit<ProductDocument> hit : elasticsearchOperations.search(nativeQuery, ProductDocument.class)) {
            result.add(hit.getContent());
        }
        return result;
    }

    @ReadOnlyDataSource
    public long rebuildIndex() {
        List<Product> products = productRepository.findAll();
        List<ProductDocument> documents = products.stream().map(this::toDocument).toList();
        IndexOperations indexOperations = elasticsearchOperations.indexOps(ProductDocument.class);
        if (indexOperations.exists()) {
            indexOperations.delete();
        }
        indexOperations.create();
        indexOperations.putMapping(indexOperations.createMapping(ProductDocument.class));
        productSearchRepository.saveAll(documents);
        return documents.size();
    }

    public void saveOrUpdate(Product product) {
        try {
            productSearchRepository.save(toDocument(product));
        } catch (Exception e) {
            log.warn("elasticsearch index update failed for product id {}", product.getId(), e);
        }
    }

    private ProductDocument toDocument(Product product) {
        ProductDocument document = new ProductDocument();
        document.setId(product.getId());
        document.setName(product.getName());
        document.setStock(product.getStock());
        document.setPrice(product.getPrice().doubleValue());
        return document;
    }
}
