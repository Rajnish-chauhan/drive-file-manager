package com.project.drive;

import com.google.api.services.drive.Drive;
import com.project.drive.entity.StorageTracker;
import com.project.drive.repo.FileRepository;
import com.project.drive.repo.StorageTrackerRepository;
import com.project.drive.service.FileServiceStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

@ExtendWith(MockitoExtension.class)
class FileServiceStorageTest {

    @Mock private FileRepository fileRepository;
    @Mock
    private StorageTrackerRepository storageTrackerRepository;
    @Mock private Drive driveService;

    @InjectMocks
    private FileServiceStorage fileServiceStorage;

    @Test
    void testGetTotalUsedStorage_ReturnsValue() {
        // Arrange
        StorageTracker tracker = new StorageTracker();
        tracker.setTotalUsedBytes(1024L);
        when(storageTrackerRepository.findById(1L)).thenReturn(Optional.of(tracker));

        // Act
        long result = fileServiceStorage.getTotalUsedStorage();

        // Assert
        assertEquals(1024L, result);
        verify(storageTrackerRepository, times(1)).findById(1L);
    }
}