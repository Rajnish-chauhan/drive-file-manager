package com.project.drive.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "files")
public class FileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;


    @Column(name = "drive_file_id", unique = true)
    private String driveFileId;

    @Column(name = "web_content_link", length = 500)
    private String webContentLink;

    private Long size;
    private String type;
    private Long parentFolderId;
    private LocalDateTime createdAt;

    @Column(name = "share_token")
    private String shareToken;

    // check file owner
    @Column(name = "owner_email")
    private String ownerEmail;

    @Column(columnDefinition = "boolean default false")
    private boolean isDeleted = false;

    @Column(columnDefinition = "boolean default false")
    private boolean isShared = false;

    public FileEntity() {}

    // --- NAYE GETTERS & SETTERS (Google Drive wale) ---
    public String getDriveFileId() { return driveFileId; }
    public void setDriveFileId(String driveFileId) { this.driveFileId = driveFileId; }

    public String getWebContentLink() { return webContentLink; }
    public void setWebContentLink(String webContentLink) { this.webContentLink = webContentLink; }


    public String getShareToken() { return shareToken; }
    public void setShareToken(String shareToken) { this.shareToken = shareToken; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Long getSize() { return size; }
    public void setSize(Long size) { this.size = size; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Long getParentFolderId() { return parentFolderId; }
    public void setParentFolderId(Long parentFolderId) { this.parentFolderId = parentFolderId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getOwnerEmail() { return ownerEmail; }
    public void setOwnerEmail(String ownerEmail) { this.ownerEmail = ownerEmail; }

    public boolean isDeleted() { return isDeleted; }
    public void setDeleted(boolean deleted) { this.isDeleted = deleted; }

    public boolean isShared() { return isShared; }
    public void setShared(boolean shared) { this.isShared = shared; }
}