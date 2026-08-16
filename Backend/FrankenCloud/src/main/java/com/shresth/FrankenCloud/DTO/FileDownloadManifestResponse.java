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
public class FileDownloadManifestResponse {
    private ObjectId fileId;
    private String fileName;
    private Long fileSize;
    private FileType fileType;
    private Long dataShards;
    private Long parityShards;
    private String encryptionKey;  // AES-CTR Key
    private String iv;             // AES-CTR Initialization Vector (Nonce)
    private List<DownloadableChunkResponse> chunks;
}