package com.devops.pfe.service;

import com.devops.pfe.dto.ProductDTO;
import com.devops.pfe.entity.Product;
import com.devops.pfe.exception.DuplicateResourceException;
import com.devops.pfe.exception.InsufficientStockException;
import com.devops.pfe.exception.ResourceNotFoundException;
import com.devops.pfe.mapper.ProductMapper;
import com.devops.pfe.repository.ProductRepository;
import com.devops.pfe.service.impl.ProductServiceImpl;
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
@DisplayName("ProductService Tests")
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private ProductServiceImpl productService;

    private Product product;
    private ProductDTO productDTO;

    @BeforeEach
    void setUp() {
        product = Product.builder()
                .id(1L)
                .name("Test Product")
                .description("Test Description")
                .price(new BigDecimal("99.99"))
                .stockQuantity(100)
                .category("Electronics")
                .sku("TEST-SKU-001")
                .active(true)
                .build();

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
    @DisplayName("CRUD Operations Tests")
    class CrudOperationsTests {

        @Test
        @DisplayName("Should create product successfully")
        void shouldCreateProductSuccessfully() {
            // Given
            when(productRepository.existsBySku(productDTO.getSku())).thenReturn(false);
            when(productMapper.toEntity(productDTO)).thenReturn(product);
            when(productRepository.save(product)).thenReturn(product);
            when(productMapper.toDTO(product)).thenReturn(productDTO);

            // When
            ProductDTO result = productService.createProduct(productDTO);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("Test Product");
            assertThat(result.getSku()).isEqualTo("TEST-SKU-001");
            verify(productRepository).save(product);
        }

        @Test
        @DisplayName("Should throw exception when SKU already exists")
        void shouldThrowExceptionWhenSkuExists() {
            // Given
            when(productRepository.existsBySku(productDTO.getSku())).thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> productService.createProduct(productDTO))
                    .isInstanceOf(DuplicateResourceException.class);
            verify(productRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should get product by ID")
        void shouldGetProductById() {
            // Given
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
            when(productMapper.toDTO(product)).thenReturn(productDTO);

            // When
            ProductDTO result = productService.getProductById(1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Should throw exception when product not found")
        void shouldThrowExceptionWhenProductNotFound() {
            // Given
            when(productRepository.findById(999L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> productService.getProductById(999L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Should get all products")
        void shouldGetAllProducts() {
            // Given
            List<Product> products = Arrays.asList(product, product);
            when(productRepository.findAll()).thenReturn(products);
            when(productMapper.toDTO(any(Product.class))).thenReturn(productDTO);

            // When
            List<ProductDTO> result = productService.getAllProducts();

            // Then
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("Should update product successfully")
        void shouldUpdateProductSuccessfully() {
            // Given
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
            when(productRepository.save(product)).thenReturn(product);
            when(productMapper.toDTO(product)).thenReturn(productDTO);

            // When
            ProductDTO result = productService.updateProduct(1L, productDTO);

            // Then
            assertThat(result).isNotNull();
            verify(productMapper).updateEntityFromDTO(productDTO, product);
            verify(productRepository).save(product);
        }

        @Test
        @DisplayName("Should delete product successfully")
        void shouldDeleteProductSuccessfully() {
            // Given
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));

            // When
            productService.deleteProduct(1L);

            // Then
            verify(productRepository).delete(product);
        }
    }

    @Nested
    @DisplayName("Stock Management Tests")
    class StockManagementTests {

        @Test
        @DisplayName("Should update stock successfully")
        void shouldUpdateStockSuccessfully() {
            // Given
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
            when(productRepository.save(product)).thenReturn(product);
            when(productMapper.toDTO(product)).thenReturn(productDTO);

            // When
            ProductDTO result = productService.updateStock(1L, 50);

            // Then
            assertThat(product.getStockQuantity()).isEqualTo(50);
            verify(productRepository).save(product);
        }

        @Test
        @DisplayName("Should throw exception for negative stock quantity")
        void shouldThrowExceptionForNegativeStock() {
            // When/Then
            assertThatThrownBy(() -> productService.updateStock(1L, -10))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("négative");
        }

        @Test
        @DisplayName("Should add stock successfully")
        void shouldAddStockSuccessfully() {
            // Given
            product.setStockQuantity(100);
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
            when(productRepository.save(product)).thenReturn(product);
            when(productMapper.toDTO(product)).thenReturn(productDTO);

            // When
            productService.addStock(1L, 50);

            // Then
            assertThat(product.getStockQuantity()).isEqualTo(150);
        }

        @Test
        @DisplayName("Should remove stock successfully")
        void shouldRemoveStockSuccessfully() {
            // Given
            product.setStockQuantity(100);
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
            when(productRepository.save(product)).thenReturn(product);
            when(productMapper.toDTO(product)).thenReturn(productDTO);

            // When
            productService.removeStock(1L, 30);

            // Then
            assertThat(product.getStockQuantity()).isEqualTo(70);
        }

        @Test
        @DisplayName("Should throw exception when insufficient stock")
        void shouldThrowExceptionWhenInsufficientStock() {
            // Given
            product.setStockQuantity(10);
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));

            // When/Then
            assertThatThrownBy(() -> productService.removeStock(1L, 50))
                    .isInstanceOf(InsufficientStockException.class);
        }

        @Test
        @DisplayName("Should check stock availability correctly")
        void shouldCheckStockAvailabilityCorrectly() {
            // Given
            product.setStockQuantity(100);
            product.setActive(true);
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));

            // When
            boolean result = productService.checkStockAvailability(1L, 50);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false when product inactive")
        void shouldReturnFalseWhenProductInactive() {
            // Given
            product.setStockQuantity(100);
            product.setActive(false);
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));

            // When
            boolean result = productService.checkStockAvailability(1L, 50);

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("Business Calculations Tests")
    class BusinessCalculationsTests {

        @Test
        @DisplayName("Should calculate discounted price correctly")
        void shouldCalculateDiscountedPriceCorrectly() {
            // Given
            product.setPrice(new BigDecimal("100.00"));
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));

            // When
            BigDecimal result = productService.calculateDiscountedPrice(1L, new BigDecimal("20"));

            // Then
            assertThat(result).isEqualByComparingTo(new BigDecimal("80.00"));
        }

        @Test
        @DisplayName("Should throw exception for invalid discount percentage")
        void shouldThrowExceptionForInvalidDiscountPercentage() {
            // When/Then
            assertThatThrownBy(() -> productService.calculateDiscountedPrice(1L, new BigDecimal("150")))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Should calculate total stock value")
        void shouldCalculateTotalStockValue() {
            // Given
            Product product1 = Product.builder()
                    .price(new BigDecimal("10.00"))
                    .stockQuantity(100)
                    .active(true)
                    .build();
            Product product2 = Product.builder()
                    .price(new BigDecimal("20.00"))
                    .stockQuantity(50)
                    .active(true)
                    .build();

            when(productRepository.findByActiveTrue()).thenReturn(Arrays.asList(product1, product2));

            // When
            BigDecimal result = productService.calculateTotalValue();

            // Then
            assertThat(result).isEqualByComparingTo(new BigDecimal("2000.00")); // (10*100) + (20*50)
        }
    }

    @Nested
    @DisplayName("Product Status Tests")
    class ProductStatusTests {

        @Test
        @DisplayName("Should activate product")
        void shouldActivateProduct() {
            // Given
            product.setActive(false);
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
            when(productRepository.save(product)).thenReturn(product);
            when(productMapper.toDTO(product)).thenReturn(productDTO);

            // When
            productService.activateProduct(1L);

            // Then
            assertThat(product.getActive()).isTrue();
            verify(productRepository).save(product);
        }

        @Test
        @DisplayName("Should deactivate product")
        void shouldDeactivateProduct() {
            // Given
            product.setActive(true);
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
            when(productRepository.save(product)).thenReturn(product);
            when(productMapper.toDTO(product)).thenReturn(productDTO);

            // When
            productService.deactivateProduct(1L);

            // Then
            assertThat(product.getActive()).isFalse();
        }
    }

    @Nested
    @DisplayName("Search & Filter Tests")
    class SearchFilterTests {

        @Test
        @DisplayName("Should get products by category")
        void shouldGetProductsByCategory() {
            // Given
            when(productRepository.findByCategory("Electronics")).thenReturn(Arrays.asList(product));
            when(productMapper.toDTO(product)).thenReturn(productDTO);

            // When
            List<ProductDTO> result = productService.getProductsByCategory("Electronics");

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getCategory()).isEqualTo("Electronics");
        }

        @Test
        @DisplayName("Should search products by keyword")
        void shouldSearchProductsByKeyword() {
            // Given
            when(productRepository.searchByKeyword("Test")).thenReturn(Arrays.asList(product));
            when(productMapper.toDTO(product)).thenReturn(productDTO);

            // When
            List<ProductDTO> result = productService.searchProducts("Test");

            // Then
            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Should get products by price range")
        void shouldGetProductsByPriceRange() {
            // Given
            BigDecimal minPrice = new BigDecimal("50.00");
            BigDecimal maxPrice = new BigDecimal("150.00");
            when(productRepository.findByPriceRange(minPrice, maxPrice)).thenReturn(Arrays.asList(product));
            when(productMapper.toDTO(product)).thenReturn(productDTO);

            // When
            List<ProductDTO> result = productService.getProductsByPriceRange(minPrice, maxPrice);

            // Then
            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Should get low stock products")
        void shouldGetLowStockProducts() {
            // Given
            product.setStockQuantity(5);
            when(productRepository.findLowStockProducts(10)).thenReturn(Arrays.asList(product));
            when(productMapper.toDTO(product)).thenReturn(productDTO);

            // When
            List<ProductDTO> result = productService.getLowStockProducts(10);

            // Then
            assertThat(result).hasSize(1);
        }
    }
}
