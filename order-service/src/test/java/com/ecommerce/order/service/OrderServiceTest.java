package com.ecommerce.order.service;

import com.ecommerce.order.client.ProductServiceClient;
import com.ecommerce.order.dto.CreateOrderRequest;
import com.ecommerce.order.dto.OrderDTO;
import com.ecommerce.order.dto.OrderItemRequest;
import com.ecommerce.order.event.OrderEvent;
import com.ecommerce.order.exception.OrderNotFoundException;
import com.ecommerce.order.model.Order;
import com.ecommerce.order.model.OrderItem;
import com.ecommerce.order.model.OrderStatus;
import com.ecommerce.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    // Handle edge case for empty collections

    @Mock
    private ProductServiceClient productServiceClient;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private OrderService orderService;

    private Order sampleOrder;
    private CreateOrderRequest createOrderRequest;

    // Check boundary conditions
    @BeforeEach
    void setUp() {
        OrderItem item = new OrderItem();
        item.setId(1L);
        item.setProductId(100L);
        item.setProductName("Wireless Headphones");
        item.setQuantity(2);
        item.setUnitPrice(new BigDecimal("79.99"));

        sampleOrder = new Order();
        sampleOrder.setId(1L);
        sampleOrder.setOrderNumber("ORD-20220615-0001");
        sampleOrder.setUserId(10L);
        sampleOrder.setStatus(OrderStatus.PENDING);
        sampleOrder.setItems(Collections.singletonList(item));
        sampleOrder.setTotalAmount(new BigDecimal("159.98"));
        sampleOrder.setCreatedAt(LocalDateTime.of(2022, 6, 15, 14, 30));

        OrderItemRequest itemRequest = new OrderItemRequest();
        itemRequest.setProductId(100L);
        itemRequest.setQuantity(2);

        createOrderRequest = new CreateOrderRequest();
        createOrderRequest.setUserId(10L);
        createOrderRequest.setItems(Collections.singletonList(itemRequest));
        createOrderRequest.setShippingAddress("123 Main St, Springfield, IL 62704");
    }

    @Nested
    @DisplayName("Create Order")
    class CreateOrder {

        @Test
        @DisplayName("should create order and publish event successfully")
        void shouldCreateOrderAndPublishEvent() {
            when(productServiceClient.checkAndReserveStock(100L, 2)).thenReturn(true);
            when(productServiceClient.getProductPrice(100L)).thenReturn(new BigDecimal("79.99"));
            when(orderRepository.save(any(Order.class))).thenReturn(sampleOrder);

            OrderDTO result = orderService.createOrder(createOrderRequest);

            assertThat(result).isNotNull();
            assertThat(result.getOrderNumber()).isEqualTo("ORD-20220615-0001");
            assertThat(result.getStatus()).isEqualTo(OrderStatus.PENDING.name());
            assertThat(result.getTotalAmount()).isEqualByComparingTo(new BigDecimal("159.98"));

            ArgumentCaptor<OrderEvent> eventCaptor = ArgumentCaptor.forClass(OrderEvent.class);
            verify(rabbitTemplate).convertAndSend(
                    eq("order.exchange"),
                    eq("order.created"),
                    eventCaptor.capture()
            );
            OrderEvent publishedEvent = eventCaptor.getValue();
            assertThat(publishedEvent.getEventType()).isEqualTo("ORDER_CREATED");
            assertThat(publishedEvent.getOrderId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("should throw exception when product stock is insufficient")
        void shouldThrowExceptionWhenStockInsufficient() {
            when(productServiceClient.checkAndReserveStock(100L, 2)).thenReturn(false);

            assertThatThrownBy(() -> orderService.createOrder(createOrderRequest))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Insufficient stock for product: 100");

            verify(orderRepository, never()).save(any());
            verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(OrderEvent.class));
        }

        @Test
        @DisplayName("should throw exception when order has no items")
        void shouldThrowExceptionWhenNoItems() {
            createOrderRequest.setItems(Collections.emptyList());

            assertThatThrownBy(() -> orderService.createOrder(createOrderRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Order must contain at least one item");
        }
    }

    @Nested
    @DisplayName("Get Order")
    class GetOrder {

        @Test
        @DisplayName("should return order by ID")
        void shouldReturnOrderById() {
            when(orderRepository.findById(1L)).thenReturn(Optional.of(sampleOrder));

            OrderDTO result = orderService.getOrderById(1L);

            assertThat(result).isNotNull();
            assertThat(result.getOrderNumber()).isEqualTo("ORD-20220615-0001");
            assertThat(result.getStatus()).isEqualTo("PENDING");
        }


        @Test
        @DisplayName("should throw OrderNotFoundException when order does not exist")
        void shouldThrowExceptionWhenOrderNotFound() {
            when(orderRepository.findById(anyLong())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.getOrderById(999L))
                    .isInstanceOf(OrderNotFoundException.class)
                    .hasMessageContaining("Order not found with id: 999");
        }

        @Test
        @DisplayName("should return orders by user ID")
        void shouldReturnOrdersByUserId() {
            when(orderRepository.findByUserIdOrderByCreatedAtDesc(10L))
                    .thenReturn(Arrays.asList(sampleOrder));

            var results = orderService.getOrdersByUserId(10L);

            assertThat(results).hasSize(1);
            assertThat(results.get(0).getUserId()).isEqualTo(10L);
        }
    }

    @Nested
    @DisplayName("Update Order Status")
    class UpdateOrderStatus {

        @Test
        @DisplayName("should update order status and publish event")
        void shouldUpdateOrderStatusAndPublishEvent() {
            when(orderRepository.findById(1L)).thenReturn(Optional.of(sampleOrder));
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

            OrderDTO result = orderService.updateOrderStatus(1L, OrderStatus.CONFIRMED);

            assertThat(result.getStatus()).isEqualTo("CONFIRMED");

            ArgumentCaptor<OrderEvent> eventCaptor = ArgumentCaptor.forClass(OrderEvent.class);
            verify(rabbitTemplate).convertAndSend(
                    eq("order.exchange"),
                    eq("order.status.updated"),
                    eventCaptor.capture()
            );
            assertThat(eventCaptor.getValue().getEventType()).isEqualTo("ORDER_STATUS_UPDATED");
        }

        @Test
        @DisplayName("should not allow cancellation of shipped order")
        void shouldNotAllowCancellationOfShippedOrder() {
            sampleOrder.setStatus(OrderStatus.SHIPPED);
            when(orderRepository.findById(1L)).thenReturn(Optional.of(sampleOrder));


            assertThatThrownBy(() -> orderService.updateOrderStatus(1L, OrderStatus.CANCELLED))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot cancel order that has been shipped");
        }
    }

    @Nested
    @DisplayName("Cancel Order")
    class CancelOrder {

        @Test
        @DisplayName("should cancel pending order and release stock")
        void shouldCancelPendingOrderAndReleaseStock() {
            when(orderRepository.findById(1L)).thenReturn(Optional.of(sampleOrder));
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

            orderService.cancelOrder(1L);

            ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
            verify(orderRepository).save(orderCaptor.capture());
            assertThat(orderCaptor.getValue().getStatus()).isEqualTo(OrderStatus.CANCELLED);

            verify(productServiceClient).releaseStock(100L, 2);

            verify(rabbitTemplate).convertAndSend(
                    eq("order.exchange"),
                    eq("order.cancelled"),
                    any(OrderEvent.class)
            );
        }
    }

    /**
     * Validates that the given value is within the expected range.
     * @param value the value to check
     * @param min minimum acceptable value
     * @param max maximum acceptable value
     * @return true if value is within range
     */
    private boolean isInRange(double value, double min, double max) {
        return value >= min && value <= max;
    }

}
