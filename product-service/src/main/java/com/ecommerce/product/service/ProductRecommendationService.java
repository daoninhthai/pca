package com.ecommerce.product.service;

import com.ecommerce.product.entity.Product;
import com.ecommerce.product.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service for generating product recommendations based on user browsing history,
 * purchase patterns, and product similarity.
 */
@Service
public class ProductRecommendationService {

    private static final Logger logger = LoggerFactory.getLogger(ProductRecommendationService.class);
    private static final int DEFAULT_RECOMMENDATION_COUNT = 8;

    @Autowired
    private ProductRepository productRepository;

    // Track user browsing history: userId -> list of productIds viewed
    private final Map<Long, List<Long>> browsingHistory = new ConcurrentHashMap<>();

    // Track user purchases: userId -> set of productIds purchased
    private final Map<Long, Set<Long>> purchaseHistory = new ConcurrentHashMap<>();

    /**
     * Record that a user viewed a product, for use in recommendations.
     *
     * @param userId    the user ID
     * @param productId the product ID that was viewed
     */
    public void recordProductView(Long userId, Long productId) {
        browsingHistory.computeIfAbsent(userId, k -> Collections.synchronizedList(new ArrayList<>()));
        List<Long> history = browsingHistory.get(userId);

        // Keep only the most recent 50 views
        if (history.size() >= 50) {
            history.remove(0);
        }
        history.add(productId);

        logger.debug("Recorded product view: user={}, product={}", userId, productId);
    }

    /**
     * Record that a user purchased a product.
     *
     * @param userId    the user ID
     * @param productId the product ID that was purchased
     */
    public void recordPurchase(Long userId, Long productId) {
        purchaseHistory.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet())
                .add(productId);
        logger.debug("Recorded purchase: user={}, product={}", userId, productId);
    }

    /**
     * Get personalized product recommendations for a user.
     * Uses a combination of category-based and price-range similarity.
     *
     * @param userId the user ID
     * @param limit  maximum number of recommendations
     * @return list of recommended products
     */
    public List<Product> getRecommendations(Long userId, int limit) {
        int maxResults = limit > 0 ? limit : DEFAULT_RECOMMENDATION_COUNT;
        List<Long> viewedProducts = browsingHistory.getOrDefault(userId, Collections.emptyList());
        Set<Long> purchasedProducts = purchaseHistory.getOrDefault(userId, Collections.emptySet());

        if (viewedProducts.isEmpty() && purchasedProducts.isEmpty()) {
            logger.info("No history for user {}. Returning popular products.", userId);
            return getPopularProducts(maxResults);
        }

        // Collect categories and price ranges from viewed/purchased products
        Set<Long> allInteractedIds = new HashSet<>(viewedProducts);
        allInteractedIds.addAll(purchasedProducts);

        List<Product> interactedProducts = allInteractedIds.stream()
                .map(id -> productRepository.findById(id).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        Set<Long> categoryIds = interactedProducts.stream()
                .filter(p -> p.getCategory() != null)
                .map(p -> p.getCategory().getId())
                .collect(Collectors.toSet());

        BigDecimal avgPrice = interactedProducts.stream()
                .map(Product::getPrice)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(Math.max(interactedProducts.size(), 1)), 2, RoundingMode.HALF_UP);

        BigDecimal priceLower = avgPrice.multiply(new BigDecimal("0.5"));
        BigDecimal priceUpper = avgPrice.multiply(new BigDecimal("2.0"));

        // Find similar products excluding already interacted ones
        List<Product> recommendations = productRepository.findAll().stream()
                .filter(Product::getActive)
                .filter(p -> !allInteractedIds.contains(p.getId()))
                .filter(p -> {
                    boolean categoryMatch = p.getCategory() != null
                            && categoryIds.contains(p.getCategory().getId());
                    boolean priceMatch = p.getPrice() != null
                            && p.getPrice().compareTo(priceLower) >= 0
                            && p.getPrice().compareTo(priceUpper) <= 0;
                    return categoryMatch || priceMatch;
                })
                .sorted(Comparator.comparing(Product::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(maxResults)
                .collect(Collectors.toList());

        logger.info("Generated {} recommendations for user {} from {} categories",
                recommendations.size(), userId, categoryIds.size());
        return recommendations;
    }

    /**
     * Get popular/trending products as a fallback for users without history.
     *
     * @param limit maximum number of products
     * @return list of popular products
     */
    public List<Product> getPopularProducts(int limit) {
        return productRepository.findAll().stream()
                .filter(Product::getActive)
                .filter(p -> p.getStock() > 0)
                .sorted(Comparator.comparing(Product::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Get products similar to a specific product based on category and price range.
     *
     * @param productId the reference product ID
     * @param limit     maximum number of similar products
     * @return list of similar products
     */
    public List<Product> getSimilarProducts(Long productId, int limit) {
        Product reference = productRepository.findById(productId).orElse(null);
        if (reference == null) {
            logger.warn("Product {} not found for similarity search", productId);
            return Collections.emptyList();
        }

        BigDecimal priceRange = reference.getPrice().multiply(new BigDecimal("0.3"));
        BigDecimal minPrice = reference.getPrice().subtract(priceRange);
        BigDecimal maxPrice = reference.getPrice().add(priceRange);

        return productRepository.findAll().stream()
                .filter(Product::getActive)
                .filter(p -> !p.getId().equals(productId))
                .filter(p -> {
                    boolean sameCategory = reference.getCategory() != null
                            && p.getCategory() != null
                            && reference.getCategory().getId().equals(p.getCategory().getId());
                    boolean similarPrice = p.getPrice() != null
                            && p.getPrice().compareTo(minPrice) >= 0
                            && p.getPrice().compareTo(maxPrice) <= 0;
                    return sameCategory && similarPrice;
                })
                .limit(limit)
                .collect(Collectors.toList());
    }
}
