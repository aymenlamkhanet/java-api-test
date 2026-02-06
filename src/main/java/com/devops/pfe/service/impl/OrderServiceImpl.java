package com.devops.pfe.service.impl;

import com.devops.pfe.dto.OrderDTO;
import com.devops.pfe.dto.OrderItemDTO;
import com.devops.pfe.entity.Order;
import com.devops.pfe.entity.OrderItem;
import com.devops.pfe.entity.OrderStatus;
import com.devops.pfe.entity.Product;
import com.devops.pfe.exception.BusinessException;
import com.devops.pfe.exception.InsufficientStockException;
import com.devops.pfe.exception.ResourceNotFoundException;
import com.devops.pfe.mapper.OrderMapper;
import com.devops.pfe.repository.OrderRepository;
import com.devops.pfe.repository.ProductRepository;
import com.devops.pfe.service.OrderService;
import com.devops.pfe.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final ProductService productService;
    private final OrderMapper orderMapper;

    @Override
    public OrderDTO createOrder(OrderDTO orderDTO) {
        log.info("Création d'une nouvelle commande pour le client: {}", orderDTO.getCustomerEmail());
        
        // Valider la commande
        validateOrder(orderDTO);
        
        // Créer l'entité Order
        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .customerName(orderDTO.getCustomerName())
                .customerEmail(orderDTO.getCustomerEmail())
                .status(OrderStatus.PENDING)
                .build();
        
        // Traiter chaque article de la commande
        for (OrderItemDTO itemDTO : orderDTO.getItems()) {
            Product product = productRepository.findById(itemDTO.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Produit", itemDTO.getProductId()));
            
            // Vérifier la disponibilité du stock
            if (!productService.checkStockAvailability(product.getId(), itemDTO.getQuantity())) {
                throw new InsufficientStockException(product.getName(), product.getStockQuantity(), itemDTO.getQuantity());
            }
            
            // Créer l'article de commande
            OrderItem orderItem = OrderItem.builder()
                    .product(product)
                    .quantity(itemDTO.getQuantity())
                    .unitPrice(product.getPrice())
                    .build();
            
            order.addItem(orderItem);
            
            // Décrémenter le stock
            productService.removeStock(product.getId(), itemDTO.getQuantity());
        }
        
        Order savedOrder = orderRepository.save(order);
        log.info("Commande créée avec succès. Numéro: {}", savedOrder.getOrderNumber());
        
        return orderMapper.toDTO(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDTO getOrderById(Long id) {
        log.debug("Recherche de la commande avec l'ID: {}", id);
        Order order = findOrderById(id);
        return orderMapper.toDTO(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDTO getOrderByNumber(String orderNumber) {
        log.debug("Recherche de la commande avec le numéro: {}", orderNumber);
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Commande", "numéro", orderNumber));
        return orderMapper.toDTO(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderDTO> getAllOrders() {
        log.debug("Récupération de toutes les commandes");
        return orderRepository.findAll().stream()
                .map(orderMapper::toDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderDTO> getOrdersByCustomerEmail(String email) {
        log.debug("Recherche des commandes pour le client: {}", email);
        return orderRepository.findByCustomerEmail(email).stream()
                .map(orderMapper::toDTO)
                .toList();
    }

    @Override
    public void cancelOrder(Long id) {
        log.info("Annulation de la commande: {}", id);
        
        Order order = findOrderById(id);
        
        // Vérifier si la commande peut être annulée
        if (order.getStatus() == OrderStatus.SHIPPED || order.getStatus() == OrderStatus.DELIVERED) {
            throw new BusinessException("Impossible d'annuler une commande déjà expédiée ou livrée", "ORDER_CANNOT_CANCEL");
        }
        
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BusinessException("La commande est déjà annulée", "ORDER_ALREADY_CANCELLED");
        }
        
        // Remettre le stock pour chaque article
        for (OrderItem item : order.getItems()) {
            productService.addStock(item.getProduct().getId(), item.getQuantity());
        }
        
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
        
        log.info("Commande {} annulée avec succès", id);
    }

    @Override
    public OrderDTO updateOrderStatus(Long id, OrderStatus status) {
        log.info("Mise à jour du statut de la commande {} vers {}", id, status);
        
        Order order = findOrderById(id);
        
        // Valider la transition de statut
        validateStatusTransition(order.getStatus(), status);
        
        order.setStatus(status);
        Order updatedOrder = orderRepository.save(order);
        
        log.info("Statut de la commande {} mis à jour vers {}", id, status);
        return orderMapper.toDTO(updatedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderDTO> getOrdersByStatus(OrderStatus status) {
        log.debug("Recherche des commandes avec le statut: {}", status);
        return orderRepository.findByStatus(status).stream()
                .map(orderMapper::toDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderDTO> getOrdersByDateRange(LocalDateTime start, LocalDateTime end) {
        log.debug("Recherche des commandes entre {} et {}", start, end);
        return orderRepository.findByDateRange(start, end).stream()
                .map(orderMapper::toDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateOrderTotal(Long orderId) {
        log.debug("Calcul du total de la commande: {}", orderId);
        Order order = findOrderById(orderId);
        
        return order.getItems().stream()
                .map(OrderItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    @Transactional(readOnly = true)
    public Long countOrdersByStatus(OrderStatus status) {
        return orderRepository.countByStatus(status);
    }

    @Override
    public boolean validateOrder(OrderDTO orderDTO) {
        // Vérifier que la commande a au moins un article
        if (orderDTO.getItems() == null || orderDTO.getItems().isEmpty()) {
            throw new BusinessException("La commande doit contenir au moins un article", "ORDER_EMPTY");
        }
        
        // Vérifier chaque article
        for (OrderItemDTO item : orderDTO.getItems()) {
            if (item.getProductId() == null) {
                throw new BusinessException("L'ID du produit est obligatoire", "PRODUCT_ID_REQUIRED");
            }
            
            if (item.getQuantity() == null || item.getQuantity() < 1) {
                throw new BusinessException("La quantité doit être au moins 1", "INVALID_QUANTITY");
            }
            
            // Vérifier que le produit existe et est actif
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Produit", item.getProductId()));
            
            if (!product.getActive()) {
                throw new BusinessException("Le produit '" + product.getName() + "' n'est pas disponible", "PRODUCT_NOT_AVAILABLE");
            }
        }
        
        return true;
    }

    private Order findOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Commande", id));
    }

    private String generateOrderNumber() {
        return "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private void validateStatusTransition(OrderStatus currentStatus, OrderStatus newStatus) {
        // Définir les transitions valides
        boolean isValid = switch (currentStatus) {
            case PENDING -> newStatus == OrderStatus.CONFIRMED || newStatus == OrderStatus.CANCELLED;
            case CONFIRMED -> newStatus == OrderStatus.PROCESSING || newStatus == OrderStatus.CANCELLED;
            case PROCESSING -> newStatus == OrderStatus.SHIPPED || newStatus == OrderStatus.CANCELLED;
            case SHIPPED -> newStatus == OrderStatus.DELIVERED;
            case DELIVERED, CANCELLED -> false;
        };
        
        if (!isValid) {
            throw new BusinessException(
                String.format("Transition de statut invalide: %s -> %s", currentStatus, newStatus),
                "INVALID_STATUS_TRANSITION"
            );
        }
    }
}
