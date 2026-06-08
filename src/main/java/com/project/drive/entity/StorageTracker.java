package com.project.drive.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "storage_tracker")
public class StorageTracker {

    @Id
    private Long id = 1L;
    @Column(nullable = false)
    private Long totalUsedBytes = 0L;

    @Column(nullable = false)
    // Updated to 5GB
    private Long maxLimitBytes = 5368709120L;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTotalUsedBytes() { return totalUsedBytes; }
    public void setTotalUsedBytes(Long totalUsedBytes) { this.totalUsedBytes = totalUsedBytes; }
    public Long getMaxLimitBytes() { return maxLimitBytes; }
    public void setMaxLimitBytes(Long maxLimitBytes) { this.maxLimitBytes = maxLimitBytes; }
}