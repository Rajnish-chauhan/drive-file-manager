package com.project.drive;

import com.project.drive.controller.FileController;
import com.project.drive.repo.FileRepository;
import com.project.drive.service.FileServiceStorage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FileController.class)
// @Import(SecurityConfig.class) // Uncomment if you have a custom SecurityConfig
class FileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FileServiceStorage fileServiceStorage;

    @MockitoBean
    private FileRepository fileRepository;

    @Test
    @WithMockUser // Simulates a logged-in user
    void testGetHomeFiles_ReturnsList() throws Exception {
        // Arrange: Mock the repository to return an empty list
        when(fileRepository.findByOwnerEmailAndIsDeletedFalse(anyString()))
                .thenReturn(Collections.emptyList());

        // Act & Assert
        mockMvc.perform(get("/api/files/home"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray()); // Correct usage of ResultMatcher
    }
}