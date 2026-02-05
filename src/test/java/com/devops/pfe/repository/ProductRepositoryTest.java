package com.devops.pfe.repository;

import com.devops.pfe.entity.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("ProductRepository Tests")
class ProductRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ProductRepository productRepository;

    private Product product1;
    private Product product2;
    private Product product3;

    @BeforeEach
    void setUp() {
        product1 = Product.builder()
                .name("Laptop")
                .description("High-end laptop")
                .price(new BigDecimal("1200.00"))
                .stockQuantity(50)
                .category("Electronics")
                .sku("LAPTOP-001")
                .active(true)
                .build();

        product2 = Product.builder()
                .name("Mouse")
                .description("Wireless mouse")
                .price(new BigDecimal("25.00"))
                .stockQuantity(200)
                .category("Electronics")
                .sku("MOUSE-001")
                .active(true)
                .build();

        product3 = Product.builder()
                .name("Desk")
                .description("Office desk")
                .price(new BigDecimal("300.00"))
                .stockQuantity(5)
                .category("Furniture")
                .sku("DESK-001")
                .active(true)
                .build();

        // Product 4 - inactive product for testing inactive filter
        Product product4 = Product.builder()
                .name("Old Chair")
                .description("Discontinued chair")
                .price(new BigDecimal("150.00"))
                .stockQuantity(0)
                .category("Furniture")
                .sku("CHAIR-OLD")
                .active(false)
                .build();

        entityManager.persist(product1);
        entityManager.persist(product2);
        entityManager.persist(product3);
        entityManager.persist(product4);
        entityManager.flush();
    }

    @Test
    @DisplayName("Should find product by SKU")
    void shouldFindProductBySku() {
        Optional<Product> found = productRepository.findBySku("LAPTOP-001");

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Laptop");
    }

    @Test
    @DisplayName("Should return empty when SKU not found")
    void shouldReturnEmptyWhenSkuNotFound() {
        Optional<Product> found = productRepository.findBySku("NONEXISTENT");

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should find products by category")
    void shouldFindProductsByCategory() {
        List<Product> electronics = productRepository.findByCategory("Electronics");

        assertThat(electronics).hasSize(2);
        assertThat(electronics).extracting(Product::getName)
                .containsExactlyInAnyOrder("Laptop", "Mouse");
    }

    @Test
    @DisplayName("Should find only active products")
    void shouldFindOnlyActiveProducts() {
        List<Product> activeProducts = productRepository.findByActiveTrue();

        assertThat(activeProducts).hasSize(3);
        assertThat(activeProducts).allMatch(Product::getActive);
    }

    @Test
    @DisplayName("Should find only inactive products")
    void shouldFindOnlyInactiveProducts() {
        List<Product> inactiveProducts = productRepository.findByActiveFalse();

        assertThat(inactiveProducts).hasSize(1);
        assertThat(inactiveProducts.get(0).getName()).isEqualTo("Old Chair");
    }

    @Test
    @DisplayName("Should find low stock products")
    void shouldFindLowStockProducts() {
        List<Product> lowStock = productRepository.findLowStockProducts(10);

        assertThat(lowStock).hasSize(1);
        assertThat(lowStock.get(0).getName()).isEqualTo("Desk");
    }

    @Test
    @DisplayName("Should find products by price range")
    void shouldFindProductsByPriceRange() {
        List<Product> products = productRepository.findByPriceRange(
                new BigDecimal("20.00"),
                new BigDecimal("500.00")
        );

        assertThat(products).hasSize(2); // Mouse (25.00) and Desk (300.00) are active and in range
    }

    @Test
    @DisplayName("Should search products by keyword in name")
    void shouldSearchProductsByKeywordInName() {
        List<Product> products = productRepository.searchByKeyword("Laptop");

        assertThat(products).hasSize(1);
        assertThat(products.get(0).getName()).isEqualTo("Laptop");
    }

    @Test
    @DisplayName("Should search products by keyword in description")
    void shouldSearchProductsByKeywordInDescription() {
        List<Product> products = productRepository.searchByKeyword("wireless");

        assertThat(products).hasSize(1);
        assertThat(products.get(0).getName()).isEqualTo("Mouse");
    }

    @Test
    @DisplayName("Should find all distinct categories")
    void shouldFindAllDistinctCategories() {
        List<String> categories = productRepository.findAllCategories();

        assertThat(categories).hasSize(2); // Electronics and Furniture (Desk is now active)
        assertThat(categories).containsExactlyInAnyOrder("Electronics", "Furniture");
    }

    @Test
    @DisplayName("Should check if SKU exists")
    void shouldCheckIfSkuExists() {
        boolean exists = productRepository.existsBySku("LAPTOP-001");
        boolean notExists = productRepository.existsBySku("NONEXISTENT");

        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }
}
