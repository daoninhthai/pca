package com.ecommerce.order.service;

import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderItem;
import com.ecommerce.order.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for exporting order data to CSV format.
 * Useful for reporting, analytics, and bookkeeping.
 */
@Service
public class OrderExportService {

    private static final Logger logger = LoggerFactory.getLogger(OrderExportService.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String CSV_HEADER =
            "OrderID,UserID,Status,TotalAmount,PaymentMethod,ShippingAddress,OrderDate,ItemCount";

    @Autowired
    private OrderRepository orderRepository;

    /**
     * Export all orders to CSV format.
     *
     * @return byte array containing the CSV data
     */
    public byte[] exportAllOrders() {
        List<Order> orders = orderRepository.findAll();
        logger.info("Exporting {} orders to CSV", orders.size());
        return generateCSV(orders);
    }

    /**
     * Export orders for a specific user to CSV format.
     *
     * @param userId the user ID
     * @return byte array containing the CSV data
     */
    public byte[] exportOrdersByUser(Long userId) {
        List<Order> orders = orderRepository.findByUserIdOrderByOrderDateDesc(userId);
        logger.info("Exporting {} orders for user {} to CSV", orders.size(), userId);
        return generateCSV(orders);
    }

    /**
     * Export orders filtered by status.
     *
     * @param status the order status to filter by
     * @return byte array containing the CSV data
     */
    public byte[] exportOrdersByStatus(Order.OrderStatus status) {
        List<Order> orders = orderRepository.findAll().stream()
                .filter(o -> o.getStatus() == status)
                .collect(Collectors.toList());
        logger.info("Exporting {} orders with status {} to CSV", orders.size(), status);
        return generateCSV(orders);
    }

    /**
     * Export orders within a date range.
     *
     * @param startDate start of the range
     * @param endDate   end of the range
     * @return byte array containing the CSV data
     */
    public byte[] exportOrdersByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        List<Order> orders = orderRepository.findAll().stream()
                .filter(o -> o.getOrderDate() != null
                        && !o.getOrderDate().isBefore(startDate)
                        && !o.getOrderDate().isAfter(endDate))
                .collect(Collectors.toList());

        logger.info("Exporting {} orders between {} and {} to CSV",
                orders.size(), startDate.format(DATE_FMT), endDate.format(DATE_FMT));
        return generateCSV(orders);
    }

    /**
     * Get a revenue summary as CSV.
     *
     * @return byte array with revenue summary CSV
     */
    public byte[] exportRevenueSummary() {
        List<Order> orders = orderRepository.findAll();

        BigDecimal totalRevenue = orders.stream()
                .filter(o -> o.getStatus() != Order.OrderStatus.CANCELLED
                        && o.getStatus() != Order.OrderStatus.REFUNDED)
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long completedCount = orders.stream()
                .filter(o -> o.getStatus() == Order.OrderStatus.DELIVERED)
                .count();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))) {
            writer.println("Metric,Value");
            writer.println("TotalOrders," + orders.size());
            writer.println("CompletedOrders," + completedCount);
            writer.println("TotalRevenue," + totalRevenue.toPlainString());
            writer.flush();
        }

        logger.info("Revenue summary exported: {} orders, revenue={}", orders.size(), totalRevenue);
        return out.toByteArray();
    }

    private byte[] generateCSV(List<Order> orders) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))) {
            // UTF-8 BOM for Excel
            out.write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
            writer.println(CSV_HEADER);

            for (Order order : orders) {
                int itemCount = order.getItems() != null ? order.getItems().size() : 0;
                String line = String.join(",",
                        String.valueOf(order.getId()),
                        String.valueOf(order.getUserId()),
                        order.getStatus() != null ? order.getStatus().name() : "",
                        order.getTotalAmount() != null ? order.getTotalAmount().toPlainString() : "0",
                        escapeCSV(order.getPaymentMethod()),
                        escapeCSV(order.getShippingAddress()),
                        order.getOrderDate() != null ? order.getOrderDate().format(DATE_FMT) : "",
                        String.valueOf(itemCount)
                );
                writer.println(line);
            }

            writer.flush();
        } catch (Exception e) {
            logger.error("Error generating order CSV export", e);
            return new byte[0];
        }

        return out.toByteArray();
    }

    private String escapeCSV(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
