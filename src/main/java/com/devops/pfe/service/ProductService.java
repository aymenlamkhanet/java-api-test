package com.devops.pfe.service;

import com.devops.pfe.dto.ProductDTO;
import java.math.BigDecimal;
import java.util.List;

public interface ProductService {
    
    // CRUD Operations
    ProductDTO createProduct(ProductDTO productDTO);
    ProductDTO getProductById(Long id);
    ProductDTO getProductBySku(String sku);
    List<ProductDTO> getAllProducts();
    List<ProductDTO> getActiveProducts();
    ProductDTO updateProduct(Long id, ProductDTO productDTO);
    void deleteProduct(Long id);
    
    // Business Logic Operations
    List<ProductDTO> getProductsByCategory(String category);
    List<ProductDTO> searchProducts(String keyword);
    List<ProductDTO> getProductsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice);
    List<ProductDTO> getLowStockProducts(Integer threshold);
    List<String> getAllCategories();
    
    // Stock Management
    ProductDTO updateStock(Long id, Integer quantity);
    ProductDTO addStock(Long id, Integer quantity);
    ProductDTO removeStock(Long id, Integer quantity);
    boolean checkStockAvailability(Long productId, Integer quantity);
    
    // Product Status
    ProductDTO activateProduct(Long id);
    ProductDTO deactivateProduct(Long id);
    
    // Business Calculations
    BigDecimal calculateDiscountedPrice(Long productId, BigDecimal discountPercentage);
    BigDecimal calculateTotalValue();
}
