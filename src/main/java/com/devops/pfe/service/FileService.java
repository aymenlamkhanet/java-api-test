package com.devops.pfe.service;

import com.devops.pfe.dto.FileDTO;
import com.devops.pfe.entity.FileEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Interface du service de gestion des fichiers.
 */
public interface FileService {

    /**
     * Upload un fichier et le stocke en base de données.
     * @param file Le fichier à uploader
     * @param description Description optionnelle du fichier
     * @return Les métadonnées du fichier uploadé
     */
    FileDTO uploadFile(MultipartFile file, String description);

    /**
     * Récupère un fichier par son ID (avec contenu binaire).
     * @param id L'ID du fichier
     * @return L'entité fichier complète
     */
    FileEntity downloadFile(Long id);

    /**
     * Récupère les métadonnées d'un fichier (sans contenu).
     * @param id L'ID du fichier
     * @return Les métadonnées du fichier
     */
    FileDTO getFileMetadata(Long id);

    /**
     * Liste tous les fichiers (métadonnées seulement).
     * @return Liste des métadonnées de tous les fichiers
     */
    List<FileDTO> getAllFiles();

    /**
     * Supprime un fichier par son ID.
     * @param id L'ID du fichier à supprimer
     */
    void deleteFile(Long id);

    /**
     * Recherche des fichiers par nom.
     * @param keyword Mot-clé de recherche
     * @return Liste des fichiers correspondants
     */
    List<FileDTO> searchFiles(String keyword);

    /**
     * Récupère les statistiques de stockage.
     * @return Nombre de fichiers et espace utilisé
     */
    StorageStats getStorageStats();

    /**
     * Classe interne pour les statistiques de stockage.
     */
    record StorageStats(long totalFiles, long totalSizeBytes, String totalSizeFormatted) {}
}
