package com.devops.pfe.controller;

import com.devops.pfe.dto.FileDTO;
import com.devops.pfe.entity.FileEntity;
import com.devops.pfe.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * Contrôleur REST pour la gestion des fichiers (upload/download).
 * 
 * Endpoints:
 * - POST   /api/v1/files/upload         → Upload un fichier
 * - GET    /api/v1/files/{id}/download  → Télécharger un fichier
 * - GET    /api/v1/files/{id}           → Métadonnées d'un fichier
 * - GET    /api/v1/files                → Liste tous les fichiers
 * - DELETE /api/v1/files/{id}           → Supprimer un fichier
 * - GET    /api/v1/files/search         → Rechercher des fichiers
 * - GET    /api/v1/files/stats          → Statistiques de stockage
 */
@RestController
@RequestMapping("/api/v1/files")
@CrossOrigin(origins = "${cors.allowed-origins:http://localhost:3000,http://localhost:4200}")
public class FileController {

    private final FileService fileService;

    @Autowired
    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    /**
     * Upload un fichier.
     * 
     * @param file        Le fichier à uploader (multipart/form-data)
     * @param description Description optionnelle du fichier
     * @return Les métadonnées du fichier uploadé
     * 
     * Exemple curl:
     * curl -X POST http://localhost:8080/api/v1/files/upload \
     *      -F "file=@document.pdf" \
     *      -F "description=Mon document PDF"
     */
    @PostMapping("/upload")
    public ResponseEntity<FileDTO> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "description", required = false) String description) {
        
        FileDTO savedFile = fileService.uploadFile(file, description);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedFile);
    }

    /**
     * Télécharge un fichier par son ID.
     * 
     * @param id L'ID du fichier
     * @return Le contenu binaire du fichier avec les headers appropriés
     */
    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> downloadFile(@PathVariable Long id) {
        FileEntity file = fileService.downloadFile(id);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                        "attachment; filename=\"" + file.getFileName() + "\"")
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(file.getFileSize()))
                .body(file.getContent());
    }

    /**
     * Récupère les métadonnées d'un fichier (sans le contenu).
     * 
     * @param id L'ID du fichier
     * @return Les métadonnées du fichier
     */
    @GetMapping("/{id}")
    public ResponseEntity<FileDTO> getFileMetadata(@PathVariable Long id) {
        FileDTO file = fileService.getFileMetadata(id);
        return ResponseEntity.ok(file);
    }

    /**
     * Liste tous les fichiers (métadonnées seulement).
     * 
     * @return Liste des métadonnées de tous les fichiers
     */
    @GetMapping
    public ResponseEntity<List<FileDTO>> getAllFiles() {
        List<FileDTO> files = fileService.getAllFiles();
        return ResponseEntity.ok(files);
    }

    /**
     * Supprime un fichier par son ID.
     * 
     * @param id L'ID du fichier à supprimer
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFile(@PathVariable Long id) {
        fileService.deleteFile(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Recherche des fichiers par nom.
     * 
     * @param keyword Mot-clé de recherche
     * @return Liste des fichiers correspondants
     */
    @GetMapping("/search")
    public ResponseEntity<List<FileDTO>> searchFiles(
            @RequestParam("keyword") String keyword) {
        List<FileDTO> files = fileService.searchFiles(keyword);
        return ResponseEntity.ok(files);
    }

    /**
     * Récupère les statistiques de stockage.
     * 
     * @return Nombre de fichiers et espace utilisé
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStorageStats() {
        FileService.StorageStats stats = fileService.getStorageStats();
        return ResponseEntity.ok(Map.of(
                "totalFiles", stats.totalFiles(),
                "totalSizeBytes", stats.totalSizeBytes(),
                "totalSizeFormatted", stats.totalSizeFormatted()
        ));
    }
}
