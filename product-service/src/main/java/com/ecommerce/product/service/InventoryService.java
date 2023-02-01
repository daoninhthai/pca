package com.ecommerce.product.service;

import java.util.List;
import java.util.Map;

public interface InventoryService {

    /**
     * Reserve stock for a product when an order is placed.
     *
     * @param productId the product ID
     * @param quantity  the quantity to reserve
     * @return true if the reservation was successful
     */
    boolean reserveStock(Long productId, int quantity);

    /**
     * Release previously reserved stock (e.g., on order cancellation).
     *
     * @param productId the product ID
     * @param quantity  the quantity to release
     */
    void releaseStock(Long productId, int quantity);

    /**
     * Confirm a stock reservation (e.g., after payment is confirmed).
     *
     * @param productId the product ID
     * @param quantity  the quantity to confirm
     */
    void confirmStockReduction(Long productId, int quantity);

    /**
     * Get the current available stock for a product.
     *
     * @param productId the product ID
     * @return available quantity (total minus reserved)
     */
    int getAvailableStock(Long productId);

    /**
     * Check if a product has sufficient stock for the requested quantity.
     *
     * @param productId the product ID
     * @param quantity  the required quantity
     * @return true if sufficient stock is available
     */
    boolean isInStock(Long productId, int quantity);

    /**
     * Get products that are low in stock (below threshold).
     *
     * @param threshold the minimum stock threshold
     * @return list of product IDs with stock below the threshold
     */
    List<Long> getLowStockProducts(int threshold);

    /**
     * Get the stock levels for multiple products at once.
     *
     * @param productIds list of product IDs
     * @return map of productId to available stock
     */
    Map<Long, Integer> getBulkStockLevels(List<Long> productIds);
}
