package com.shresth.FrankenCloud.Entity;

import com.shresth.FrankenCloud.Entity.Enum.ChunkStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "file_chunk")
public class FileChunk {

    @Id
    private ObjectId id;
    private ObjectId fileId;
    private ObjectId accountId;
    private Long chunkIndex;
    private String segmentName;
    private Boolean isParity;
    private Long chunkSize;
    private String driveFileId;
    private String hash; //this is for file health and checking if it was corrupted or not.
    private ChunkStatus status;


    @CreatedDate
    private Date createdAt;
    @LastModifiedDate
    private Date updatedAt;
}

