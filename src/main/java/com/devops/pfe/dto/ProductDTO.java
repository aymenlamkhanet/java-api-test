package com.devops.pfe.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDTO {

    private Long id;

    @NotBlank(message = "Le nom du produit est obligatoire")
    @Size(min = 2, max = 100, message = "Le nom doit contenir entre 2 et 100 caractères")
    private String name;

    @Size(max = 500, message = "La description ne peut pas dépasser 500 caractères")
    private String description;

    @NotNull(message = "Le prix est obligatoire")
    @DecimalMin(value = "0.01", message = "Le prix doit être supérieur à 0")
    private BigDecimal price;

    @NotNull(message = "La quantité en stock est obligatoire")
    @Min(value = 0, message = "La quantité ne peut pas être négative")
    private Integer stockQuantity;

    @NotBlank(message = "La catégorie est obligatoire")
    private String category;

    private String sku;

    private Boolean active;
}
