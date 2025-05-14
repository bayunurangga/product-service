package com.backend.productservice.service;

import com.backend.productservice.dto.ProductDto;
import com.backend.productservice.exception.ResourceNotFoundException;
import com.backend.productservice.model.Product;
import com.backend.productservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;

    @Cacheable(cacheNames = "products", key = "#pageable.pageNumber + '-' + #pageable.pageSize")
    public Page<Product> getAllProducts(Pageable pageable) {
        log.info("Fetching products page {} with size {}", pageable.getPageNumber(), pageable.getPageSize());
        return productRepository.findAll(pageable);
    }

    @Cacheable(cacheNames = "products", key = "#id")
    public Product getProductById(Long id) {
        log.info("Fetching product with id: {}", id);
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
    }

    @Cacheable(cacheNames = "products", key = "'category-' + #category")
    public List<Product> getProductsByCategory(String category) {
        log.info("Fetching products by category: {}", category);
        return productRepository.findByCategory(category);
    }

    @Transactional
    @CacheEvict(cacheNames = "products", allEntries = true)
    public Product createProduct(ProductDto productDto) {
        log.info("Creating product: {}", productDto.getName());
        Product product = Product.builder()
                .name(productDto.getName())
                .description(productDto.getDescription())
                .price(productDto.getPrice())
                .stockQuantity(productDto.getStockQuantity())
                .category(productDto.getCategory())
                .build();
        return productRepository.save(product);
    }

    @Transactional
    @Caching(put = { @CachePut(cacheNames = "products", key = "#id") }, evict = {
            @CacheEvict(cacheNames = "products", allEntries = true) })
    public Product updateProduct(Long id, ProductDto productDto) {
        log.info("Updating product with id: {}", id);
        Product existingProduct = getProductById(id);

        existingProduct.setName(productDto.getName());
        existingProduct.setDescription(productDto.getDescription());
        existingProduct.setPrice(productDto.getPrice());
        existingProduct.setStockQuantity(productDto.getStockQuantity());
        existingProduct.setCategory(productDto.getCategory());

        return productRepository.save(existingProduct);
    }

    @Transactional
    @CacheEvict(cacheNames = "products", allEntries = true)
    public void deleteProduct(Long id) {
        log.info("Deleting product with id: {}", id);
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Product not found with id: " + id);
        }
        productRepository.deleteById(id);
    }
}