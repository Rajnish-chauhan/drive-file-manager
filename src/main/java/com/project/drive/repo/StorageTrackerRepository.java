package com.project.drive.repo;

import com.project.drive.entity.StorageTracker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StorageTrackerRepository extends JpaRepository<StorageTracker, Long> {

}