package com.shresth.FrankenCloud.Services;


import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.shresth.FrankenCloud.Config.Exceptions.*;
import com.shresth.FrankenCloud.DTO.DownloadableChunkResponse;
import com.shresth.FrankenCloud.DTO.FileChunkResponse;
import com.shresth.FrankenCloud.DTO.FileDownloadManifestResponse;
import com.shresth.FrankenCloud.DTO.FileRequest;
import com.shresth.FrankenCloud.Entity.DriveAccount;
import com.shresth.FrankenCloud.Entity.Enum.ChunkStatus;
import com.shresth.FrankenCloud.Entity.Enum.FileType;
import com.shresth.FrankenCloud.Entity.FileChunk;
import com.shresth.FrankenCloud.Entity.Files;
import com.shresth.FrankenCloud.Repositories.FileRepository;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class FileService {

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private CryptographyService cryptographyService;

    @Autowired
    private DriveService driveService;

    @Autowired
    private FileChunkService fileChunkService;

    public record UploadPreparation(Files file, List<DriveAccount> targetDrives) {}

    private record ShardAllocation(Long dataShards, Long parityShards) {}

    private static final List<String> VIDEO_EXTENSIONS = List.of("mp4", "mkv", "mov", "avi", "webm", "m4s");
    private static final List<String> IMAGE_EXTENSIONS = List.of("png", "jpeg", "jpg", "webp", "gif");


    public Files createUploadMetadata(ObjectId userId,
                                      ObjectId parentFolderId,
                                      String fileName,
                                      String userMail,
                                      Long fileSize,
                                      List<DriveAccount> driveAccounts) {
        ObjectId fileId = new ObjectId();

        long accessibleDrivesSize = driveAccounts.size();
        if (accessibleDrivesSize == 0) {
            throw new DriveNotFoundException();
        }

        ShardAllocation allocation = calculateShardAllocation(accessibleDrivesSize);
        String encryptionKey = cryptographyService.generateEncryptionKey(userId, fileId, fileName, userMail);
        String iv = cryptographyService.generateIV();

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
        file.setIv(iv);

        return fileRepository.save(file);
    }

    public List<FileChunkResponse> getChunksMetadata(ObjectId userId,
                                                     ObjectId parentFolderId,
                                                     String userMail,
                                                     FileRequest fileRequest) throws IOException {
        //received file metadata and drives

        List<DriveAccount> driveAccounts = driveService.getDriveAccounts(userId, true, true);


        Files file = createUploadMetadata(
                userId,
                parentFolderId,
                fileRequest.getFileName(),
                userMail,
                fileRequest.getFileSize(),
                driveAccounts
        );


        List<FileChunkResponse> responses = new ArrayList<>();
        List<FileChunk> fileChunks = new ArrayList<>();

        fileToChunkAndResponse(fileChunks, responses, driveAccounts, file);
        fileChunkService.saveAllFileChunks(fileChunks);
//        frontend will send four things, chunk status, driveFileId(the drive url) and chunk hash
        return responses;
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
        Long parityShards = totalDrives - dataShards;
        return new ShardAllocation(dataShards, parityShards);
    }

    private Long calculateDataShards(Long totalDrives) {
        if (totalDrives == 1L || totalDrives == 2L) return 1L;
        else if (totalDrives <= 4L) return totalDrives - 1L; // 1 Parity Shard
        else if (totalDrives < 8L)  return totalDrives - 2L; // 2 Parity Shards

        return totalDrives - 3L;
    }

    private String extractExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(lastDotIndex + 1).toLowerCase();
    }

    private void fileToChunkAndResponse(List<FileChunk> fileChunks, List<FileChunkResponse> chunkResponses, List<DriveAccount> driveAccounts, Files file) throws IOException {
        int totalShards = (int) (file.getShards() + file.getParityShards());
        long targetShardSize = (long) Math.ceil((double) file.getFileSize() / file.getShards());

        for(int i = 0; i < totalShards; i++) {
            ObjectId chunkId = new ObjectId();

            //adding to the response
            FileChunkResponse res = new FileChunkResponse();
            String segmentName = file.getId().toHexString() + (i < file.getShards() ? "__chunk_" : "__parity__chunk_") + i;
            res.setChunkId(chunkId);
            res.setFileId(file.getId());
            res.setAccountId(driveAccounts.get(i % driveAccounts.size()).getAccountId());
            res.setEncryptionKey(file.getEncryptionKey()); //this will go to frontend for enc
            res.setIv(file.getIv()); //this will go to frontend for enc
            res.setChunkIndex((long) i);
            res.setSegmentName(segmentName);
            res.setDriveFileURI(driveService.generateUploadURI(driveAccounts.get(i % driveAccounts.size()), segmentName));
            res.setIsParity(i >= file.getShards());
            res.setStatus(ChunkStatus.UPLOADING);
            res.setChunkSize(targetShardSize);
            chunkResponses.add(res);

            //adding to the chunk for db
            FileChunk fileChunk = new FileChunk();
            fileChunk.setId(chunkId);
            fileChunk.setChunkSize(targetShardSize);
            fileChunk.setChunkIndex((long) i);
            fileChunk.setFileId(file.getId());
            fileChunk.setStatus(ChunkStatus.UPLOADING);
            fileChunk.setAccountId(driveAccounts.get(i % driveAccounts.size()).getAccountId());
            fileChunk.setSegmentName(segmentName);
            fileChunk.setIsParity(i >= file.getShards());
            fileChunks.add(fileChunk);
        }
    }

    public FileDownloadManifestResponse getFileMetadata(ObjectId fileId, ObjectId userId) {
        Files file = getFile(fileId);
        if (file == null) {
            throw new FileNotFoundException("File not found");
        }
        if (!file.getUserId().equals(userId)) {
            throw new UnauthorizedAccessException();
        }
        List<FileChunk> fileChunks = fileChunkService.getFileChunksByFileId(fileId);
        if (fileChunks.isEmpty()) {
            throw new FileNotFoundException("File chunks not found");
        }
        return setFileManifestResponse(fileChunks, file);
    }

    private Files getFile(ObjectId fileId) {
        return fileRepository.getFilesById(fileId);
    }

    private FileDownloadManifestResponse setFileManifestResponse(List<FileChunk> fileChunks, Files file) {
        FileDownloadManifestResponse response = new FileDownloadManifestResponse();
        response.setFileId(file.getId());
        response.setFileSize(file.getFileSize());
        response.setFileType(file.getFileType());
        response.setFileName(file.getFileName());
        response.setDataShards(file.getShards());
        response.setParityShards(file.getParityShards());
        response.setEncryptionKey(file.getEncryptionKey());
        response.setIv(file.getIv());

        List<DownloadableChunkResponse> chunkResponses = new ArrayList<>();

                fileChunks.forEach(
                chunk ->  {
                    DownloadableChunkResponse chunkResponse = null;
                    try {
                        chunkResponse = setChunkManifestResponse(chunk, file);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                    chunkResponses.add(chunkResponse);
                });

        response.setChunks(chunkResponses);
        return response;
    }

    private DownloadableChunkResponse setChunkManifestResponse(FileChunk fileChunk, Files file) throws IOException {
        DownloadableChunkResponse response = new DownloadableChunkResponse();
        response.setChunkId(fileChunk.getId());
        response.setChunkIndex(fileChunk.getChunkIndex());
        response.setChunkSize(fileChunk.getChunkSize());
        response.setStatus(fileChunk.getStatus());
        response.setSegmentName(fileChunk.getSegmentName());
        response.setIsParity(fileChunk.getIsParity());
        response.setDownloadUrl(driveService.generateDownloadURI(fileChunk.getDriveFileId()));
        response.setHash(fileChunk.getHash());
        response.setToken(driveService.generateAccessToken(fileChunk.getAccountId()));
        return response;
    }

    @Async
    @Transactional(rollbackFor = Exception.class)
    public void deleteFile(ObjectId fileId, ObjectId userId) throws Exception {
        Files file = getFile(fileId);
        if (file == null) {
            throw new FileNotFoundException("File not found");
        }
        if (!file.getUserId().equals(userId)) {
            throw new UnauthorizedAccessException();
        }

        List<FileChunk> fileChunks = fileChunkService.getFileChunksByFileId(fileId);

        // Run parallel chunk deletions and track success status
        boolean allChunksDeleted = fileChunks.parallelStream().map(chunk -> {
            try {
                fileChunkService.deleteFileChunk(chunk.getId());
                return true;
            } catch (Exception e) {
                return false;
            }
        }).reduce(true, (a, b) -> a && b);

        // If even one chunk failed, abort so the parent File record is NOT deleted
        if (!allChunksDeleted) {
            throw new RuntimeException("One or more chunks failed to delete from Google Drive. Transaction rolled back for retry.");
        }

        fileRepository.delete(file);
    }
}
