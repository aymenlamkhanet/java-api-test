package com.devops.pfe.mapper;

import com.devops.pfe.dto.ProductDTO;
import com.devops.pfe.entity.Product;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

    public ProductDTO toDTO(Product product) {
        if (product == null) {
            return null;
        }
        
        return ProductDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stockQuantity(product.getStockQuantity())
                .category(product.getCategory())
                .sku(product.getSku())
                .active(product.getActive())
                .build();
    }

    public Product toEntity(ProductDTO dto) {
        if (dto == null) {
            return null;
        }
        
        return Product.builder()
                .id(dto.getId())
                .name(dto.getName())
                .description(dto.getDescription())
                .price(dto.getPrice())
                .stockQuantity(dto.getStockQuantity())
                .category(dto.getCategory())
                .sku(dto.getSku())
                .active(!Boolean.FALSE.equals(dto.getActive()))
                .build();
    }

    public void updateEntityFromDTO(ProductDTO dto, Product product) {
        if (dto == null || product == null) {
            return;
        }
        
        if (dto.getName() != null) {
            product.setName(dto.getName());
        }
        if (dto.getDescription() != null) {
            product.setDescription(dto.getDescription());
        }
        if (dto.getPrice() != null) {
            product.setPrice(dto.getPrice());
        }
        if (dto.getStockQuantity() != null) {
            product.setStockQuantity(dto.getStockQuantity());
        }
        if (dto.getCategory() != null) {
            product.setCategory(dto.getCategory());
        }
        if (dto.getSku() != null) {
            product.setSku(dto.getSku());
        }
        if (dto.getActive() != null) {
            product.setActive(dto.getActive());
        }
    }
}
