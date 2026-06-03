package com.project.drive.controller;

import com.project.drive.entity.FileEntity;
import com.project.drive.repo.FileRepository;
import com.project.drive.service.FileServiceStorage;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/files")
@CrossOrigin(origins = {"https://drive.rajnishsystems.in", "http://localhost:5173"}, allowCredentials = "true")
public class FileController {

    private final FileServiceStorage fileServiceStorage;
    private final FileRepository fileRepository;

    public FileController(FileServiceStorage fileServiceStorage, FileRepository fileRepository) {
        this.fileServiceStorage = fileServiceStorage;
        this.fileRepository = fileRepository;
    }

    // find user email
    private String getCurrentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
            throw new RuntimeException("Unauthorized Access");
        }
        if (auth.getPrincipal() instanceof OAuth2User) {
            OAuth2User oauthUser = (OAuth2User) auth.getPrincipal();
            return oauthUser.getAttribute("email");
        }
        return auth.getName(); // Custom login user email
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "parentFolderId", required = false) Long parentFolderId) {
        try {
            String currentUserEmail = getCurrentUserEmail();
            // send email from service
            return ResponseEntity.ok(fileServiceStorage.saveFile(file, parentFolderId, currentUserEmail));
        } catch (Exception e) {
            // FIX 2:print issue if have?
            e.printStackTrace();
            return ResponseEntity.status(500).body("File upload failed! Reason: " + e.getMessage());
        }
    }


    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long id) {
        try {
            FileEntity fileEntity = fileServiceStorage.getFileById(id);

            // FIX: Hard-disk path Google Drive binary data find
            Resource resource = fileServiceStorage.downloadFileFromDrive(fileEntity.getDriveFileId());

            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"" + fileEntity.getName() + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(404).build();
        }
    }

    //fetch those user which actually logedin
    @GetMapping("/home")
    public ResponseEntity<List<FileEntity>> getHomeFiles() {
        return ResponseEntity.ok(fileRepository.findByOwnerEmailAndIsDeletedFalse(getCurrentUserEmail()));
    }

    @GetMapping("/recents")
    public ResponseEntity<List<FileEntity>> getRecentFiles() {
        return ResponseEntity.ok(fileRepository.findByOwnerEmailAndIsDeletedFalseOrderByCreatedAtDesc(getCurrentUserEmail()));
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

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> deleteFile(@PathVariable Long id) {
        try {
            fileServiceStorage.deletePermanent(id);
            return ResponseEntity.ok("Deleted permanently");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed");
        }
    }

    // NAYA: Share link creation
    @PutMapping("/generate-share-link/{id}")
    public ResponseEntity<String> generateShareLink(@PathVariable Long id) {
        return ResponseEntity.ok(fileServiceStorage.generateShareLink(id));
    }

    // NAYA: Public File Info
    @GetMapping("/public/shared/{token}")
    public ResponseEntity<FileEntity> getSharedFileInfo(@PathVariable String token) {
        FileEntity file = fileServiceStorage.getFileByShareToken(token);
        if (!file.isShared() || file.isDeleted()) {
            return ResponseEntity.status(404).body(null);
        }
        return ResponseEntity.ok(file);
    }

    // NAYA: Publicly shared
    @GetMapping("/public/download/shared/{token}")
    public ResponseEntity<Resource> downloadSharedFile(@PathVariable String token) {
        FileEntity fileEntity = fileServiceStorage.getFileByShareToken(token);
        if (!fileEntity.isShared() || fileEntity.isDeleted()) {
            return ResponseEntity.status(404).build();
        }
        try {
            // FIX: shared file fetch from drive
            Resource resource = fileServiceStorage.downloadFileFromDrive(fileEntity.getDriveFileId());

            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"" + fileEntity.getName() + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(404).build();
        }
    }

}