package com.shresth.FrankenCloud.DTO;

import com.shresth.FrankenCloud.Entity.Enum.ChunkStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SaveChunkDTO {


    //        frontend will send four things, chunk status, driveFileId(the drive url) and chunk hash
    private ObjectId chunkId;
    private ChunkStatus chunkStatus;
    private String driveFileId;
    private String hash;
}