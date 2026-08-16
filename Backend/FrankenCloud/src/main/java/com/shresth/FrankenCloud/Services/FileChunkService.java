package com.shresth.FrankenCloud.Services;

import com.shresth.FrankenCloud.Config.Exceptions.*;
//import com.shresth.FrankenCloud.DTO.RegisterChunkDTO;
import com.shresth.FrankenCloud.DTO.SaveChunkDTO;
import com.shresth.FrankenCloud.Entity.FileChunk;
import com.shresth.FrankenCloud.Repositories.FileChunkRepository;
import com.shresth.FrankenCloud.Repositories.FileRepository;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FileChunkService {

    @Autowired
    private FileChunkRepository fileChunkRepository;

    @Autowired
    private FileRepository fileRepository;


//    @Transactional
//    public List<FileChunk> registerChunks(List<RegisterChunkDTO> chunkDTOS, ObjectId fileId, ObjectId userId) {
//        Files file = fileRepository.getFilesById(fileId);
//        if (file == null) {
//            throw new RuntimeException("File not found");
//        }
//        if(!file.getUserId().equals(userId)) {
//            throw new UnauthorizedAccessException();
//        }
//        List<FileChunk> chunks = chunkDTOS.stream().map(dto -> {
//            FileChunk chunk = new FileChunk();
//            chunk.setFileId(fileId);
//            chunk.setAccountId(dto.getAccountId());
//            chunk.setChunkIndex(dto.getChunkIndex());
//            chunk.setSegmentName(dto.getSegmentName());
//            chunk.setIsParity(dto.getIsParity());
//            chunk.setChunkSize(dto.getChunkSize());
//            chunk.setDriveFileId(dto.getDriveFileId());
//            chunk.setHash(dto.getHash());
//            chunk.setStatus(ChunkStatus.HEALTHY);
//            return chunk;
////            private ObjectId accountId; //
////            private Long chunkIndex;
////            private String segmentName; //
////            private Boolean isParity;
////            private Long chunkSize; //
////            private String driveFileId;
////            private String hash;
//        }).toList();
//
//        return fileChunkRepository.saveAll(chunks);
//    }

    public List<FileChunk> getFileChunksByFileId(ObjectId fileId) {
        return fileChunkRepository.findByFileIdOrderByChunkIndexAsc(fileId);
    }

    public FileChunk getFileChunkById(ObjectId chunkId) {
        return fileChunkRepository.findById(chunkId).orElseThrow(() -> new FileNotFoundException("File not found"));
    }

    public FileChunk registerFileChunk(SaveChunkDTO saveChunkDTO) {
        FileChunk chunk = getFileChunkById(saveChunkDTO.getChunkId());
        chunk.setStatus(saveChunkDTO.getChunkStatus());
        chunk.setDriveFileId(saveChunkDTO.getDriveFileId());
        chunk.setHash(saveChunkDTO.getHash());
        return fileChunkRepository.save(chunk);
    }
}
