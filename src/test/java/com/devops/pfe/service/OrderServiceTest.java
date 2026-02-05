package com.devops.pfe.service;

import com.devops.pfe.dto.OrderDTO;
import com.devops.pfe.dto.OrderItemDTO;
import com.devops.pfe.entity.*;
import com.devops.pfe.exception.BusinessException;
import com.devops.pfe.exception.InsufficientStockException;
import com.devops.pfe.exception.ResourceNotFoundException;
import com.devops.pfe.mapper.OrderMapper;
import com.devops.pfe.repository.OrderRepository;
import com.devops.pfe.repository.ProductRepository;
import com.devops.pfe.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService Tests")
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductService productService;

    @Mock
    private OrderMapper orderMapper;

    @InjectMocks
    private OrderServiceImpl orderService;

    private Order order;
    private OrderDTO orderDTO;
    private Product product;
    private OrderItemDTO orderItemDTO;

    @BeforeEach
    void setUp() {
        product = Product.builder()
                .id(1L)
                .name("Test Product")
                .price(new BigDecimal("99.99"))
                .stockQuantity(100)
                .active(true)
                .build();

        orderItemDTO = OrderItemDTO.builder()
                .productId(1L)
                .productName("Test Product")
                .quantity(2)
                .unitPrice(new BigDecimal("99.99"))
                .build();

        orderDTO = OrderDTO.builder()
                .customerName("John Doe")
                .customerEmail("john.doe@example.com")
                .items(Arrays.asList(orderItemDTO))
                .build();

        OrderItem orderItem = OrderItem.builder()
                .id(1L)
                .product(product)
                .quantity(2)
                .unitPrice(new BigDecimal("99.99"))
                .build();

        order = Order.builder()
                .id(1L)
                .orderNumber("ORD-12345678")
                .customerName("John Doe")
                .customerEmail("john.doe@example.com")
                .status(OrderStatus.PENDING)
                .items(Arrays.asList(orderItem))
                .build();
    }

    @Nested
    @DisplayName("Order Creation Tests")
    class OrderCreationTests {

        @Test
        @DisplayName("Should create order successfully")
        void shouldCreateOrderSuccessfully() {
            // Given
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
            when(productService.checkStockAvailability(1L, 2)).thenReturn(true);
            when(orderRepository.save(any(Order.class))).thenReturn(order);
            when(orderMapper.toDTO(any(Order.class))).thenReturn(orderDTO);

            // When
            OrderDTO result = orderService.createOrder(orderDTO);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getCustomerEmail()).isEqualTo("john.doe@example.com");
            verify(productService).removeStock(1L, 2);
        }

        @Test
        @DisplayName("Should throw exception when product not found")
        void shouldThrowExceptionWhenProductNotFound() {
            // Given
            when(productRepository.findById(1L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> orderService.createOrder(orderDTO))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Should throw exception when insufficient stock")
        void shouldThrowExceptionWhenInsufficientStock() {
            // Given
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
            when(productService.checkStockAvailability(1L, 2)).thenReturn(false);

            // When/Then
            assertThatThrownBy(() -> orderService.createOrder(orderDTO))
                    .isInstanceOf(InsufficientStockException.class);
        }

        @Test
        @DisplayName("Should throw exception when order has no items")
        void shouldThrowExceptionWhenOrderHasNoItems() {
            // Given
            OrderDTO emptyOrderDTO = OrderDTO.builder()
                    .customerName("John Doe")
                    .customerEmail("john.doe@example.com")
                    .items(Arrays.asList())
                    .build();

            // When/Then
            assertThatThrownBy(() -> orderService.createOrder(emptyOrderDTO))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("au moins un article");
        }
    }

    @Nested
    @DisplayName("Order Retrieval Tests")
    class OrderRetrievalTests {

        @Test
        @DisplayName("Should get order by ID")
        void shouldGetOrderById() {
            // Given
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
            when(orderMapper.toDTO(order)).thenReturn(orderDTO);

            // When
            OrderDTO result = orderService.getOrderById(1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getCustomerEmail()).isEqualTo("john.doe@example.com");
        }

        @Test
        @DisplayName("Should throw exception when order not found")
        void shouldThrowExceptionWhenOrderNotFound() {
            // Given
            when(orderRepository.findById(999L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> orderService.getOrderById(999L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Should get order by order number")
        void shouldGetOrderByNumber() {
            // Given
            when(orderRepository.findByOrderNumber("ORD-12345678")).thenReturn(Optional.of(order));
            when(orderMapper.toDTO(order)).thenReturn(orderDTO);

            // When
            OrderDTO result = orderService.getOrderByNumber("ORD-12345678");

            // Then
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("Should get all orders")
        void shouldGetAllOrders() {
            // Given
            when(orderRepository.findAll()).thenReturn(Arrays.asList(order));
            when(orderMapper.toDTO(order)).thenReturn(orderDTO);

            // When
            List<OrderDTO> result = orderService.getAllOrders();

            // Then
            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Should get orders by customer email")
        void shouldGetOrdersByCustomerEmail() {
            // Given
            when(orderRepository.findByCustomerEmail("john.doe@example.com"))
                    .thenReturn(Arrays.asList(order));
            when(orderMapper.toDTO(order)).thenReturn(orderDTO);

            // When
            List<OrderDTO> result = orderService.getOrdersByCustomerEmail("john.doe@example.com");

            // Then
            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Order Cancellation Tests")
    class OrderCancellationTests {

        @Test
        @DisplayName("Should cancel pending order successfully")
        void shouldCancelPendingOrderSuccessfully() {
            // Given
            order.setStatus(OrderStatus.PENDING);
            OrderItem orderItem = order.getItems().get(0);
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

            // When
            orderService.cancelOrder(1L);

            // Then
            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
            verify(productService).addStock(orderItem.getProduct().getId(), orderItem.getQuantity());
        }

        @Test
        @DisplayName("Should throw exception when cancelling shipped order")
        void shouldThrowExceptionWhenCancellingShippedOrder() {
            // Given
            order.setStatus(OrderStatus.SHIPPED);
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

            // When/Then
            assertThatThrownBy(() -> orderService.cancelOrder(1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("expédiée");
        }

        @Test
        @DisplayName("Should throw exception when order already cancelled")
        void shouldThrowExceptionWhenOrderAlreadyCancelled() {
            // Given
            order.setStatus(OrderStatus.CANCELLED);
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

            // When/Then
            assertThatThrownBy(() -> orderService.cancelOrder(1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("déjà annulée");
        }
    }

    @Nested
    @DisplayName("Order Status Update Tests")
    class OrderStatusUpdateTests {

        @Test
        @DisplayName("Should update order status from PENDING to CONFIRMED")
        void shouldUpdateStatusFromPendingToConfirmed() {
            // Given
            order.setStatus(OrderStatus.PENDING);
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
            when(orderRepository.save(order)).thenReturn(order);
            when(orderMapper.toDTO(order)).thenReturn(orderDTO);

            // When
            orderService.updateOrderStatus(1L, OrderStatus.CONFIRMED);

            // Then
            assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        }

        @Test
        @DisplayName("Should throw exception for invalid status transition")
        void shouldThrowExceptionForInvalidStatusTransition() {
            // Given
            order.setStatus(OrderStatus.PENDING);
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

            // When/Then - cannot go from PENDING to SHIPPED directly
            assertThatThrownBy(() -> orderService.updateOrderStatus(1L, OrderStatus.SHIPPED))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Transition de statut invalide");
        }

        @Test
        @DisplayName("Should get orders by status")
        void shouldGetOrdersByStatus() {
            // Given
            when(orderRepository.findByStatus(OrderStatus.PENDING))
                    .thenReturn(Arrays.asList(order));
            when(orderMapper.toDTO(order)).thenReturn(orderDTO);

            // When
            List<OrderDTO> result = orderService.getOrdersByStatus(OrderStatus.PENDING);

            // Then
            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Order Calculation Tests")
    class OrderCalculationTests {

        @Test
        @DisplayName("Should calculate order total correctly")
        void shouldCalculateOrderTotalCorrectly() {
            // Given
            OrderItem item = OrderItem.builder()
                    .quantity(2)
                    .unitPrice(new BigDecimal("50.00"))
                    .build();
            order.setItems(Arrays.asList(item));
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

            // When
            BigDecimal result = orderService.calculateOrderTotal(1L);

            // Then
            assertThat(result).isEqualByComparingTo(new BigDecimal("100.00"));
        }

        @Test
        @DisplayName("Should count orders by status")
        void shouldCountOrdersByStatus() {
            // Given
            when(orderRepository.countByStatus(OrderStatus.PENDING)).thenReturn(5L);

            // When
            Long count = orderService.countOrdersByStatus(OrderStatus.PENDING);

            // Then
            assertThat(count).isEqualTo(5L);
        }
    }

    @Nested
    @DisplayName("Order Validation Tests")
    class OrderValidationTests {

        @Test
        @DisplayName("Should validate order successfully")
        void shouldValidateOrderSuccessfully() {
            // Given
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));

            // When
            boolean result = orderService.validateOrder(orderDTO);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should throw exception for inactive product")
        void shouldThrowExceptionForInactiveProduct() {
            // Given
            product.setActive(false);
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));

            // When/Then
            assertThatThrownBy(() -> orderService.validateOrder(orderDTO))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("n'est pas disponible");
        }

        @Test
        @DisplayName("Should throw exception when product ID is null")
        void shouldThrowExceptionWhenProductIdIsNull() {
            // Given
            OrderItemDTO invalidItem = OrderItemDTO.builder()
                    .productId(null)
                    .quantity(1)
                    .build();
            OrderDTO invalidOrder = OrderDTO.builder()
                    .customerName("John")
                    .customerEmail("john@example.com")
                    .items(Arrays.asList(invalidItem))
                    .build();

            // When/Then
            assertThatThrownBy(() -> orderService.validateOrder(invalidOrder))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("L'ID du produit");
        }
    }
}
