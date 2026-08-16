package com.shresth.FrankenCloud.DTO;

import com.shresth.FrankenCloud.Entity.Enum.ChunkStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileChunkResponse {


    private ObjectId fileId;
    private String accountId;
    private Long chunkIndex;
    private String segmentName;
    private Boolean isParity;
    private Long chunkSize;
    private String driveFileURI;
    private String encryptionKey;
    private String iv;
    private ChunkStatus status;
}
