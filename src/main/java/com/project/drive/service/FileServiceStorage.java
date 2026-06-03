package com.project.drive.service;

import com.google.api.client.http.InputStreamContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.project.drive.entity.FileEntity;
import com.project.drive.entity.StorageTracker;
import com.project.drive.repo.FileRepository;
import com.project.drive.repo.StorageTrackerRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.UUID;

@Service
public class FileServiceStorage {

    private final FileRepository fileRepository;
    private final StorageTrackerRepository storageTrackerRepository;
    private final Drive driveService; // NAYA: Google Drive Client

    @Value("${google.drive.folder.id}")
    private String sharedFolderId;

    public FileServiceStorage(FileRepository fileRepository,
                              StorageTrackerRepository storageTrackerRepository,
                              Drive driveService) {
        this.fileRepository = fileRepository;
        this.storageTrackerRepository = storageTrackerRepository;
        this.driveService = driveService;
    }

    @Transactional(rollbackFor = Exception.class)
    public String saveFile(MultipartFile file, Long parentFolderId, String ownerEmail) throws Exception {
        long incomingSize = file.getSize();

        // FIX 1: Agar Tracker entry not found then create it dont throw exception or error
        StorageTracker tracker = storageTrackerRepository.findById(1L).orElseGet(() -> {
            StorageTracker newTracker = new StorageTracker();
            newTracker.setId(1L);
            newTracker.setTotalUsedBytes(0L);
            newTracker.setMaxLimitBytes(2147483648L); // 2GB Limit
            return storageTrackerRepository.save(newTracker);
        });

        if (tracker.getTotalUsedBytes() + incomingSize > tracker.getMaxLimitBytes()) {
            throw new RuntimeException("2GB Global Storage Limit Exceeded!");
        }

        // 2. Google Drive par File Upload
        File fileMetadata = new File();
        fileMetadata.setName(file.getOriginalFilename());
        fileMetadata.setParents(Collections.singletonList(sharedFolderId));

        InputStreamContent mediaContent = new InputStreamContent(
                file.getContentType(), file.getInputStream());

        // File upload find id and url
        File uploadedDriveFile = driveService.files().create(fileMetadata, mediaContent)
                .setFields("id, name, webContentLink")
                .execute();

        // 3. TiDB update tracker
        tracker.setTotalUsedBytes(tracker.getTotalUsedBytes() + incomingSize);
        storageTrackerRepository.save(tracker);

        // 4. File details save in DB
        FileEntity fileEntity = new FileEntity();
        fileEntity.setName(uploadedDriveFile.getName());
        fileEntity.setDriveFileId(uploadedDriveFile.getId()); // NAYA
        fileEntity.setWebContentLink(uploadedDriveFile.getWebContentLink()); // NAYA
        fileEntity.setSize(incomingSize);
        fileEntity.setType(file.getContentType());
        fileEntity.setParentFolderId(parentFolderId);
        fileEntity.setCreatedAt(java.time.LocalDateTime.now());
        fileEntity.setOwnerEmail(ownerEmail);

        fileRepository.save(fileEntity);
        return "File uploaded to Google Drive successfully";
    }

    public FileEntity getFileById(Long id) {
        return fileRepository.findById(id).orElseThrow(()->new RuntimeException("File not found"));
    }

    public String generateShareLink(Long id) {
        FileEntity file = getFileById(id);
        if(file.getShareToken() == null) {
            file.setShareToken(UUID.randomUUID().toString());
        }
        file.setShared(true);
        fileRepository.save(file);
        return file.getShareToken();
    }

    public FileEntity getFileByShareToken(String token) {
        return fileRepository.findByShareToken(token).orElseThrow(() -> new RuntimeException("Link Invalid"));
    }

    public void moveToTrash(Long id) {
        FileEntity file = getFileById(id);
        file.setDeleted(true);
        fileRepository.save(file);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deletePermanent(Long id) throws Exception {
        FileEntity file = getFileById(id);

        // 1. Google Drive se permanently udana
        if (file.getDriveFileId() != null) {
            try {
                driveService.files().delete(file.getDriveFileId()).execute();
            } catch (Exception e) {
                System.out.println("Google Drive se delete nahi ho payi.");
            }
        }

        // 2. Storage space back
        StorageTracker tracker = storageTrackerRepository.findById(1L).orElse(null);
        if (tracker != null) {
            tracker.setTotalUsedBytes(tracker.getTotalUsedBytes() - file.getSize());
            storageTrackerRepository.save(tracker);
        }

        // 3. DB se delete
        fileRepository.deleteById(id);
    }

    public long getTotalUsedStorage() {
        return storageTrackerRepository.findById(1L)
                .map(StorageTracker::getTotalUsedBytes)
                .orElse(0L);
    }

    // Google Drive (fetch data and convert into stream)
    public org.springframework.core.io.Resource downloadFileFromDrive(String driveFileId) throws Exception {
        java.io.InputStream is = driveService.files().get(driveFileId).executeMediaAsInputStream();
        return new org.springframework.core.io.InputStreamResource(is);
    }
}