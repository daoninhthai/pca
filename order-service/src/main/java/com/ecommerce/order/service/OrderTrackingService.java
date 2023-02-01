package com.ecommerce.order.service;

import java.util.List;

public interface OrderTrackingService {

    /**
     * Add a tracking event to an order.
     *
     * @param orderId     the order ID
     * @param status      the tracking status
     * @param location    the current location/facility
     * @param description additional description of the event
     */
    void addTrackingEvent(Long orderId, String status, String location, String description);

    /**
     * Get the full tracking history for an order.
     *
     * @param orderId the order ID
     * @return list of tracking events in chronological order
     */
    List<TrackingEvent> getTrackingHistory(Long orderId);

    /**
     * Get the latest tracking event for an order.
     *
     * @param orderId the order ID
     * @return the most recent tracking event, or null if none exist
     */
    TrackingEvent getLatestTrackingEvent(Long orderId);

    /**
     * Get the estimated delivery date as a formatted string.
     *
     * @param orderId the order ID
     * @return estimated delivery date string
     */
    String getEstimatedDelivery(Long orderId);

    /**
     * Value object representing a single tracking event.
     */
    class TrackingEvent {
        private Long orderId;
        private String status;
        private String location;
        private String description;
        private String timestamp;

        public TrackingEvent() {}

        public TrackingEvent(Long orderId, String status, String location,
                             String description, String timestamp) {
            this.orderId = orderId;
            this.status = status;
            this.location = location;
            this.description = description;
            this.timestamp = timestamp;
        }

        public Long getOrderId() { return orderId; }
        public void setOrderId(Long orderId) { this.orderId = orderId; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    }
}
