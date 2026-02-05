package com.devops.pfe.controller;

import com.devops.pfe.dto.ProductDTO;
import com.devops.pfe.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    // ==================== CRUD Operations ====================

    @PostMapping
    public ResponseEntity<ProductDTO> createProduct(@Valid @RequestBody ProductDTO productDTO) {
        ProductDTO createdProduct = productService.createProduct(productDTO);
        return new ResponseEntity<>(createdProduct, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDTO> getProductById(@PathVariable Long id) {
        ProductDTO product = productService.getProductById(id);
        return ResponseEntity.ok(product);
    }

    @GetMapping("/sku/{sku}")
    public ResponseEntity<ProductDTO> getProductBySku(@PathVariable String sku) {
        ProductDTO product = productService.getProductBySku(sku);
        return ResponseEntity.ok(product);
    }

    @GetMapping
    public ResponseEntity<List<ProductDTO>> getAllProducts() {
        List<ProductDTO> products = productService.getAllProducts();
        return ResponseEntity.ok(products);
    }

    @GetMapping("/active")
    public ResponseEntity<List<ProductDTO>> getActiveProducts() {
        List<ProductDTO> products = productService.getActiveProducts();
        return ResponseEntity.ok(products);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductDTO> updateProduct(@PathVariable Long id, @Valid @RequestBody ProductDTO productDTO) {
        ProductDTO updatedProduct = productService.updateProduct(id, productDTO);
        return ResponseEntity.ok(updatedProduct);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== Search & Filter Operations ====================

    @GetMapping("/category/{category}")
    public ResponseEntity<List<ProductDTO>> getProductsByCategory(@PathVariable String category) {
        List<ProductDTO> products = productService.getProductsByCategory(category);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/search")
    public ResponseEntity<List<ProductDTO>> searchProducts(@RequestParam String keyword) {
        List<ProductDTO> products = productService.searchProducts(keyword);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/price-range")
    public ResponseEntity<List<ProductDTO>> getProductsByPriceRange(
            @RequestParam BigDecimal minPrice,
            @RequestParam BigDecimal maxPrice) {
        List<ProductDTO> products = productService.getProductsByPriceRange(minPrice, maxPrice);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/low-stock")
    public ResponseEntity<List<ProductDTO>> getLowStockProducts(
            @RequestParam(defaultValue = "10") Integer threshold) {
        List<ProductDTO> products = productService.getLowStockProducts(threshold);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/categories")
    public ResponseEntity<List<String>> getAllCategories() {
        List<String> categories = productService.getAllCategories();
        return ResponseEntity.ok(categories);
    }

    // ==================== Stock Management ====================

    @PatchMapping("/{id}/stock")
    public ResponseEntity<ProductDTO> updateStock(
            @PathVariable Long id,
            @RequestParam Integer quantity) {
        ProductDTO product = productService.updateStock(id, quantity);
        return ResponseEntity.ok(product);
    }

    @PostMapping("/{id}/stock/add")
    public ResponseEntity<ProductDTO> addStock(
            @PathVariable Long id,
            @RequestParam Integer quantity) {
        ProductDTO product = productService.addStock(id, quantity);
        return ResponseEntity.ok(product);
    }

    @PostMapping("/{id}/stock/remove")
    public ResponseEntity<ProductDTO> removeStock(
            @PathVariable Long id,
            @RequestParam Integer quantity) {
        ProductDTO product = productService.removeStock(id, quantity);
        return ResponseEntity.ok(product);
    }

    @GetMapping("/{id}/stock/check")
    public ResponseEntity<Boolean> checkStockAvailability(
            @PathVariable Long id,
            @RequestParam Integer quantity) {
        boolean isAvailable = productService.checkStockAvailability(id, quantity);
        return ResponseEntity.ok(isAvailable);
    }

    // ==================== Product Status ====================

    @PostMapping("/{id}/activate")
    public ResponseEntity<ProductDTO> activateProduct(@PathVariable Long id) {
        ProductDTO product = productService.activateProduct(id);
        return ResponseEntity.ok(product);
    }

    @PostMapping("/{id}/deactivate")
    public ResponseEntity<ProductDTO> deactivateProduct(@PathVariable Long id) {
        ProductDTO product = productService.deactivateProduct(id);
        return ResponseEntity.ok(product);
    }

    // ==================== Business Calculations ====================

    @GetMapping("/{id}/discounted-price")
    public ResponseEntity<BigDecimal> calculateDiscountedPrice(
            @PathVariable Long id,
            @RequestParam BigDecimal discountPercentage) {
        BigDecimal discountedPrice = productService.calculateDiscountedPrice(id, discountPercentage);
        return ResponseEntity.ok(discountedPrice);
    }

    @GetMapping("/total-value")
    public ResponseEntity<BigDecimal> calculateTotalValue() {
        BigDecimal totalValue = productService.calculateTotalValue();
        return ResponseEntity.ok(totalValue);
    }
}
