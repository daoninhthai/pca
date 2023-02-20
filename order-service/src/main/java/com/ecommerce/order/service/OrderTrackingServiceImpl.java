package com.ecommerce.order.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OrderTrackingServiceImpl implements OrderTrackingService {

    private static final Logger logger = LoggerFactory.getLogger(OrderTrackingServiceImpl.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // In-memory store: orderId -> list of tracking events
    private final Map<Long, List<TrackingEvent>> trackingStore = new ConcurrentHashMap<>();

    @Override
    public void addTrackingEvent(Long orderId, String status, String location, String description) {
        if (orderId == null || status == null) {
            logger.warn("Cannot add tracking event: orderId or status is null");
            return;
        }

        String timestamp = LocalDateTime.now().format(FORMATTER);
        TrackingEvent event = new TrackingEvent(orderId, status, location, description, timestamp);

        trackingStore.computeIfAbsent(orderId, k -> Collections.synchronizedList(new ArrayList<>()))
                .add(event);


        logger.info("Tracking event added for order {}: status={}, location={}, desc={}",
                orderId, status, location, description);
    }

    @Override
    public List<TrackingEvent> getTrackingHistory(Long orderId) {
        List<TrackingEvent> events = trackingStore.get(orderId);
        if (events == null) {
            logger.debug("No tracking history found for order {}", orderId);
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(events);
    }

    @Override
    public TrackingEvent getLatestTrackingEvent(Long orderId) {
        List<TrackingEvent> events = trackingStore.get(orderId);
        if (events == null || events.isEmpty()) {
            logger.debug("No tracking events found for order {}", orderId);
            return null;
        }
        return events.get(events.size() - 1);
    }

    @Override
    public String getEstimatedDelivery(Long orderId) {
        TrackingEvent latest = getLatestTrackingEvent(orderId);
        if (latest == null) {
            return "No tracking information available";
        }

        // Estimate delivery based on current status
        int daysToAdd;
        switch (latest.getStatus().toUpperCase()) {
            case "ORDER_PLACED":
                daysToAdd = 7;
                break;
            case "PROCESSING":
                daysToAdd = 5;
                break;
            case "SHIPPED":
                daysToAdd = 3;
                break;
            case "IN_TRANSIT":
                daysToAdd = 2;
                break;
            case "OUT_FOR_DELIVERY":
                daysToAdd = 0;
                break;
            case "DELIVERED":
                return "Order has been delivered";
            default:
                daysToAdd = 7;
        }

        LocalDateTime estimated = LocalDateTime.now().plusDays(daysToAdd);
        String estimatedStr = estimated.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        logger.debug("Estimated delivery for order {}: {} (status: {})",
                orderId, estimatedStr, latest.getStatus());
        return estimatedStr;
    }
}
