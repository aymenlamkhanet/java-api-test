package com.devops.pfe.repository;

import com.devops.pfe.entity.FileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository pour l'accès aux fichiers stockés en base de données.
 */
@Repository
public interface FileRepository extends JpaRepository<FileEntity, Long> {

    /**
     * Trouve tous les fichiers par type de contenu.
     */
    List<FileEntity> findByContentType(String contentType);

    /**
     * Trouve tous les fichiers dont le nom contient le mot-clé.
     */
    List<FileEntity> findByFileNameContainingIgnoreCase(String keyword);

    /**
     * Trouve tous les fichiers PDF.
     */
    @Query("SELECT f FROM FileEntity f WHERE f.contentType = 'application/pdf'")
    List<FileEntity> findAllPdfFiles();

    /**
     * Vérifie si un fichier avec ce nom existe déjà.
     */
    boolean existsByFileName(String fileName);

    /**
     * Compte le nombre total de fichiers.
     */
    @Query("SELECT COUNT(f) FROM FileEntity f")
    long countAllFiles();

    /**
     * Calcule la taille totale de tous les fichiers.
     */
    @Query("SELECT COALESCE(SUM(f.fileSize), 0) FROM FileEntity f")
    long getTotalStorageUsed();
}
