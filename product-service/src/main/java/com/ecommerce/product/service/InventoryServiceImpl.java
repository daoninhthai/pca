package com.ecommerce.product.service;

import com.ecommerce.product.entity.Product;
import com.ecommerce.product.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@Transactional
public class InventoryServiceImpl implements InventoryService {

    private static final Logger logger = LoggerFactory.getLogger(InventoryServiceImpl.class);

    @Autowired
    private ProductRepository productRepository;

    // Track reserved stock: productId -> reserved quantity
    private final Map<Long, Integer> reservedStock = new ConcurrentHashMap<>();

    @Override
    public boolean reserveStock(Long productId, int quantity) {
        if (quantity <= 0) {
            logger.warn("Invalid reservation quantity {} for product {}", quantity, productId);
            return false;
        }

        Product product = findProduct(productId);
        int available = product.getStock() - reservedStock.getOrDefault(productId, 0);

        if (available < quantity) {
            logger.warn("Insufficient stock for product {}: available={}, requested={}",
                    productId, available, quantity);
            return false;
        }

        reservedStock.merge(productId, quantity, Integer::sum);
        logger.info("Reserved {} units of product {}. Total reserved: {}",
                quantity, productId, reservedStock.get(productId));
        return true;
    }

    @Override
    public void releaseStock(Long productId, int quantity) {
        Integer currentReserved = reservedStock.get(productId);
        if (currentReserved == null || currentReserved == 0) {
            logger.debug("No reserved stock to release for product {}", productId);
            return;
        }

        int newReserved = Math.max(0, currentReserved - quantity);
        if (newReserved == 0) {
            reservedStock.remove(productId);
        } else {
            reservedStock.put(productId, newReserved);
        }

        logger.info("Released {} units of product {}. Remaining reserved: {}",
                quantity, productId, newReserved);
    }

    @Override
    public void confirmStockReduction(Long productId, int quantity) {
        Product product = findProduct(productId);
        int newStock = product.getStock() - quantity;

        if (newStock < 0) {
            logger.error("Stock would go negative for product {}: current={}, reduce={}",
                    productId, product.getStock(), quantity);
            throw new RuntimeException("Cannot reduce stock below zero for product: " + productId);
        }

        product.setStock(newStock);
        productRepository.save(product);

        // Also remove from reserved tracking
        releaseStock(productId, quantity);

        logger.info("Stock confirmed for product {}: {} -> {}", productId, product.getStock() + quantity, newStock);
    }

    @Override
    public int getAvailableStock(Long productId) {
        Product product = findProduct(productId);
        int reserved = reservedStock.getOrDefault(productId, 0);
        return Math.max(0, product.getStock() - reserved);
    }

    @Override
    public boolean isInStock(Long productId, int quantity) {
        return getAvailableStock(productId) >= quantity;
    }

    @Override
    public List<Long> getLowStockProducts(int threshold) {
        List<Product> allProducts = productRepository.findAll();
        return allProducts.stream()
                .filter(p -> p.getActive() && p.getStock() <= threshold)
                .map(Product::getId)
                .collect(Collectors.toList());
    }

    @Override
    public Map<Long, Integer> getBulkStockLevels(List<Long> productIds) {
        Map<Long, Integer> levels = new HashMap<>();
        for (Long productId : productIds) {
            try {
                levels.put(productId, getAvailableStock(productId));
            } catch (EntityNotFoundException e) {
                logger.warn("Product {} not found during bulk stock check", productId);
                levels.put(productId, 0);
            }
        }
        return levels;
    }

    private Product findProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + productId));
    }
}
