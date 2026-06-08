package com.project.drive.controller;

import com.project.drive.entity.FileEntity;
import com.project.drive.repo.FileRepository;
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
import java.util.Optional;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileServiceStorage fileServiceStorage;
    private final FileRepository fileRepository;

    public FileController(FileServiceStorage fileServiceStorage, FileRepository fileRepository) {
        this.fileServiceStorage = fileServiceStorage;
        this.fileRepository = fileRepository;
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
            String currentUserEmail = getCurrentUserEmail();
            return ResponseEntity.ok(fileServiceStorage.saveFile(file, parentFolderId, currentUserEmail));
        } catch (Exception e) {
            e.printStackTrace();
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
        return ResponseEntity.ok(fileRepository.findByOwnerEmailAndIsDeletedFalse(getCurrentUserEmail()));
    }

    @GetMapping("/recents")
    public ResponseEntity<List<FileEntity>> getRecentFiles() {
        return ResponseEntity.ok(fileRepository.findByOwnerEmailAndIsDeletedFalseOrderByCreatedAtDesc(getCurrentUserEmail()));
    }

    // 🔴 Added the missing PUT endpoint for the React share button
    @PutMapping("/share/{id}")
    public ResponseEntity<?> markAsShared(@PathVariable Long id) {
        Optional<FileEntity> fileOpt = fileRepository.findById(id);
        if (fileOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("message", "File not found"));
        }
        FileEntity file = fileOpt.get();
        file.setShared(true);
        fileRepository.save(file);
        return ResponseEntity.ok(Map.of("message", "File shared successfully"));
    }

    @GetMapping("/share")
    public ResponseEntity<List<FileEntity>> getSharedFiles() {
        return ResponseEntity.ok(fileRepository.findByOwnerEmailAndIsSharedTrueAndIsDeletedFalse(getCurrentUserEmail()));
    }

    @GetMapping("/trash")
    public ResponseEntity<List<FileEntity>> getTrashFiles() {
        return ResponseEntity.ok(fileRepository.findByOwnerEmailAndIsDeletedTrue(getCurrentUserEmail()));
    }

    @GetMapping("/storage")
    public ResponseEntity<Long> getStorageInfo() {
        return ResponseEntity.ok(fileServiceStorage.getTotalUsedStorage());
    }

    @PutMapping("/trash/{id}")
    public ResponseEntity<String> moveToTrash(@PathVariable Long id) {
        fileServiceStorage.moveToTrash(id);
        return ResponseEntity.ok("Moved to trash");
    }

    @PutMapping("/restore/{id}")
    public ResponseEntity<String> restoreFromTrash(@PathVariable Long id) {
        fileServiceStorage.restoreFromTrash(id);
        return ResponseEntity.ok("File restored successfully");
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> deleteFile(@PathVariable Long id) {
        try {
            fileServiceStorage.deletePermanent(id);
            return ResponseEntity.ok("Deleted permanently");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed");
        }
    }
}