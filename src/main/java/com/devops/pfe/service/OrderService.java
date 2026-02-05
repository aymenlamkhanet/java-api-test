package com.devops.pfe.service;

import com.devops.pfe.dto.OrderDTO;
import com.devops.pfe.entity.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface OrderService {
    
    // CRUD Operations
    OrderDTO createOrder(OrderDTO orderDTO);
    OrderDTO getOrderById(Long id);
    OrderDTO getOrderByNumber(String orderNumber);
    List<OrderDTO> getAllOrders();
    List<OrderDTO> getOrdersByCustomerEmail(String email);
    void cancelOrder(Long id);
    
    // Status Management
    OrderDTO updateOrderStatus(Long id, OrderStatus status);
    List<OrderDTO> getOrdersByStatus(OrderStatus status);
    
    // Business Logic
    List<OrderDTO> getOrdersByDateRange(LocalDateTime start, LocalDateTime end);
    BigDecimal calculateOrderTotal(Long orderId);
    Long countOrdersByStatus(OrderStatus status);
    
    // Order Validation
    boolean validateOrder(OrderDTO orderDTO);
}
