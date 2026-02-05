package com.devops.pfe.mapper;

import com.devops.pfe.dto.OrderDTO;
import com.devops.pfe.dto.OrderItemDTO;
import com.devops.pfe.entity.Order;
import com.devops.pfe.entity.OrderItem;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class OrderMapper {

    public OrderDTO toDTO(Order order) {
        if (order == null) {
            return null;
        }
        
        List<OrderItemDTO> itemDTOs = order.getItems().stream()
                .map(this::toItemDTO)
                .collect(Collectors.toList());
        
        return OrderDTO.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .customerName(order.getCustomerName())
                .customerEmail(order.getCustomerEmail())
                .status(order.getStatus().name())
                .items(itemDTOs)
                .build();
    }

    public OrderItemDTO toItemDTO(OrderItem item) {
        if (item == null) {
            return null;
        }
        
        return OrderItemDTO.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .productName(item.getProduct().getName())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .totalPrice(item.getTotalPrice())
                .build();
    }
}
