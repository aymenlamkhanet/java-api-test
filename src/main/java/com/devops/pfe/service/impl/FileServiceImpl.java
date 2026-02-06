package com.devops.pfe.service.impl;

import com.devops.pfe.dto.FileDTO;
import com.devops.pfe.entity.FileEntity;
import com.devops.pfe.exception.ResourceNotFoundException;
import com.devops.pfe.repository.FileRepository;
import com.devops.pfe.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * Implémentation du service de gestion des fichiers.
 * Les fichiers sont stockés en base de données (BLOB).
 */
@Service
@Transactional
public class FileServiceImpl implements FileService {

    private final FileRepository fileRepository;

    // Taille maximale: 10 MB
    private static final long MAX_FILE_SIZE = 10L * 1024L * 1024L;

    @Autowired
    public FileServiceImpl(FileRepository fileRepository) {
        this.fileRepository = fileRepository;
    }

    @Override
    public FileDTO uploadFile(MultipartFile file, String description) {
        // Validation: fichier non vide
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Le fichier est vide");
        }

        // Validation: taille maximale
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Le fichier dépasse la taille maximale de 10 MB");
        }

        // Validation: type de contenu (optionnel - peut être désactivé)
        String contentType = file.getContentType();
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        try {
            // Créer l'entité fichier
            FileEntity fileEntity = new FileEntity();
            fileEntity.setFileName(file.getOriginalFilename());
            fileEntity.setContentType(contentType);
            fileEntity.setFileSize(file.getSize());
            fileEntity.setContent(file.getBytes());
            fileEntity.setDescription(description);

            // Sauvegarder en base
            FileEntity saved = fileRepository.save(fileEntity);

            // Retourner les métadonnées
            return mapToDTO(saved);

        } catch (IOException e) {
            throw new IllegalStateException("Erreur lors de la lecture du fichier: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public FileEntity downloadFile(Long id) {
        return fileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fichier non trouvé avec l'ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public FileDTO getFileMetadata(Long id) {
        FileEntity file = fileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fichier non trouvé avec l'ID: " + id));
        return mapToDTO(file);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FileDTO> getAllFiles() {
        return fileRepository.findAll().stream()
                .map(this::mapToDTO)
                .toList();
    }

    @Override
    public void deleteFile(Long id) {
        if (!fileRepository.existsById(id)) {
            throw new ResourceNotFoundException("Fichier non trouvé avec l'ID: " + id);
        }
        fileRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FileDTO> searchFiles(String keyword) {
        return fileRepository.findByFileNameContainingIgnoreCase(keyword).stream()
                .map(this::mapToDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public StorageStats getStorageStats() {
        long totalFiles = fileRepository.countAllFiles();
        long totalSize = fileRepository.getTotalStorageUsed();
        String formatted = formatFileSize(totalSize);
        return new StorageStats(totalFiles, totalSize, formatted);
    }

    /**
     * Convertit une entité FileEntity en FileDTO.
     */
    private FileDTO mapToDTO(FileEntity entity) {
        return new FileDTO(
                entity.getId(),
                entity.getFileName(),
                entity.getContentType(),
                entity.getFileSize(),
                entity.getUploadedAt(),
                entity.getDescription()
        );
    }

    /**
     * Formate la taille du fichier en format lisible.
     */
    private String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
        }
    }
}
