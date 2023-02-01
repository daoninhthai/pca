package com.ecommerce.order.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class CouponServiceImpl implements CouponService {

    private static final Logger logger = LoggerFactory.getLogger(CouponServiceImpl.class);
    private static final BigDecimal MAX_PERCENTAGE = new BigDecimal("100");

    private final Map<String, CouponData> couponStore = new ConcurrentHashMap<>();

    @Override
    public BigDecimal applyCoupon(String couponCode, BigDecimal orderTotal) {
        if (!isValidCoupon(couponCode)) {
            logger.warn("Attempt to apply invalid coupon: {}", couponCode);
            return orderTotal;
        }

        CouponData coupon = couponStore.get(couponCode.toUpperCase());
        if (orderTotal.compareTo(coupon.minOrderAmount) < 0) {
            logger.info("Order total {} below minimum {} for coupon {}",
                    orderTotal, coupon.minOrderAmount, couponCode);
            return orderTotal;
        }

        BigDecimal discount = calculateDiscount(coupon, orderTotal);
        BigDecimal discountedTotal = orderTotal.subtract(discount)
                .max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);

        coupon.usedCount++;

        logger.info("Coupon {} applied: original={}, discount={}, final={}",
                couponCode, orderTotal, discount, discountedTotal);
        return discountedTotal;
    }

    @Override
    public boolean isValidCoupon(String couponCode) {
        if (couponCode == null || couponCode.trim().isEmpty()) {
            return false;
        }

        CouponData coupon = couponStore.get(couponCode.toUpperCase());
        if (coupon == null) {
            logger.debug("Coupon {} not found", couponCode);
            return false;
        }

        if (!coupon.active) {
            logger.debug("Coupon {} is inactive", couponCode);
            return false;
        }

        if (coupon.usedCount >= coupon.maxUses) {
            logger.debug("Coupon {} has reached max uses ({}/{})",
                    couponCode, coupon.usedCount, coupon.maxUses);
            return false;
        }

        return true;
    }

    @Override
    public boolean createCoupon(String couponCode, String discountType,
                                BigDecimal discountValue, BigDecimal minOrderAmount, int maxUses) {
        if (couponCode == null || discountType == null || discountValue == null) {
            logger.warn("Cannot create coupon with null parameters");
            return false;
        }

        String normalizedCode = couponCode.toUpperCase().trim();
        if (couponStore.containsKey(normalizedCode)) {
            logger.warn("Coupon {} already exists", normalizedCode);
            return false;
        }

        if (!"PERCENTAGE".equals(discountType) && !"FIXED_AMOUNT".equals(discountType)) {
            logger.warn("Invalid discount type: {}", discountType);
            return false;
        }

        if ("PERCENTAGE".equals(discountType) && discountValue.compareTo(MAX_PERCENTAGE) > 0) {
            logger.warn("Percentage discount cannot exceed 100%");
            return false;
        }

        CouponData coupon = new CouponData(normalizedCode, discountType, discountValue,
                minOrderAmount != null ? minOrderAmount : BigDecimal.ZERO, maxUses);
        couponStore.put(normalizedCode, coupon);

        logger.info("Coupon created: code={}, type={}, value={}, minOrder={}, maxUses={}",
                normalizedCode, discountType, discountValue, minOrderAmount, maxUses);
        return true;
    }

    @Override
    public BigDecimal getDiscountAmount(String couponCode, BigDecimal orderTotal) {
        if (!isValidCoupon(couponCode)) {
            return BigDecimal.ZERO;
        }

        CouponData coupon = couponStore.get(couponCode.toUpperCase());
        if (orderTotal.compareTo(coupon.minOrderAmount) < 0) {
            return BigDecimal.ZERO;
        }

        return calculateDiscount(coupon, orderTotal);
    }

    @Override
    public boolean deactivateCoupon(String couponCode) {
        CouponData coupon = couponStore.get(couponCode.toUpperCase());
        if (coupon == null) {
            logger.warn("Cannot deactivate non-existent coupon: {}", couponCode);
            return false;
        }

        coupon.active = false;
        logger.info("Coupon {} deactivated", couponCode);
        return true;
    }

    @Override
    public List<String> getActiveCoupons() {
        return couponStore.entrySet().stream()
                .filter(e -> e.getValue().active && e.getValue().usedCount < e.getValue().maxUses)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private BigDecimal calculateDiscount(CouponData coupon, BigDecimal orderTotal) {
        if ("PERCENTAGE".equals(coupon.discountType)) {
            return orderTotal.multiply(coupon.discountValue)
                    .divide(MAX_PERCENTAGE, 2, RoundingMode.HALF_UP);
        } else {
            return coupon.discountValue.min(orderTotal);
        }
    }

    private static class CouponData {
        String code;
        String discountType;
        BigDecimal discountValue;
        BigDecimal minOrderAmount;
        int maxUses;
        int usedCount;
        boolean active;

        CouponData(String code, String discountType, BigDecimal discountValue,
                   BigDecimal minOrderAmount, int maxUses) {
            this.code = code;
            this.discountType = discountType;
            this.discountValue = discountValue;
            this.minOrderAmount = minOrderAmount;
            this.maxUses = maxUses;
            this.usedCount = 0;
            this.active = true;
        }
    }
}
