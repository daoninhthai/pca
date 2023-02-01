package com.ecommerce.order.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility component for calculating shipping costs based on weight,
 * destination zone, and shipping method.
 */
@Component
public class ShippingCalculator {

    private static final Logger logger = LoggerFactory.getLogger(ShippingCalculator.class);

    private static final BigDecimal FREE_SHIPPING_THRESHOLD = new BigDecimal("500000");

    // Base rates per kg by shipping method (in VND)
    private static final Map<String, BigDecimal> BASE_RATES = new HashMap<>();
    static {
        BASE_RATES.put("STANDARD", new BigDecimal("15000"));
        BASE_RATES.put("EXPRESS", new BigDecimal("30000"));
        BASE_RATES.put("OVERNIGHT", new BigDecimal("50000"));
    }

    // Zone multipliers based on distance
    private static final Map<String, BigDecimal> ZONE_MULTIPLIERS = new HashMap<>();
    static {
        ZONE_MULTIPLIERS.put("LOCAL", new BigDecimal("1.0"));
        ZONE_MULTIPLIERS.put("REGIONAL", new BigDecimal("1.5"));
        ZONE_MULTIPLIERS.put("NATIONAL", new BigDecimal("2.0"));
        ZONE_MULTIPLIERS.put("REMOTE", new BigDecimal("3.0"));
    }

    /**
     * Calculate the shipping cost for an order.
     *
     * @param weightKg       the total weight in kilograms
     * @param shippingMethod the shipping method (STANDARD, EXPRESS, OVERNIGHT)
     * @param zone           the destination zone (LOCAL, REGIONAL, NATIONAL, REMOTE)
     * @param orderTotal     the total order amount (for free shipping check)
     * @return the calculated shipping cost
     */
    public BigDecimal calculateShippingCost(double weightKg, String shippingMethod,
                                             String zone, BigDecimal orderTotal) {
        if (weightKg <= 0) {
            logger.warn("Invalid weight: {}kg", weightKg);
            return BigDecimal.ZERO;
        }

        // Free shipping for orders above threshold with STANDARD method
        if ("STANDARD".equals(shippingMethod)
                && orderTotal != null
                && orderTotal.compareTo(FREE_SHIPPING_THRESHOLD) >= 0) {
            logger.info("Free shipping applied for order total {}", orderTotal);
            return BigDecimal.ZERO;
        }

        BigDecimal baseRate = BASE_RATES.getOrDefault(shippingMethod, BASE_RATES.get("STANDARD"));
        BigDecimal zoneMultiplier = ZONE_MULTIPLIERS.getOrDefault(zone, ZONE_MULTIPLIERS.get("NATIONAL"));

        BigDecimal weight = BigDecimal.valueOf(Math.max(weightKg, 0.5));
        BigDecimal cost = baseRate.multiply(weight).multiply(zoneMultiplier)
                .setScale(0, RoundingMode.CEILING);

        // Apply surcharge for heavy packages (over 30kg)
        if (weightKg > 30) {
            BigDecimal surcharge = cost.multiply(new BigDecimal("0.2"))
                    .setScale(0, RoundingMode.CEILING);
            cost = cost.add(surcharge);
            logger.debug("Heavy package surcharge applied: +{}", surcharge);
        }

        logger.info("Shipping calculated: weight={}kg, method={}, zone={}, cost={}",
                weightKg, shippingMethod, zone, cost);
        return cost;
    }

    /**
     * Get the estimated delivery days based on shipping method and zone.
     *
     * @param shippingMethod the shipping method
     * @param zone           the destination zone
     * @return estimated number of business days
     */
    public int getEstimatedDeliveryDays(String shippingMethod, String zone) {
        int baseDays;
        switch (shippingMethod != null ? shippingMethod : "STANDARD") {
            case "OVERNIGHT":
                baseDays = 1;
                break;
            case "EXPRESS":
                baseDays = 2;
                break;
            default:
                baseDays = 5;
        }

        int zoneAdjustment;
        switch (zone != null ? zone : "NATIONAL") {
            case "LOCAL":
                zoneAdjustment = 0;
                break;
            case "REGIONAL":
                zoneAdjustment = 1;
                break;
            case "NATIONAL":
                zoneAdjustment = 2;
                break;
            case "REMOTE":
                zoneAdjustment = 4;
                break;
            default:
                zoneAdjustment = 2;
        }

        return baseDays + zoneAdjustment;
    }

    /**
     * Check if a given shipping method is supported.
     *
     * @param method the shipping method to check
     * @return true if supported
     */
    public boolean isSupportedMethod(String method) {
        return method != null && BASE_RATES.containsKey(method.toUpperCase());
    }

    /**
     * Check if a given zone identifier is valid.
     *
     * @param zone the zone to check
     * @return true if valid
     */
    public boolean isValidZone(String zone) {
        return zone != null && ZONE_MULTIPLIERS.containsKey(zone.toUpperCase());
    }
}
