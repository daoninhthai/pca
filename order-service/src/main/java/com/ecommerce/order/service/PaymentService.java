package com.ecommerce.order.service;

import java.math.BigDecimal;
import java.util.List;

public interface PaymentService {

    /**
     * Process a payment for an order.
     *
     * @param orderId       the ID of the order
     * @param amount        the payment amount
     * @param paymentMethod the payment method (CREDIT_CARD, BANK_TRANSFER, E_WALLET)
     * @return a PaymentResult containing the transaction status and reference
     */
    PaymentResult processPayment(Long orderId, BigDecimal amount, String paymentMethod);

    /**
     * Refund a previously completed payment.
     *
     * @param transactionId the original transaction ID
     * @param reason        the reason for the refund
     * @return a PaymentResult for the refund transaction
     */
    PaymentResult refundPayment(String transactionId, String reason);

    /**
     * Get the payment status for a given transaction.
     *
     * @param transactionId the transaction ID
     * @return current payment status
     */
    String getPaymentStatus(String transactionId);

    /**
     * Get all payment transactions for a specific order.
     *
     * @param orderId the order ID
     * @return list of payment results for the order
     */
    List<PaymentResult> getPaymentsByOrder(Long orderId);

    /**
     * Validate a payment method string.
     *
     * @param paymentMethod the method to validate
     * @return true if the method is supported
     */
    boolean isValidPaymentMethod(String paymentMethod);

    /**
     * Value object representing the result of a payment operation.
     */
    class PaymentResult {
        private String transactionId;
        private String status;
        private BigDecimal amount;
        private String paymentMethod;
        private String message;
        private Long orderId;

        public PaymentResult() {}

        public PaymentResult(String transactionId, String status, BigDecimal amount,
                             String paymentMethod, String message, Long orderId) {
            this.transactionId = transactionId;
            this.status = status;
            this.amount = amount;
            this.paymentMethod = paymentMethod;
            this.message = message;
            this.orderId = orderId;
        }

        public String getTransactionId() { return transactionId; }
        public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public String getPaymentMethod() { return paymentMethod; }
        public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public Long getOrderId() { return orderId; }
        public void setOrderId(Long orderId) { this.orderId = orderId; }
    }
}
