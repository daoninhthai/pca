package com.ecommerce.order.event;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class OrderEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private String eventId;
    private String eventType;
    private Long orderId;
    private String orderNumber;
    private Long userId;
    private String previousStatus;
    private String currentStatus;
    private BigDecimal totalAmount;
    private List<OrderItemEvent> items;
    private LocalDateTime timestamp;

    public OrderEvent() {
        this.eventId = UUID.randomUUID().toString();
        this.timestamp = LocalDateTime.now();
    }

    public OrderEvent(String eventType, Long orderId, String orderNumber) {
        this();
        this.eventType = eventType;
        this.orderId = orderId;
        this.orderNumber = orderNumber;
    }

    public static OrderEvent created(Long orderId, String orderNumber, Long userId,
                                     BigDecimal totalAmount, List<OrderItemEvent> items) {
        OrderEvent event = new OrderEvent("ORDER_CREATED", orderId, orderNumber);
        event.setUserId(userId);
        event.setCurrentStatus("PENDING");
        event.setTotalAmount(totalAmount);
        event.setItems(items);
        return event;
    }

    public static OrderEvent statusUpdated(Long orderId, String orderNumber,
                                           String previousStatus, String currentStatus) {
        OrderEvent event = new OrderEvent("ORDER_STATUS_UPDATED", orderId, orderNumber);
        event.setPreviousStatus(previousStatus);
        event.setCurrentStatus(currentStatus);
        return event;
    }

    public static OrderEvent cancelled(Long orderId, String orderNumber,
                                       Long userId, List<OrderItemEvent> items) {
        OrderEvent event = new OrderEvent("ORDER_CANCELLED", orderId, orderNumber);
        event.setUserId(userId);
        event.setCurrentStatus("CANCELLED");
        event.setItems(items);
        return event;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getPreviousStatus() {
        return previousStatus;
    }

    public void setPreviousStatus(String previousStatus) {
        this.previousStatus = previousStatus;
    }

    public String getCurrentStatus() {
        return currentStatus;
    }

    public void setCurrentStatus(String currentStatus) {
        this.currentStatus = currentStatus;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public List<OrderItemEvent> getItems() {
        return items;
    }

    public void setItems(List<OrderItemEvent> items) {
        this.items = items;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderEvent that = (OrderEvent) o;
        return Objects.equals(eventId, that.eventId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventId);
    }

    @Override
    public String toString() {
        return "OrderEvent{" +
                "eventId='" + eventId + '\'' +
                ", eventType='" + eventType + '\'' +
                ", orderId=" + orderId +
                ", orderNumber='" + orderNumber + '\'' +
                ", currentStatus='" + currentStatus + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }

    /**
     * Nested class representing an order item within an event payload.
     */
    public static class OrderItemEvent implements Serializable {

        private static final long serialVersionUID = 1L;

        private Long productId;
        private String productName;
        private int quantity;
        private BigDecimal unitPrice;

        public OrderItemEvent() {
        }

        public OrderItemEvent(Long productId, String productName, int quantity, BigDecimal unitPrice) {
            this.productId = productId;
            this.productName = productName;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
        }

        public Long getProductId() {
            return productId;
        }

        public void setProductId(Long productId) {
            this.productId = productId;
        }

        public String getProductName() {
            return productName;
        }

        public void setProductName(String productName) {
            this.productName = productName;
        }

        public int getQuantity() {
            return quantity;
        }

        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }

        public BigDecimal getUnitPrice() {
            return unitPrice;
        }

        public void setUnitPrice(BigDecimal unitPrice) {
            this.unitPrice = unitPrice;
        }

        public BigDecimal getSubtotal() {
            return unitPrice.multiply(BigDecimal.valueOf(quantity));
        }

        @Override
        public String toString() {
            return "OrderItemEvent{" +
                    "productId=" + productId +
                    ", quantity=" + quantity +
                    ", unitPrice=" + unitPrice +
                    '}';
        }
    }
}
