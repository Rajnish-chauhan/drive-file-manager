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
import java.util.List;
import java.util.UUID;

@Service
public class FileServiceStorage {

    private final FileRepository fileRepository;
    private final StorageTrackerRepository storageTrackerRepository;
    private final Drive driveService;

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

        StorageTracker tracker = storageTrackerRepository.findById(1L).orElseGet(() -> {
            StorageTracker newTracker = new StorageTracker();
            newTracker.setId(1L);
            newTracker.setTotalUsedBytes(0L);
            newTracker.setMaxLimitBytes(5368709120L); // 5GB Limit
            return storageTrackerRepository.save(newTracker);
        });

        if (tracker.getTotalUsedBytes() + incomingSize > tracker.getMaxLimitBytes()) {
            throw new RuntimeException("5GB Global Storage Limit Exceeded!");
        }

        File fileMetadata = new File();
        fileMetadata.setName(file.getOriginalFilename());
        fileMetadata.setParents(Collections.singletonList(sharedFolderId));

        InputStreamContent mediaContent = new InputStreamContent(
                file.getContentType(), file.getInputStream());

        File uploadedDriveFile = driveService.files().create(fileMetadata, mediaContent)
                .setFields("id, name, webContentLink")
                .execute();

        tracker.setTotalUsedBytes(tracker.getTotalUsedBytes() + incomingSize);
        storageTrackerRepository.save(tracker);

        FileEntity fileEntity = new FileEntity();
        fileEntity.setName(uploadedDriveFile.getName());
        fileEntity.setDriveFileId(uploadedDriveFile.getId());
        fileEntity.setWebContentLink(uploadedDriveFile.getWebContentLink());
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


    public List<FileEntity> getHomeFiles(String email) {
        return fileRepository.findByOwnerEmailAndIsDeletedFalse(email);
    }

    public List<FileEntity> getRecentFiles(String email) {
        return fileRepository.findByOwnerEmailAndIsDeletedFalseOrderByCreatedAtDesc(email);
    }

    public List<FileEntity> getSharedFiles(String email) {
        return fileRepository.findByOwnerEmailAndIsSharedTrueAndIsDeletedFalse(email);
    }

    public List<FileEntity> getTrashFiles(String email) {
        return fileRepository.findByOwnerEmailAndIsDeletedTrue(email);
    }

    public void markAsShared(Long id) {
        FileEntity file = getFileById(id);
        file.setShared(true);
        fileRepository.save(file);
    }


    public void restoreFromTrash(Long id) {
        FileEntity file = getFileById(id);
        file.setDeleted(false);
        fileRepository.save(file);
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

        if (file.getDriveFileId() != null) {
            try {
                driveService.files().delete(file.getDriveFileId()).execute();
            } catch (Exception e) {
                System.out.println("Unable to Delete from Google Drive");
            }
        }

        StorageTracker tracker = storageTrackerRepository.findById(1L).orElse(null);
        if (tracker != null) {
            tracker.setTotalUsedBytes(tracker.getTotalUsedBytes() - file.getSize());
            storageTrackerRepository.save(tracker);
        }

        fileRepository.deleteById(id);
    }

    public long getTotalUsedStorage() {
        return storageTrackerRepository.findById(1L)
                .map(StorageTracker::getTotalUsedBytes)
                .orElse(0L);
    }

    public org.springframework.core.io.Resource downloadFileFromDrive(String driveFileId) throws Exception {
        java.io.InputStream is = driveService.files().get(driveFileId).executeMediaAsInputStream();
        return new org.springframework.core.io.InputStreamResource(is);
    }
}