package com.devops.pfe.service;

import com.devops.pfe.dto.FileDTO;
import com.devops.pfe.entity.FileEntity;
import com.devops.pfe.exception.ResourceNotFoundException;
import com.devops.pfe.repository.FileRepository;
import com.devops.pfe.service.impl.FileServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FileService Tests")
class FileServiceTest {

    @Mock
    private FileRepository fileRepository;

    @InjectMocks
    private FileServiceImpl fileService;

    private FileEntity testFile;
    private MultipartFile mockPdfFile;
    private MultipartFile mockImageFile;

    @BeforeEach
    void setUp() {
        // Créer une entité fichier de test
        testFile = new FileEntity();
        testFile.setId(1L);
        testFile.setFileName("test-document.pdf");
        testFile.setContentType("application/pdf");
        testFile.setFileSize(1024L);
        testFile.setContent("PDF content".getBytes());
        testFile.setDescription("Test PDF file");
        testFile.setUploadedAt(LocalDateTime.now());

        // Créer des fichiers multipart de test
        mockPdfFile = new MockMultipartFile(
                "file",
                "document.pdf",
                "application/pdf",
                "PDF file content for testing".getBytes()
        );

        mockImageFile = new MockMultipartFile(
                "file",
                "image.png",
                "image/png",
                "PNG image content".getBytes()
        );
    }

    @Nested
    @DisplayName("Upload File Tests")
    class UploadFileTests {

        @Test
        @DisplayName("Should upload PDF file successfully")
        void shouldUploadPdfFileSuccessfully() {
            // Given
            when(fileRepository.save(any(FileEntity.class))).thenAnswer(invocation -> {
                FileEntity saved = invocation.getArgument(0);
                saved.setId(1L);
                saved.setUploadedAt(LocalDateTime.now());
                return saved;
            });

            // When
            FileDTO result = fileService.uploadFile(mockPdfFile, "Test description");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getFileName()).isEqualTo("document.pdf");
            assertThat(result.getContentType()).isEqualTo("application/pdf");
            assertThat(result.getDescription()).isEqualTo("Test description");
            assertThat(result.getDownloadUrl()).isEqualTo("/api/v1/files/1/download");
            
            verify(fileRepository, times(1)).save(any(FileEntity.class));
        }

