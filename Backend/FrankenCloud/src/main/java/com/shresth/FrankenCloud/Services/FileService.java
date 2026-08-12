package com.shresth.FrankenCloud.Services;


import com.shresth.FrankenCloud.Config.Exceptions.*;
import com.shresth.FrankenCloud.Entity.Enum.FileType;
import com.shresth.FrankenCloud.Entity.Files;
import com.shresth.FrankenCloud.Repositories.FileRepository;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FileService {

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private CryptographyService cryptographyService;

    @Autowired
    private DriveService driveService;


    private record ShardAllocation(Long dataShards, Long parityShards) {}

    private static final List<String> VIDEO_EXTENSIONS = List.of("mp4", "mkv", "mov", "avi", "webm", "m4s");
    private static final List<String> IMAGE_EXTENSIONS = List.of("png", "jpeg", "jpg", "webp", "gif");


    public Files createUploadMetadata(ObjectId userId, ObjectId parentFolderId, String fileName, String userMail, Long fileSize) {
        ObjectId fileId = new ObjectId();

        long accessibleDrives = driveService.getDriveAccounts(userId, true).size();
        if (accessibleDrives == 0) {
            throw new DriveNotFoundException();
        }

        ShardAllocation allocation = calculateShardAllocation(accessibleDrives);
        String encryptionKey = cryptographyService.generateEncryptionKey(userId, fileId, fileName, userMail);

        Files file = new Files();
        file.setId(fileId);
        file.setUserId(userId);
        file.setParentFolderId(parentFolderId); //null if root
        file.setFileName(fileName);
        file.setFileSize(fileSize);
        file.setFileType(parseFileType(fileName));
        file.setShards(allocation.dataShards());
        file.setParityShards(allocation.parityShards());
        file.setEncryptionKey(encryptionKey);

        return fileRepository.save(file);
    }

    public Files createFolder(ObjectId userId, ObjectId parentFolderId, String folderName) {
        Files folder = new Files();
        folder.setId(new ObjectId());
        folder.setUserId(userId);
        folder.setParentFolderId(parentFolderId);
        folder.setFileName(folderName);
        folder.setFileSize(0L);
        folder.setFileType(FileType.DIRECTORY);
        folder.setShards(0L);
        folder.setParityShards(0L);
        folder.setEncryptionKey(null);

        return fileRepository.save(folder);
    }

    public List<Files> currentDirectoryItems(ObjectId userId, ObjectId parentFolderId) {
        return fileRepository.findByUserIdAndParentFolderId(userId, parentFolderId);
    }



    private FileType parseFileType(String fileName) {
        String extension = extractExtension(fileName);
        if(extension.isEmpty()) {
            return FileType.DIRECTORY;
        } else if ("pdf".equalsIgnoreCase(extension)) {
            return FileType.PDF;
        } else if (VIDEO_EXTENSIONS.contains(extension)) {
            return FileType.VIDEO;
        } else if (IMAGE_EXTENSIONS.contains(extension)) {
            return FileType.IMAGE;
        } else {
            return FileType.OTHER;
        }
    }

    private ShardAllocation calculateShardAllocation(Long totalDrives) {
        Long dataShards = calculateDataShards(totalDrives);
        Long parityShards = calculateParityShards(totalDrives, dataShards);
        return new ShardAllocation(dataShards, parityShards);
    }

    private Long calculateDataShards(Long totalDrives) {
        if (totalDrives == 1L || totalDrives == 2L) return 1L;
        else if (totalDrives <= 4L) return totalDrives - 1L; // 1 Parity Shard
        else if (totalDrives <= 8L)  return totalDrives - 2L; // 2 Parity Shards

        // 9+ Drives: 60% Data (N)
        return (long) Math.floor(totalDrives * 0.6);
    }

    private Long calculateParityShards(Long totalDrives, Long dataShards) {
        if (totalDrives == 1L) return 0L; // Single drive cannot offer parity fault tolerance
        else if (totalDrives == 2L) return 1L; // Mirroring (1 Data, 1 Parity)
        else if (totalDrives <= 8L) return totalDrives - dataShards; // Uses all remaining drives for K


        // 9+ Drives: 20% Parity (K), leaving remaining 20% as empty dynamic failover buffer
        return (long) Math.floor(totalDrives * 0.2);
    }

    private String extractExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(lastDotIndex + 1).toLowerCase();
    }

}
