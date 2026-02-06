package com.devops.pfe.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entité représentant un fichier stocké en base de données.
 * Le contenu du fichier est stocké en tant que BLOB (Binary Large Object).
 */
@Entity
@Table(name = "files")
public class FileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String contentType;

    @Column(nullable = false)
    private Long fileSize;

    @Lob
    @Column(nullable = false, columnDefinition = "BLOB")
    private byte[] content;

    @Column(nullable = false)
    private LocalDateTime uploadedAt;

    @Column
    private String description;

    @PrePersist
    protected void onCreate() {
        this.uploadedAt = LocalDateTime.now();
    }

    // Constructeurs
    public FileEntity() {
    }

    public FileEntity(String fileName, String contentType, Long fileSize, byte[] content) {
        this.fileName = fileName;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.content = content;
    }

    // Getters et Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