        @Test
        @DisplayName("Should upload image file successfully")
        void shouldUploadImageFileSuccessfully() {
            // Given
            when(fileRepository.save(any(FileEntity.class))).thenAnswer(invocation -> {
                FileEntity saved = invocation.getArgument(0);
                saved.setId(2L);
                saved.setUploadedAt(LocalDateTime.now());
                return saved;
            });

            // When
            FileDTO result = fileService.uploadFile(mockImageFile, null);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getFileName()).isEqualTo("image.png");
            assertThat(result.getContentType()).isEqualTo("image/png");
        }

        @Test
        @DisplayName("Should throw exception when file is empty")
        void shouldThrowExceptionWhenFileIsEmpty() {
            // Given
            MultipartFile emptyFile = new MockMultipartFile(
                    "file",
                    "empty.pdf",
                    "application/pdf",
                    new byte[0]
            );

            // When/Then
            assertThatThrownBy(() -> fileService.uploadFile(emptyFile, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("vide");
        }

        @Test
        @DisplayName("Should throw exception when file exceeds max size")
        void shouldThrowExceptionWhenFileExceedsMaxSize() {
            // Given: fichier de 11 MB (> 10 MB max)
            byte[] largeContent = new byte[11 * 1024 * 1024];
            MultipartFile largeFile = new MockMultipartFile(
                    "file",
                    "large.pdf",
                    "application/pdf",
                    largeContent
            );

            // When/Then
            assertThatThrownBy(() -> fileService.uploadFile(largeFile, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("10 MB");
        }
    }

    @Nested
    @DisplayName("Download File Tests")
    class DownloadFileTests {

        @Test
        @DisplayName("Should download file successfully")
        void shouldDownloadFileSuccessfully() {
            // Given
            when(fileRepository.findById(1L)).thenReturn(Optional.of(testFile));

            // When
            FileEntity result = fileService.downloadFile(1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getFileName()).isEqualTo("test-document.pdf");
            assertThat(result.getContent()).isNotNull();
        }

        @Test
        @DisplayName("Should throw exception when file not found")
        void shouldThrowExceptionWhenFileNotFound() {
            // Given
            when(fileRepository.findById(999L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> fileService.downloadFile(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("999");
        }
    }

    @Nested
    @DisplayName("Get File Metadata Tests")
    class GetFileMetadataTests {

        @Test
        @DisplayName("Should get file metadata successfully")
        void shouldGetFileMetadataSuccessfully() {
            // Given
            when(fileRepository.findById(1L)).thenReturn(Optional.of(testFile));

            // When
            FileDTO result = fileService.getFileMetadata(1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getFileName()).isEqualTo("test-document.pdf");
            assertThat(result.getFileSize()).isEqualTo(1024L);
        }

        @Test
        @DisplayName("Should throw exception when file not found for metadata")
        void shouldThrowExceptionWhenFileNotFoundForMetadata() {
            // Given
            when(fileRepository.findById(999L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> fileService.getFileMetadata(999L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Get All Files Tests")
    class GetAllFilesTests {

        @Test
        @DisplayName("Should get all files")
        void shouldGetAllFiles() {
            // Given
            FileEntity file2 = new FileEntity();
            file2.setId(2L);
            file2.setFileName("image.png");
            file2.setContentType("image/png");
            file2.setFileSize(2048L);
            file2.setContent("PNG content".getBytes());
            file2.setUploadedAt(LocalDateTime.now());

            when(fileRepository.findAll()).thenReturn(Arrays.asList(testFile, file2));

            // When
            List<FileDTO> results = fileService.getAllFiles();

            // Then
            assertThat(results).hasSize(2);
            assertThat(results.get(0).getFileName()).isEqualTo("test-document.pdf");
            assertThat(results.get(1).getFileName()).isEqualTo("image.png");
        }

        @Test
        @DisplayName("Should return empty list when no files")
        void shouldReturnEmptyListWhenNoFiles() {
            // Given
            when(fileRepository.findAll()).thenReturn(List.of());

            // When
            List<FileDTO> results = fileService.getAllFiles();

            // Then
            assertThat(results).isEmpty();
        }
    }

    @Nested
    @DisplayName("Delete File Tests")
    class DeleteFileTests {

        @Test
        @DisplayName("Should delete file successfully")
        void shouldDeleteFileSuccessfully() {
            // Given
            when(fileRepository.existsById(1L)).thenReturn(true);
            doNothing().when(fileRepository).deleteById(1L);

            // When
            fileService.deleteFile(1L);

            // Then
            verify(fileRepository, times(1)).deleteById(1L);
        }

        @Test
        @DisplayName("Should throw exception when deleting non-existent file")
        void shouldThrowExceptionWhenDeletingNonExistentFile() {
            // Given
            when(fileRepository.existsById(999L)).thenReturn(false);

            // When/Then
            assertThatThrownBy(() -> fileService.deleteFile(999L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Search Files Tests")
    class SearchFilesTests {

        @Test
        @DisplayName("Should search files by keyword")
        void shouldSearchFilesByKeyword() {
            // Given
            when(fileRepository.findByFileNameContainingIgnoreCase("document"))
                    .thenReturn(List.of(testFile));

            // When
            List<FileDTO> results = fileService.searchFiles("document");

            // Then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getFileName()).contains("document");
        }

        @Test
        @DisplayName("Should return empty list when no matches")
        void shouldReturnEmptyListWhenNoMatches() {
            // Given
            when(fileRepository.findByFileNameContainingIgnoreCase("nonexistent"))
                    .thenReturn(List.of());

            // When
            List<FileDTO> results = fileService.searchFiles("nonexistent");

            // Then
            assertThat(results).isEmpty();
        }
    }

    @Nested
    @DisplayName("Storage Stats Tests")
    class StorageStatsTests {

        @Test
        @DisplayName("Should get storage stats")
        void shouldGetStorageStats() {
            // Given
            when(fileRepository.countAllFiles()).thenReturn(5L);
            when(fileRepository.getTotalStorageUsed()).thenReturn(1024L * 1024L); // 1 MB

            // When
            FileService.StorageStats stats = fileService.getStorageStats();

            // Then
            assertThat(stats.totalFiles()).isEqualTo(5L);
            assertThat(stats.totalSizeBytes()).isEqualTo(1024L * 1024L);
            assertThat(stats.totalSizeFormatted()).isEqualTo("1.00 MB");
        }

        @Test
        @DisplayName("Should format bytes correctly")
        void shouldFormatBytesCorrectly() {
            // Given
            when(fileRepository.countAllFiles()).thenReturn(1L);
            when(fileRepository.getTotalStorageUsed()).thenReturn(500L);

            // When
            FileService.StorageStats stats = fileService.getStorageStats();

            // Then
            assertThat(stats.totalSizeFormatted()).isEqualTo("500 B");
        }

        @Test
        @DisplayName("Should format KB correctly")
        void shouldFormatKBCorrectly() {
            // Given
            when(fileRepository.countAllFiles()).thenReturn(1L);
            when(fileRepository.getTotalStorageUsed()).thenReturn(2048L);

            // When
            FileService.StorageStats stats = fileService.getStorageStats();

            // Then
            assertThat(stats.totalSizeFormatted()).isEqualTo("2.00 KB");
        }
    }
}
