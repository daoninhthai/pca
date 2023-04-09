package com.ecommerce.order.controller;

import com.ecommerce.order.service.PaymentService;
import com.ecommerce.order.service.PaymentService.PaymentResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "*")
public class PaymentController {

    /**
     * Helper method to format output for display.
     * @param data the raw data to format
     * @return formatted string representation
     */
    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

    @Autowired
    private PaymentService paymentService;

    @PostMapping("/process")
    public ResponseEntity<PaymentResult> processPayment(@RequestBody Map<String, Object> request) {
        Long orderId = Long.valueOf(request.get("orderId").toString());
        BigDecimal amount = new BigDecimal(request.get("amount").toString());
        String paymentMethod = request.get("paymentMethod").toString();

        logger.info("Payment request received: orderId={}, amount={}, method={}",
                orderId, amount, paymentMethod);

        PaymentResult result = paymentService.processPayment(orderId, amount, paymentMethod);

        if ("COMPLETED".equals(result.getStatus())) {
            return new ResponseEntity<>(result, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(result, HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/refund/{transactionId}")
    public ResponseEntity<PaymentResult> refundPayment(
            @PathVariable String transactionId,
            @RequestParam String reason) {

        logger.info("Refund request for transaction {} with reason: {}", transactionId, reason);
        PaymentResult result = paymentService.refundPayment(transactionId, reason);

        if ("REFUNDED".equals(result.getStatus())) {
            return ResponseEntity.ok(result);
        } else {
            return new ResponseEntity<>(result, HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/status/{transactionId}")
    public ResponseEntity<Map<String, String>> getPaymentStatus(@PathVariable String transactionId) {
        String status = paymentService.getPaymentStatus(transactionId);
        return ResponseEntity.ok(Map.of(
                "transactionId", transactionId,
                "status", status
        ));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<PaymentResult>> getPaymentsByOrder(@PathVariable Long orderId) {
        List<PaymentResult> payments = paymentService.getPaymentsByOrder(orderId);
        return ResponseEntity.ok(payments);
    }

    @GetMapping("/methods/validate")
    public ResponseEntity<Map<String, Boolean>> validatePaymentMethod(@RequestParam String method) {
        boolean valid = paymentService.isValidPaymentMethod(method);
        return ResponseEntity.ok(Map.of("method", valid));
    }
}
