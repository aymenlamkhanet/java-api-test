package com.devops.pfe.controller;

import com.devops.pfe.dto.ProductDTO;
import com.devops.pfe.exception.ResourceNotFoundException;
import com.devops.pfe.service.ProductService;
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

@WebMvcTest(ProductController.class)
@DisplayName("ProductController Tests")
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    private ProductDTO productDTO;

    @BeforeEach
    void setUp() {
        productDTO = ProductDTO.builder()
                .id(1L)
                .name("Test Product")
                .description("Test Description")
                .price(new BigDecimal("99.99"))
                .stockQuantity(100)
                .category("Electronics")
                .sku("TEST-SKU-001")
                .active(true)
                .build();
    }

    @Nested
    @DisplayName("CRUD Endpoints Tests")
    class CrudEndpointsTests {

        @Test
        @DisplayName("POST /api/v1/products - Should create product")
        void shouldCreateProduct() throws Exception {
            // Given
            when(productService.createProduct(any(ProductDTO.class))).thenReturn(productDTO);

            // When/Then
            mockMvc.perform(post("/api/v1/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(productDTO)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.name", is("Test Product")))
                    .andExpect(jsonPath("$.sku", is("TEST-SKU-001")));

            verify(productService).createProduct(any(ProductDTO.class));
        }

        @Test
        @DisplayName("POST /api/v1/products - Should return 400 for invalid data")
        void shouldReturn400ForInvalidData() throws Exception {
            // Given
            ProductDTO invalidProduct = ProductDTO.builder()
                    .name("") // Invalid: blank name
                    .price(new BigDecimal("-10")) // Invalid: negative price
                    .build();

            // When/Then
            mockMvc.perform(post("/api/v1/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidProduct)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("GET /api/v1/products/{id} - Should get product by ID")
        void shouldGetProductById() throws Exception {
            // Given
            when(productService.getProductById(1L)).thenReturn(productDTO);

            // When/Then
            mockMvc.perform(get("/api/v1/products/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.name", is("Test Product")));
        }

        @Test
        @DisplayName("GET /api/v1/products/{id} - Should return 404 when not found")
        void shouldReturn404WhenNotFound() throws Exception {
            // Given
            when(productService.getProductById(999L))
                    .thenThrow(new ResourceNotFoundException("Produit", 999L));

            // When/Then
            mockMvc.perform(get("/api/v1/products/999"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("GET /api/v1/products - Should get all products")
        void shouldGetAllProducts() throws Exception {
            // Given
            List<ProductDTO> products = Arrays.asList(productDTO);
            when(productService.getAllProducts()).thenReturn(products);

            // When/Then
            mockMvc.perform(get("/api/v1/products"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].name", is("Test Product")));
        }

        @Test
        @DisplayName("PUT /api/v1/products/{id} - Should update product")
        void shouldUpdateProduct() throws Exception {
            // Given
            when(productService.updateProduct(eq(1L), any(ProductDTO.class))).thenReturn(productDTO);

            // When/Then
            mockMvc.perform(put("/api/v1/products/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(productDTO)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(1)));
        }

        @Test
        @DisplayName("DELETE /api/v1/products/{id} - Should delete product")
        void shouldDeleteProduct() throws Exception {
            // Given
            doNothing().when(productService).deleteProduct(1L);

            // When/Then
            mockMvc.perform(delete("/api/v1/products/1"))
                    .andExpect(status().isNoContent());

            verify(productService).deleteProduct(1L);
        }
    }

    @Nested
    @DisplayName("Search & Filter Endpoints Tests")
    class SearchFilterEndpointsTests {

        @Test
        @DisplayName("GET /api/v1/products/category/{category} - Should get by category")
        void shouldGetProductsByCategory() throws Exception {
            // Given
            when(productService.getProductsByCategory("Electronics"))
                    .thenReturn(Arrays.asList(productDTO));

            // When/Then
            mockMvc.perform(get("/api/v1/products/category/Electronics"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)));
        }

        @Test
        @DisplayName("GET /api/v1/products/search - Should search products")
        void shouldSearchProducts() throws Exception {
            // Given
            when(productService.searchProducts("Test"))
                    .thenReturn(Arrays.asList(productDTO));

            // When/Then
            mockMvc.perform(get("/api/v1/products/search")
                            .param("keyword", "Test"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)));
        }

        @Test
        @DisplayName("GET /api/v1/products/price-range - Should get by price range")
        void shouldGetProductsByPriceRange() throws Exception {
            // Given
            when(productService.getProductsByPriceRange(
                    new BigDecimal("50.00"), new BigDecimal("150.00")))
                    .thenReturn(Arrays.asList(productDTO));

            // When/Then
            mockMvc.perform(get("/api/v1/products/price-range")
                            .param("minPrice", "50.00")
                            .param("maxPrice", "150.00"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)));
        }

        @Test
        @DisplayName("GET /api/v1/products/low-stock - Should get low stock products")
        void shouldGetLowStockProducts() throws Exception {
            // Given
            when(productService.getLowStockProducts(10))
                    .thenReturn(Arrays.asList(productDTO));

            // When/Then
            mockMvc.perform(get("/api/v1/products/low-stock")
                            .param("threshold", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)));
        }
    }

    @Nested
    @DisplayName("Stock Management Endpoints Tests")
    class StockManagementEndpointsTests {

        @Test
        @DisplayName("PATCH /api/v1/products/{id}/stock - Should update stock")
        void shouldUpdateStock() throws Exception {
            // Given
            when(productService.updateStock(1L, 50)).thenReturn(productDTO);

            // When/Then
            mockMvc.perform(patch("/api/v1/products/1/stock")
                            .param("quantity", "50"))
                    .andExpect(status().isOk());

            verify(productService).updateStock(1L, 50);
        }

        @Test
        @DisplayName("POST /api/v1/products/{id}/stock/add - Should add stock")
        void shouldAddStock() throws Exception {
            // Given
            when(productService.addStock(1L, 20)).thenReturn(productDTO);

            // When/Then
            mockMvc.perform(post("/api/v1/products/1/stock/add")
                            .param("quantity", "20"))
                    .andExpect(status().isOk());

            verify(productService).addStock(1L, 20);
        }

        @Test
        @DisplayName("POST /api/v1/products/{id}/stock/remove - Should remove stock")
        void shouldRemoveStock() throws Exception {
            // Given
            when(productService.removeStock(1L, 10)).thenReturn(productDTO);

            // When/Then
            mockMvc.perform(post("/api/v1/products/1/stock/remove")
                            .param("quantity", "10"))
                    .andExpect(status().isOk());

            verify(productService).removeStock(1L, 10);
        }

        @Test
        @DisplayName("GET /api/v1/products/{id}/stock/check - Should check availability")
        void shouldCheckStockAvailability() throws Exception {
            // Given
            when(productService.checkStockAvailability(1L, 50)).thenReturn(true);

            // When/Then
            mockMvc.perform(get("/api/v1/products/1/stock/check")
                            .param("quantity", "50"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("true"));
        }
    }

    @Nested
    @DisplayName("Product Status Endpoints Tests")
    class ProductStatusEndpointsTests {

        @Test
        @DisplayName("POST /api/v1/products/{id}/activate - Should activate product")
        void shouldActivateProduct() throws Exception {
            // Given
            when(productService.activateProduct(1L)).thenReturn(productDTO);

            // When/Then
            mockMvc.perform(post("/api/v1/products/1/activate"))
                    .andExpect(status().isOk());

            verify(productService).activateProduct(1L);
        }

        @Test
        @DisplayName("POST /api/v1/products/{id}/deactivate - Should deactivate product")
        void shouldDeactivateProduct() throws Exception {
            // Given
            when(productService.deactivateProduct(1L)).thenReturn(productDTO);

            // When/Then
            mockMvc.perform(post("/api/v1/products/1/deactivate"))
                    .andExpect(status().isOk());

            verify(productService).deactivateProduct(1L);
        }
    }

    @Nested
    @DisplayName("Business Calculations Endpoints Tests")
    class BusinessCalculationsEndpointsTests {

        @Test
        @DisplayName("GET /api/v1/products/{id}/discounted-price - Should calculate discount")
        void shouldCalculateDiscountedPrice() throws Exception {
            // Given
            when(productService.calculateDiscountedPrice(1L, new BigDecimal("20")))
                    .thenReturn(new BigDecimal("79.99"));

            // When/Then
            mockMvc.perform(get("/api/v1/products/1/discounted-price")
                            .param("discountPercentage", "20"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("79.99"));
        }

        @Test
        @DisplayName("GET /api/v1/products/total-value - Should calculate total value")
        void shouldCalculateTotalValue() throws Exception {
            // Given
            when(productService.calculateTotalValue())
                    .thenReturn(new BigDecimal("10000.00"));

            // When/Then
            mockMvc.perform(get("/api/v1/products/total-value"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("10000.00"));
        }
    }
}
