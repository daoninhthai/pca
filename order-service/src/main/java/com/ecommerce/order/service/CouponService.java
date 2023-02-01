package com.ecommerce.order.service;

import java.math.BigDecimal;
import java.util.List;

public interface CouponService {

    /**
     * Validate and apply a coupon code to an order total.
     *
     * @param couponCode  the coupon code to apply
     * @param orderTotal  the current order total
     * @return the discounted total after applying the coupon
     */
    BigDecimal applyCoupon(String couponCode, BigDecimal orderTotal);

    /**
     * Check if a coupon code is valid and not expired.
     *
     * @param couponCode the coupon code to validate
     * @return true if the coupon is valid
     */
    boolean isValidCoupon(String couponCode);

    /**
     * Create a new coupon.
     *
     * @param couponCode     the coupon code
     * @param discountType   PERCENTAGE or FIXED_AMOUNT
     * @param discountValue  the discount value
     * @param minOrderAmount minimum order amount to use the coupon
     * @param maxUses        maximum number of times this coupon can be used
     * @return true if the coupon was created successfully
     */
    boolean createCoupon(String couponCode, String discountType,
                         BigDecimal discountValue, BigDecimal minOrderAmount, int maxUses);

    /**
     * Get the discount amount for a coupon without applying it.
     *
     * @param couponCode the coupon code
     * @param orderTotal the order total
     * @return the discount amount
     */
    BigDecimal getDiscountAmount(String couponCode, BigDecimal orderTotal);

    /**
     * Deactivate a coupon so it can no longer be used.
     *
     * @param couponCode the coupon code to deactivate
     * @return true if deactivated successfully
     */
    boolean deactivateCoupon(String couponCode);

    /**
     * Get all active coupon codes.
     *
     * @return list of active coupon codes
     */
    List<String> getActiveCoupons();
}
