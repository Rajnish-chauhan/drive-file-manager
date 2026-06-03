package com.project.drive.repo;

import com.project.drive.entity.FileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface FileRepository extends JpaRepository<FileEntity, Long> {
    Optional<FileEntity> findByShareToken(String shareToken);
    // Each query check and verify user email
    List<FileEntity> findByOwnerEmailAndIsDeletedFalse(String ownerEmail); // Home
    List<FileEntity> findByOwnerEmailAndIsDeletedFalseOrderByCreatedAtDesc(String ownerEmail); // Recents
    List<FileEntity> findByOwnerEmailAndIsSharedTrueAndIsDeletedFalse(String ownerEmail); // Shared
    List<FileEntity> findByOwnerEmailAndIsDeletedTrue(String ownerEmail); // Trash

}