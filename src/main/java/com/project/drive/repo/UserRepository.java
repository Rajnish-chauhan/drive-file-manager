package com.project.drive.repo;

import com.project.drive.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {
    // This method helps to find users from email
    Optional<UserEntity> findByEmail(String email);
}