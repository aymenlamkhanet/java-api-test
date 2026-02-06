package com.devops.pfe.controller;

import com.devops.pfe.dto.FileDTO;
import com.devops.pfe.entity.FileEntity;
import com.devops.pfe.exception.ResourceNotFoundException;
import com.devops.pfe.service.FileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@WebMvcTest(FileController.class)
@DisplayName("FileController Tests")
class FileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FileService fileService;

    private FileDTO testFileDTO;
    private FileEntity testFileEntity;

    @BeforeEach
    void setUp() {
        testFileDTO = new FileDTO(
                1L,
                "test-document.pdf",
                "application/pdf",
                1024L,
                LocalDateTime.now(),
                "Test PDF file"
        );

        testFileEntity = new FileEntity();
        testFileEntity.setId(1L);
        testFileEntity.setFileName("test-document.pdf");
        testFileEntity.setContentType("application/pdf");
        testFileEntity.setFileSize(1024L);
        testFileEntity.setContent("PDF content".getBytes());
        testFileEntity.setDescription("Test PDF file");
        testFileEntity.setUploadedAt(LocalDateTime.now());
    }

    @Nested
    @DisplayName("POST /api/v1/files/upload")
    class UploadFileTests {

        @Test
        @DisplayName("Should upload file successfully and return 201")
        void shouldUploadFileSuccessfully() throws Exception {
            // Given
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "document.pdf",
                    "application/pdf",
                    "PDF content".getBytes()
            );
            when(fileService.uploadFile(any(), any())).thenReturn(testFileDTO);

            // When/Then
            mockMvc.perform(multipart("/api/v1/files/upload")
                            .file(file)
                            .param("description", "Test description"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.fileName").value("test-document.pdf"))
                    .andExpect(jsonPath("$.contentType").value("application/pdf"))
                    .andExpect(jsonPath("$.downloadUrl").value("/api/v1/files/1/download"));
        }

        @Test
        @DisplayName("Should upload file without description")
        void shouldUploadFileWithoutDescription() throws Exception {
            // Given
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "image.png",
                    "image/png",
                    "PNG content".getBytes()
            );
            when(fileService.uploadFile(any(), isNull())).thenReturn(testFileDTO);

            // When/Then
            mockMvc.perform(multipart("/api/v1/files/upload").file(file))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").exists());
        }

        @Test
        @DisplayName("Should return 400 when file is empty")
        void shouldReturn400WhenFileIsEmpty() throws Exception {
            // Given
            MockMultipartFile emptyFile = new MockMultipartFile(
                    "file",
                    "empty.pdf",
                    "application/pdf",
                    new byte[0]
            );
            when(fileService.uploadFile(any(), any()))
                    .thenThrow(new IllegalArgumentException("Le fichier est vide"));

            // When/Then
            mockMvc.perform(multipart("/api/v1/files/upload").file(emptyFile))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/files/{id}/download")
    class DownloadFileTests {

        @Test
        @DisplayName("Should download file successfully")
        void shouldDownloadFileSuccessfully() throws Exception {
            // Given
            when(fileService.downloadFile(1L)).thenReturn(testFileEntity);

            // When/Then
            mockMvc.perform(get("/api/v1/files/1/download"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                    .andExpect(header().string("Content-Disposition", 
                            containsString("test-document.pdf")))
                    .andExpect(content().bytes("PDF content".getBytes()));
        }

        @Test
        @DisplayName("Should return 404 when file not found")
        void shouldReturn404WhenFileNotFound() throws Exception {
            // Given
            when(fileService.downloadFile(999L))
                    .thenThrow(new ResourceNotFoundException("Fichier non trouvé"));

            // When/Then
            mockMvc.perform(get("/api/v1/files/999/download"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/files/{id}")
    class GetFileMetadataTests {

        @Test
        @DisplayName("Should get file metadata successfully")
        void shouldGetFileMetadataSuccessfully() throws Exception {
            // Given
            when(fileService.getFileMetadata(1L)).thenReturn(testFileDTO);

            // When/Then
            mockMvc.perform(get("/api/v1/files/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.fileName").value("test-document.pdf"))
                    .andExpect(jsonPath("$.contentType").value("application/pdf"))
                    .andExpect(jsonPath("$.fileSize").value(1024));
        }

        @Test
        @DisplayName("Should return 404 when file not found")
        void shouldReturn404WhenFileNotFoundForMetadata() throws Exception {
            // Given
            when(fileService.getFileMetadata(999L))
                    .thenThrow(new ResourceNotFoundException("Fichier non trouvé"));

            // When/Then
            mockMvc.perform(get("/api/v1/files/999"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/files")
    class GetAllFilesTests {

        @Test
        @DisplayName("Should get all files")
        void shouldGetAllFiles() throws Exception {
            // Given
            FileDTO file2 = new FileDTO(2L, "image.png", "image/png", 2048L, 
                    LocalDateTime.now(), "Test image");
            when(fileService.getAllFiles()).thenReturn(Arrays.asList(testFileDTO, file2));

            // When/Then
            mockMvc.perform(get("/api/v1/files"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].fileName").value("test-document.pdf"))
                    .andExpect(jsonPath("$[1].fileName").value("image.png"));
        }

        @Test
        @DisplayName("Should return empty list when no files")
        void shouldReturnEmptyListWhenNoFiles() throws Exception {
            // Given
            when(fileService.getAllFiles()).thenReturn(List.of());

            // When/Then
            mockMvc.perform(get("/api/v1/files"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/files/{id}")
    class DeleteFileTests {

        @Test
        @DisplayName("Should delete file successfully")
        void shouldDeleteFileSuccessfully() throws Exception {
            // Given
            doNothing().when(fileService).deleteFile(1L);

            // When/Then
            mockMvc.perform(delete("/api/v1/files/1"))
                    .andExpect(status().isNoContent());

            verify(fileService, times(1)).deleteFile(1L);
        }

        @Test
        @DisplayName("Should return 404 when deleting non-existent file")
        void shouldReturn404WhenDeletingNonExistentFile() throws Exception {
            // Given
            doThrow(new ResourceNotFoundException("Fichier non trouvé"))
                    .when(fileService).deleteFile(999L);

            // When/Then
            mockMvc.perform(delete("/api/v1/files/999"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/files/search")
    class SearchFilesTests {

        @Test
        @DisplayName("Should search files by keyword")
        void shouldSearchFilesByKeyword() throws Exception {
            // Given
            when(fileService.searchFiles("document")).thenReturn(List.of(testFileDTO));

            // When/Then
            mockMvc.perform(get("/api/v1/files/search")
                            .param("keyword", "document"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].fileName").value("test-document.pdf"));
        }

        @Test
        @DisplayName("Should return empty list when no matches")
        void shouldReturnEmptyListWhenNoMatches() throws Exception {
            // Given
            when(fileService.searchFiles("nonexistent")).thenReturn(List.of());

            // When/Then
            mockMvc.perform(get("/api/v1/files/search")
                            .param("keyword", "nonexistent"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/files/stats")
    class StorageStatsTests {

        @Test
        @DisplayName("Should get storage stats")
        void shouldGetStorageStats() throws Exception {
            // Given
            when(fileService.getStorageStats())
                    .thenReturn(new FileService.StorageStats(5L, 1048576L, "1.00 MB"));

            // When/Then
            mockMvc.perform(get("/api/v1/files/stats"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalFiles").value(5))
                    .andExpect(jsonPath("$.totalSizeBytes").value(1048576))
                    .andExpect(jsonPath("$.totalSizeFormatted").value("1.00 MB"));
        }
    }
}
