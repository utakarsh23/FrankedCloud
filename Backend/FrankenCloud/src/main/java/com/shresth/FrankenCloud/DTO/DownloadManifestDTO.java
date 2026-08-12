package com.shresth.FrankenCloud.DTO;

import com.shresth.FrankenCloud.Entity.Enum.FileType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DownloadManifestDTO {

    private ObjectId fileId;
    private String fileName;
    private Long fileSize;
    private FileType fileType;
    private Long shards;          // N (Data Shards count)
    private Long parityShards;    // K (Parity Shards count)
    private String encryptionKey;
    private List<ChunkLocationDTO> chunks;

    
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChunkLocationDTO {
        private ObjectId chunkId;
        private Long chunkIndex;
        private String segmentName;
        private Boolean isParity;
        private Long chunkSize;
        private ObjectId accountId;
        private String driveFileId;
        private String hash;
        private String status;

    }
}