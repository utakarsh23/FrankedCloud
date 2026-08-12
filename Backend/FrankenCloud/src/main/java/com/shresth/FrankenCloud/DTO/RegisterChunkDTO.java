package com.shresth.FrankenCloud.DTO;

import com.shresth.FrankenCloud.Entity.Enum.ChunkStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegisterChunkDTO {

    private ObjectId accountId;
    private Long chunkIndex;
    private String segmentName;
    private Boolean isParity;
    private Long chunkSize;
    private String driveFileId;
    private String hash;
}