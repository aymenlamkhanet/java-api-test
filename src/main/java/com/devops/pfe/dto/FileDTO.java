package com.devops.pfe.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO pour les métadonnées de fichier (sans le contenu binaire).
 * Utilisé pour les réponses API afin d'éviter de transférer le contenu inutilement.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileDTO {

    private Long id;
    private String fileName;
    private String contentType;
    private Long fileSize;
    private LocalDateTime uploadedAt;
    private String description;
    private String downloadUrl;

    /**
     * Constructeur avec génération automatique de l'URL de téléchargement.
     */
    public FileDTO(Long id, String fileName, String contentType, Long fileSize, 
                   LocalDateTime uploadedAt, String description) {
        this.id = id;
        this.fileName = fileName;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.uploadedAt = uploadedAt;
        this.description = description;
        this.downloadUrl = "/api/v1/files/" + id + "/download";
    }
}
