package com.ecommerce.order.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class PaymentServiceImpl implements PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentServiceImpl.class);

    private static final List<String> SUPPORTED_METHODS = Arrays.asList(
            "CREDIT_CARD", "BANK_TRANSFER", "E_WALLET", "COD"
    );

    // In-memory transaction store; replace with repository in production
    private final Map<String, PaymentResult> transactions = new ConcurrentHashMap<>();
    private final Map<Long, List<String>> orderTransactions = new ConcurrentHashMap<>();

    @Autowired
    private OrderService orderService;

    @Override
    public PaymentResult processPayment(Long orderId, BigDecimal amount, String paymentMethod) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            logger.warn("Invalid payment amount {} for order {}", amount, orderId);
            return createResult(null, "FAILED", amount, paymentMethod,
                    "Payment amount must be positive", orderId);
        }

        if (!isValidPaymentMethod(paymentMethod)) {
            logger.warn("Unsupported payment method '{}' for order {}", paymentMethod, orderId);
            return createResult(null, "FAILED", amount, paymentMethod,
                    "Unsupported payment method: " + paymentMethod, orderId);
        }

        String transactionId = generateTransactionId();
        logger.info("Processing payment: orderId={}, amount={}, method={}, txnId={}",
                orderId, amount, paymentMethod, transactionId);

        // Simulate payment gateway processing
        boolean success = simulateGatewayCall(paymentMethod, amount);

        String status = success ? "COMPLETED" : "FAILED";
        String message = success ? "Payment processed successfully" : "Payment gateway declined";

        PaymentResult result = createResult(transactionId, status, amount,
                paymentMethod, message, orderId);

        transactions.put(transactionId, result);
        orderTransactions.computeIfAbsent(orderId, k -> new ArrayList<>()).add(transactionId);

        if (success) {
            try {
                orderService.updateOrderStatus(orderId,
                        com.ecommerce.order.entity.Order.OrderStatus.CONFIRMED);
                logger.info("Order {} status updated to CONFIRMED after payment", orderId);
            } catch (Exception e) {
                logger.error("Failed to update order status for order {}", orderId, e);
            }
        }

        return result;
    }

    @Override
    public PaymentResult refundPayment(String transactionId, String reason) {
        PaymentResult original = transactions.get(transactionId);
        if (original == null) {
            logger.warn("Transaction {} not found for refund", transactionId);
            return createResult(null, "FAILED", BigDecimal.ZERO, null,
                    "Original transaction not found", null);
        }

        if (!"COMPLETED".equals(original.getStatus())) {
            logger.warn("Cannot refund transaction {} with status {}", transactionId, original.getStatus());
            return createResult(null, "FAILED", original.getAmount(), original.getPaymentMethod(),
                    "Can only refund completed transactions", original.getOrderId());
        }

        String refundTxnId = "REF_" + generateTransactionId();
        PaymentResult refundResult = createResult(refundTxnId, "REFUNDED",
                original.getAmount(), original.getPaymentMethod(),
                "Refund processed. Reason: " + reason, original.getOrderId());

        original.setStatus("REFUNDED");
        transactions.put(refundTxnId, refundResult);

        logger.info("Refund {} issued for original transaction {}. Reason: {}",
                refundTxnId, transactionId, reason);
        return refundResult;
    }

    @Override
    public String getPaymentStatus(String transactionId) {
        PaymentResult result = transactions.get(transactionId);
        return result != null ? result.getStatus() : "NOT_FOUND";
    }

    @Override
    public List<PaymentResult> getPaymentsByOrder(Long orderId) {
        List<String> txnIds = orderTransactions.getOrDefault(orderId, Collections.emptyList());
        return txnIds.stream()
                .map(transactions::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public boolean isValidPaymentMethod(String paymentMethod) {
        return paymentMethod != null && SUPPORTED_METHODS.contains(paymentMethod.toUpperCase());
    }

    private boolean simulateGatewayCall(String method, BigDecimal amount) {
        // Simulate: COD always succeeds; large amounts over 10000 have a chance to fail
        if ("COD".equals(method)) {
            return true;
        }
        return amount.compareTo(new BigDecimal("10000")) < 0 || Math.random() > 0.1;
    }

    private String generateTransactionId() {
        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return "TXN_" + timestamp + "_" + UUID.randomUUID().toString().substring(0, 6);
    }

    private PaymentResult createResult(String txnId, String status, BigDecimal amount,
                                       String method, String message, Long orderId) {
        return new PaymentResult(txnId, status, amount, method, message, orderId);
    }
}
