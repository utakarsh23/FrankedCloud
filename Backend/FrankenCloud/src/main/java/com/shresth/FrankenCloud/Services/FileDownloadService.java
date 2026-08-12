package com.shresth.FrankenCloud.Services;

import com.shresth.FrankenCloud.DTO.DownloadManifestDTO;
import com.shresth.FrankenCloud.DTO.DownloadManifestDTO.*;
import com.shresth.FrankenCloud.DTO.RegisterChunkDTO;
import com.shresth.FrankenCloud.Entity.Enum.ChunkStatus;
import com.shresth.FrankenCloud.Entity.Enum.FileType;
import com.shresth.FrankenCloud.Entity.FileChunk;
import com.shresth.FrankenCloud.Entity.Files;
import com.shresth.FrankenCloud.Repositories.FileChunkRepository;
import com.shresth.FrankenCloud.Repositories.FileRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FileDownloadService {

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private FileChunkRepository fileChunkRepository;


    public DownloadManifestDTO getDownloadManifest(ObjectId fileId, ObjectId userId) {
        Files file = validateUsers(fileId, userId);
        List<FileChunk> allChunk = fileChunkRepository.findByFileIdOrderByChunkIndexAsc(fileId);
        return buildManifest(file, allChunk);
    }

    public DownloadManifestDTO getDownloadManifest(ObjectId fileId, ObjectId userId, Long startChunk, Integer limit) {
        Files file = validateUsers(fileId, userId);
        List<FileChunk> allChunk = fileChunkRepository.findByFileIdOrderByChunkIndexAsc(fileId);
        Long endChunk = startChunk + limit;
        List<FileChunk> streamedChunks = allChunk.stream()
                .filter(c -> c.getChunkIndex() >= startChunk && c.getChunkIndex() < endChunk)
                .toList();

        return buildManifest(file, streamedChunks);
    }



    private DownloadManifestDTO buildManifest(Files file, List<FileChunk> chunks) {
        List<ChunkLocationDTO> chunkDTOS = chunks.stream().map(
                c -> new ChunkLocationDTO(
                        c.getId(),
                        c.getChunkIndex(),
                        c.getSegmentName(),
                        c.getIsParity(),
                        c.getChunkSize(),
                        c.getAccountId(),
                        c.getDriveFileId(),
                        c.getHash(),
                        c.getStatus() != null ? c.getStatus().name() : ChunkStatus.HEALTHY.name()
                )
        ).toList();

        return new DownloadManifestDTO(
                file.getId(),
                file.getFileName(),
                file.getFileSize(),
                file.getFileType(),
                file.getShards(),
                file.getParityShards(),
                file.getEncryptionKey(),
                chunkDTOS
        );
    }

    private Files validateUsers(ObjectId fileId, ObjectId userId) {
        Files files = fileRepository.findById(fileId).orElseThrow(() -> new RuntimeException("File not found"));
        if (!userId.equals(files.getUserId())) {
            throw new RuntimeException("Unauthorized access");
        }
        return files;
    }

}
