package com.project.drive.controller;

import com.project.drive.entity.FileEntity;
import com.project.drive.service.FileServiceStorage;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileServiceStorage fileServiceStorage;

    public FileController(FileServiceStorage fileServiceStorage) {
        this.fileServiceStorage = fileServiceStorage;
    }

    private String getCurrentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal().toString())) {
            throw new RuntimeException("Unauthorized Access");
        }
        if (auth.getPrincipal() instanceof OAuth2User oauthUser) {
            return oauthUser.getAttribute("email");
        }
        return auth.getName();
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "parentFolderId", required = false) Long parentFolderId) {
        try {
            return ResponseEntity.ok(fileServiceStorage.saveFile(file, parentFolderId, getCurrentUserEmail()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("File upload failed! Reason: " + e.getMessage());
        }
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long id) {
        try {
            FileEntity fileEntity = fileServiceStorage.getFileById(id);
            Resource resource = fileServiceStorage.downloadFileFromDrive(fileEntity.getDriveFileId());

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileEntity.getName() + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(404).build();
        }
    }

    @GetMapping("/home")
    public ResponseEntity<List<FileEntity>> getHomeFiles() {
        return ResponseEntity.ok(fileServiceStorage.getHomeFiles(getCurrentUserEmail()));
    }

    @GetMapping("/recents")
    public ResponseEntity<List<FileEntity>> getRecentFiles() {
        return ResponseEntity.ok(fileServiceStorage.getRecentFiles(getCurrentUserEmail()));
    }

    @PutMapping("/share/{id}")
    public ResponseEntity<?> markAsShared(@PathVariable Long id) {
        fileServiceStorage.markAsShared(id);
        return ResponseEntity.ok(Map.of("message", "File shared successfully"));
    }

    @GetMapping("/share")
    public ResponseEntity<List<FileEntity>> getSharedFiles() {
        return ResponseEntity.ok(fileServiceStorage.getSharedFiles(getCurrentUserEmail()));
    }

    @GetMapping("/trash")
    public ResponseEntity<List<FileEntity>> getTrashFiles() {
        return ResponseEntity.ok(fileServiceStorage.getTrashFiles(getCurrentUserEmail()));
    }

    @GetMapping("/storage")
    public ResponseEntity<Long> getStorageInfo() {
        return ResponseEntity.ok(fileServiceStorage.getTotalUsedStorage());
    }

    @PutMapping("/trash/{id}")
    public ResponseEntity<?> moveToTrash(@PathVariable Long id) {
        fileServiceStorage.moveToTrash(id);
        return ResponseEntity.ok(Map.of("message", "Moved to trash"));
    }

    @PutMapping("/restore/{id}")
    public ResponseEntity<?> restoreFromTrash(@PathVariable Long id) {
        fileServiceStorage.restoreFromTrash(id);
        return ResponseEntity.ok(Map.of("message", "File restored successfully"));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteFile(@PathVariable Long id) {
        try {
            fileServiceStorage.deletePermanent(id);
            return ResponseEntity.ok(Map.of("message", "Deleted permanently"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "Failed to delete file"));
        }
    }
}