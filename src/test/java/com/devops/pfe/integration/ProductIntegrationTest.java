package com.devops.pfe.integration;

import com.devops.pfe.dto.ProductDTO;
import com.devops.pfe.entity.Product;
import com.devops.pfe.repository.ProductRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@DisplayName("Product Integration Tests")
class ProductIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    private ProductDTO productDTO;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
        
        productDTO = ProductDTO.builder()
                .name("Integration Test Product")
                .description("Product for integration testing")
                .price(new BigDecimal("149.99"))
                .stockQuantity(75)
                .category("Testing")
                .sku("INT-TEST-001")
                .active(true)
                .build();
    }

    @Test
    @DisplayName("Should create and retrieve product - Full flow")
    void shouldCreateAndRetrieveProduct() throws Exception {
        // Create product
        String response = mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Integration Test Product")))
                .andReturn().getResponse().getContentAsString();

        ProductDTO created = objectMapper.readValue(response, ProductDTO.class);

        // Verify in database
        assertThat(productRepository.findById(created.getId())).isPresent();

        // Retrieve by ID
        mockMvc.perform(get("/api/v1/products/" + created.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku", is("INT-TEST-001")));
    }

    @Test
    @DisplayName("Should update product - Full flow")
    void shouldUpdateProduct() throws Exception {
        // First create a product
        Product product = Product.builder()
                .name("Original Product")
                .description("Original description")
                .price(new BigDecimal("100.00"))
                .stockQuantity(50)
                .category("Original")
                .sku("ORIG-001")
                .active(true)
                .build();
        Product saved = productRepository.save(product);

        // Update the product
        ProductDTO updateDTO = ProductDTO.builder()
                .name("Updated Product")
                .description("Updated description")
                .price(new BigDecimal("150.00"))
                .stockQuantity(100)
                .category("Updated")
                .sku("ORIG-001")
                .active(true)
                .build();

        mockMvc.perform(put("/api/v1/products/" + saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Updated Product")));

        // Verify in database
        Product updated = productRepository.findById(saved.getId()).orElseThrow();
        assertThat(updated.getName()).isEqualTo("Updated Product");
        assertThat(updated.getPrice()).isEqualByComparingTo(new BigDecimal("150.00"));
    }

    @Test
    @DisplayName("Should manage stock - Full flow")
    void shouldManageStock() throws Exception {
        // Create product
        Product product = Product.builder()
                .name("Stock Test Product")
                .price(new BigDecimal("50.00"))
                .stockQuantity(100)
                .category("Stock Test")
                .sku("STOCK-001")
                .active(true)
                .build();
        Product saved = productRepository.save(product);

        // Add stock
        mockMvc.perform(post("/api/v1/products/" + saved.getId() + "/stock/add")
                        .param("quantity", "50"))
                .andExpect(status().isOk());

        Product afterAdd = productRepository.findById(saved.getId()).orElseThrow();
        assertThat(afterAdd.getStockQuantity()).isEqualTo(150);

        // Remove stock
        mockMvc.perform(post("/api/v1/products/" + saved.getId() + "/stock/remove")
                        .param("quantity", "30"))
                .andExpect(status().isOk());

        Product afterRemove = productRepository.findById(saved.getId()).orElseThrow();
        assertThat(afterRemove.getStockQuantity()).isEqualTo(120);
    }

    @Test
    @DisplayName("Should search and filter products")
    void shouldSearchAndFilterProducts() throws Exception {
        // Create multiple products
        Product electronics1 = Product.builder()
                .name("Smartphone")
                .description("Latest smartphone")
                .price(new BigDecimal("800.00"))
                .stockQuantity(30)
                .category("Electronics")
                .sku("PHONE-001")
                .active(true)
                .build();

        Product electronics2 = Product.builder()
                .name("Tablet")
                .description("Tablet device")
                .price(new BigDecimal("500.00"))
                .stockQuantity(20)
                .category("Electronics")
                .sku("TABLET-001")
                .active(true)
                .build();

        Product furniture = Product.builder()
                .name("Chair")
                .description("Office chair")
                .price(new BigDecimal("200.00"))
                .stockQuantity(5)
                .category("Furniture")
                .sku("CHAIR-001")
                .active(true)
                .build();

        productRepository.save(electronics1);
        productRepository.save(electronics2);
        productRepository.save(furniture);

        // Search by category
        mockMvc.perform(get("/api/v1/products/category/Electronics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        // Search by keyword
        mockMvc.perform(get("/api/v1/products/search")
                        .param("keyword", "phone"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("Smartphone")));

        // Low stock products
        mockMvc.perform(get("/api/v1/products/low-stock")
                        .param("threshold", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("Chair")));
    }

    @Test
    @DisplayName("Should delete product")
    void shouldDeleteProduct() throws Exception {
        // Create product
        Product product = Product.builder()
                .name("Delete Test")
                .price(new BigDecimal("10.00"))
                .stockQuantity(10)
                .category("Test")
                .sku("DEL-001")
                .active(true)
                .build();
        Product saved = productRepository.save(product);

        // Delete
        mockMvc.perform(delete("/api/v1/products/" + saved.getId()))
                .andExpect(status().isNoContent());

        // Verify deleted
        assertThat(productRepository.findById(saved.getId())).isEmpty();
    }

    @Test
    @DisplayName("Should handle validation errors")
    void shouldHandleValidationErrors() throws Exception {
        ProductDTO invalidProduct = ProductDTO.builder()
                .name("") // Invalid
                .price(new BigDecimal("-10.00")) // Invalid
                .stockQuantity(-5) // Invalid
                .category("") // Invalid
                .build();

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidProduct)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors").exists());
    }
}
