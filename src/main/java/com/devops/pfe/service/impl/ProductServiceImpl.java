package com.devops.pfe.service.impl;

import com.devops.pfe.dto.ProductDTO;
import com.devops.pfe.entity.Product;
import com.devops.pfe.exception.DuplicateResourceException;
import com.devops.pfe.exception.InsufficientStockException;
import com.devops.pfe.exception.ResourceNotFoundException;
import com.devops.pfe.mapper.ProductMapper;
import com.devops.pfe.repository.ProductRepository;
import com.devops.pfe.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    @Override
    public ProductDTO createProduct(ProductDTO productDTO) {
        log.info("Création d'un nouveau produit: {}", productDTO.getName());
        
        // Vérifier si le SKU existe déjà
        if (productDTO.getSku() != null && productRepository.existsBySku(productDTO.getSku())) {
            throw new DuplicateResourceException("Produit", "SKU", productDTO.getSku());
        }
        
        Product product = productMapper.toEntity(productDTO);
        Product savedProduct = productRepository.save(product);
        
        log.info("Produit créé avec succès. ID: {}", savedProduct.getId());
        return productMapper.toDTO(savedProduct);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDTO getProductById(Long id) {
        log.debug("Recherche du produit avec l'ID: {}", id);
        Product product = findProductById(id);
        return productMapper.toDTO(product);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDTO getProductBySku(String sku) {
        log.debug("Recherche du produit avec le SKU: {}", sku);
        Product product = productRepository.findBySku(sku)
                .orElseThrow(() -> new ResourceNotFoundException("Produit", "SKU", sku));
        return productMapper.toDTO(product);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductDTO> getAllProducts() {
        log.debug("Récupération de tous les produits");
        return productRepository.findAll().stream()
                .map(productMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductDTO> getActiveProducts() {
        log.debug("Récupération des produits actifs");
        return productRepository.findByActiveTrue().stream()
                .map(productMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public ProductDTO updateProduct(Long id, ProductDTO productDTO) {
        log.info("Mise à jour du produit avec l'ID: {}", id);
        
        Product existingProduct = findProductById(id);
        
        // Vérifier si le nouveau SKU n'est pas déjà utilisé par un autre produit
        if (productDTO.getSku() != null && !productDTO.getSku().equals(existingProduct.getSku())) {
            if (productRepository.existsBySku(productDTO.getSku())) {
                throw new DuplicateResourceException("Produit", "SKU", productDTO.getSku());
            }
        }
        
        productMapper.updateEntityFromDTO(productDTO, existingProduct);
        Product updatedProduct = productRepository.save(existingProduct);
        
        log.info("Produit mis à jour avec succès. ID: {}", id);
        return productMapper.toDTO(updatedProduct);
    }

    @Override
    public void deleteProduct(Long id) {
        log.info("Suppression du produit avec l'ID: {}", id);
        Product product = findProductById(id);
        productRepository.delete(product);
        log.info("Produit supprimé avec succès. ID: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductDTO> getProductsByCategory(String category) {
        log.debug("Recherche des produits par catégorie: {}", category);
        return productRepository.findByCategory(category).stream()
                .map(productMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductDTO> searchProducts(String keyword) {
        log.debug("Recherche de produits avec le mot-clé: {}", keyword);
        return productRepository.searchByKeyword(keyword).stream()
                .map(productMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductDTO> getProductsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        log.debug("Recherche des produits entre {} et {}", minPrice, maxPrice);
        return productRepository.findByPriceRange(minPrice, maxPrice).stream()
                .map(productMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductDTO> getLowStockProducts(Integer threshold) {
        log.debug("Recherche des produits avec stock bas (seuil: {})", threshold);
        return productRepository.findLowStockProducts(threshold).stream()
                .map(productMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getAllCategories() {
        log.debug("Récupération de toutes les catégories");
        return productRepository.findAllCategories();
    }

    @Override
    public ProductDTO updateStock(Long id, Integer quantity) {
        log.info("Mise à jour du stock du produit {} à {}", id, quantity);
        
        if (quantity < 0) {
            throw new IllegalArgumentException("La quantité ne peut pas être négative");
        }
        
        Product product = findProductById(id);
        product.setStockQuantity(quantity);
        Product updatedProduct = productRepository.save(product);
        
        log.info("Stock mis à jour avec succès pour le produit {}", id);
        return productMapper.toDTO(updatedProduct);
    }

    @Override
    public ProductDTO addStock(Long id, Integer quantity) {
        log.info("Ajout de {} unités au stock du produit {}", quantity, id);
        
        if (quantity <= 0) {
            throw new IllegalArgumentException("La quantité à ajouter doit être positive");
        }
        
        Product product = findProductById(id);
        product.setStockQuantity(product.getStockQuantity() + quantity);
        Product updatedProduct = productRepository.save(product);
        
        log.info("Stock ajouté avec succès. Nouveau stock: {}", updatedProduct.getStockQuantity());
        return productMapper.toDTO(updatedProduct);
    }

    @Override
    public ProductDTO removeStock(Long id, Integer quantity) {
        log.info("Retrait de {} unités du stock du produit {}", quantity, id);
        
        if (quantity <= 0) {
            throw new IllegalArgumentException("La quantité à retirer doit être positive");
        }
        
        Product product = findProductById(id);
        
        if (product.getStockQuantity() < quantity) {
            throw new InsufficientStockException(product.getName(), product.getStockQuantity(), quantity);
        }
        
        product.setStockQuantity(product.getStockQuantity() - quantity);
        Product updatedProduct = productRepository.save(product);
        
        log.info("Stock retiré avec succès. Nouveau stock: {}", updatedProduct.getStockQuantity());
        return productMapper.toDTO(updatedProduct);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean checkStockAvailability(Long productId, Integer quantity) {
        log.debug("Vérification de la disponibilité du stock pour le produit {} (quantité: {})", productId, quantity);
        Product product = findProductById(productId);
        return product.getStockQuantity() >= quantity && product.getActive();
    }

    @Override
    public ProductDTO activateProduct(Long id) {
        log.info("Activation du produit {}", id);
        Product product = findProductById(id);
        product.setActive(true);
        Product updatedProduct = productRepository.save(product);
        return productMapper.toDTO(updatedProduct);
    }

    @Override
    public ProductDTO deactivateProduct(Long id) {
        log.info("Désactivation du produit {}", id);
        Product product = findProductById(id);
        product.setActive(false);
        Product updatedProduct = productRepository.save(product);
        return productMapper.toDTO(updatedProduct);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateDiscountedPrice(Long productId, BigDecimal discountPercentage) {
        log.debug("Calcul du prix remisé pour le produit {} avec {}% de réduction", productId, discountPercentage);
        
        if (discountPercentage.compareTo(BigDecimal.ZERO) < 0 || discountPercentage.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("Le pourcentage de remise doit être entre 0 et 100");
        }
        
        Product product = findProductById(productId);
        BigDecimal discountMultiplier = BigDecimal.ONE.subtract(discountPercentage.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
        
        return product.getPrice().multiply(discountMultiplier).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateTotalValue() {
        log.debug("Calcul de la valeur totale du stock");
        return productRepository.findByActiveTrue().stream()
                .map(p -> p.getPrice().multiply(BigDecimal.valueOf(p.getStockQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Product findProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produit", id));
    }
}
