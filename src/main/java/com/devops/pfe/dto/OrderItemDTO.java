package com.devops.pfe.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemDTO {

    private Long id;

    @NotNull(message = "L'ID du produit est obligatoire")
    private Long productId;

    private String productName;

    @NotNull(message = "La quantité est obligatoire")
    @Min(value = 1, message = "La quantité doit être au moins 1")
    private Integer quantity;

    private BigDecimal unitPrice;

    private BigDecimal totalPrice;
}
