package com.shresth.FrankenCloud.DTO;

import com.shresth.FrankenCloud.Entity.Enum.ChunkStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DownloadableChunkResponse {
    private ObjectId chunkId;
    private Long chunkIndex;
    private String segmentName;
    private Boolean isParity;
    private Long chunkSize;
    private String driveFileId;     // Google Drive File ID (e.g. "1A2b3C...")
    private String downloadUrl;     // Pre-formatted: https://www.googleapis.com/drive/v3/files/{driveFileId}?alt=media
    private String hash;
    private ChunkStatus status;
    private String accessToken;     // Short-lived OAuth access token for this drive account
}