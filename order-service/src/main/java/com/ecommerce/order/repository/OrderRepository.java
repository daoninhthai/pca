package com.ecommerce.order.repository;


import com.ecommerce.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUserId(Long userId);


    List<Order> findByStatus(Order.OrderStatus status);

    List<Order> findByUserIdOrderByOrderDateDesc(Long userId);

    List<Order> findByOrderDateBetween(LocalDateTime start, LocalDateTime end);

}
