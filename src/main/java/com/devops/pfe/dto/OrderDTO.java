package com.devops.pfe.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDTO {

    private Long id;

    private String orderNumber;

    @NotBlank(message = "Le nom du client est obligatoire")
    private String customerName;

    @Email(message = "L'email doit être valide")
    @NotBlank(message = "L'email est obligatoire")
    private String customerEmail;

    private String status;

    @NotEmpty(message = "La commande doit contenir au moins un article")
    private List<OrderItemDTO> items;
}
