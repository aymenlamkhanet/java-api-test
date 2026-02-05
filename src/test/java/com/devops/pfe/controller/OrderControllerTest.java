package com.devops.pfe.controller;

import com.devops.pfe.dto.OrderDTO;
import com.devops.pfe.dto.OrderItemDTO;
import com.devops.pfe.entity.OrderStatus;
import com.devops.pfe.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
@DisplayName("OrderController Tests")
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    private OrderDTO orderDTO;
    private OrderItemDTO orderItemDTO;

    @BeforeEach
    void setUp() {
        orderItemDTO = OrderItemDTO.builder()
                .productId(1L)
                .productName("Test Product")
                .quantity(2)
                .unitPrice(new BigDecimal("99.99"))
                .totalPrice(new BigDecimal("199.98"))
                .build();

        orderDTO = OrderDTO.builder()
                .id(1L)
                .orderNumber("ORD-12345678")
                .customerName("John Doe")
                .customerEmail("john.doe@example.com")
                .status("PENDING")
                .items(Arrays.asList(orderItemDTO))
                .build();
    }

    @Nested
    @DisplayName("Order CRUD Endpoints Tests")
    class OrderCrudEndpointsTests {

        @Test
        @DisplayName("POST /api/v1/orders - Should create order")
        void shouldCreateOrder() throws Exception {
            // Given
            when(orderService.createOrder(any(OrderDTO.class))).thenReturn(orderDTO);

            // When/Then
            mockMvc.perform(post("/api/v1/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(orderDTO)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.orderNumber", is("ORD-12345678")))
                    .andExpect(jsonPath("$.customerEmail", is("john.doe@example.com")));
        }

        @Test
        @DisplayName("GET /api/v1/orders/{id} - Should get order by ID")
        void shouldGetOrderById() throws Exception {
            // Given
            when(orderService.getOrderById(1L)).thenReturn(orderDTO);

            // When/Then
            mockMvc.perform(get("/api/v1/orders/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.orderNumber", is("ORD-12345678")));
        }

        @Test
        @DisplayName("GET /api/v1/orders/number/{orderNumber} - Should get order by number")
        void shouldGetOrderByNumber() throws Exception {
            // Given
            when(orderService.getOrderByNumber("ORD-12345678")).thenReturn(orderDTO);

            // When/Then
            mockMvc.perform(get("/api/v1/orders/number/ORD-12345678"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orderNumber", is("ORD-12345678")));
        }

        @Test
        @DisplayName("GET /api/v1/orders - Should get all orders")
        void shouldGetAllOrders() throws Exception {
            // Given
            List<OrderDTO> orders = Arrays.asList(orderDTO);
            when(orderService.getAllOrders()).thenReturn(orders);

            // When/Then
            mockMvc.perform(get("/api/v1/orders"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)));
        }

        @Test
        @DisplayName("POST /api/v1/orders/{id}/cancel - Should cancel order")
        void shouldCancelOrder() throws Exception {
            // Given
            doNothing().when(orderService).cancelOrder(1L);

            // When/Then
            mockMvc.perform(post("/api/v1/orders/1/cancel"))
                    .andExpect(status().isNoContent());

            verify(orderService).cancelOrder(1L);
        }
    }

    @Nested
    @DisplayName("Order Status Endpoints Tests")
    class OrderStatusEndpointsTests {

        @Test
        @DisplayName("PATCH /api/v1/orders/{id}/status - Should update status")
        void shouldUpdateOrderStatus() throws Exception {
            // Given
            orderDTO.setStatus("CONFIRMED");
            when(orderService.updateOrderStatus(1L, OrderStatus.CONFIRMED)).thenReturn(orderDTO);

            // When/Then
            mockMvc.perform(patch("/api/v1/orders/1/status")
                            .param("status", "CONFIRMED"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("CONFIRMED")));
        }

        @Test
        @DisplayName("GET /api/v1/orders/status/{status} - Should get orders by status")
        void shouldGetOrdersByStatus() throws Exception {
            // Given
            when(orderService.getOrdersByStatus(OrderStatus.PENDING))
                    .thenReturn(Arrays.asList(orderDTO));

            // When/Then
            mockMvc.perform(get("/api/v1/orders/status/PENDING"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)));
        }
    }

    @Nested
    @DisplayName("Order Business Endpoints Tests")
    class OrderBusinessEndpointsTests {

        @Test
        @DisplayName("GET /api/v1/orders/{id}/total - Should calculate order total")
        void shouldCalculateOrderTotal() throws Exception {
            // Given
            when(orderService.calculateOrderTotal(1L))
                    .thenReturn(new BigDecimal("199.98"));

            // When/Then
            mockMvc.perform(get("/api/v1/orders/1/total"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("199.98"));
        }

        @Test
        @DisplayName("GET /api/v1/orders/count/{status} - Should count orders by status")
        void shouldCountOrdersByStatus() throws Exception {
            // Given
            when(orderService.countOrdersByStatus(OrderStatus.PENDING)).thenReturn(5L);

            // When/Then
            mockMvc.perform(get("/api/v1/orders/count/PENDING"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("5"));
        }

        @Test
        @DisplayName("GET /api/v1/orders/customer - Should get orders by customer email")
        void shouldGetOrdersByCustomerEmail() throws Exception {
            // Given
            when(orderService.getOrdersByCustomerEmail("john.doe@example.com"))
                    .thenReturn(Arrays.asList(orderDTO));

            // When/Then
            mockMvc.perform(get("/api/v1/orders/customer")
                            .param("email", "john.doe@example.com"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)));
        }
    }
}
