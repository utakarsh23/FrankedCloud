package com.shresth.FrankenCloud.Controllers;

import com.google.api.client.json.Json;
import com.mongodb.lang.Nullable;
import com.shresth.FrankenCloud.Config.Exceptions.*;
import com.shresth.FrankenCloud.DTO.FileChunkResponse;
import com.shresth.FrankenCloud.DTO.FileDownloadManifestResponse;
import com.shresth.FrankenCloud.DTO.FileRequest;
import com.shresth.FrankenCloud.DTO.FolderRequest;
import com.shresth.FrankenCloud.Entity.Files;
import com.shresth.FrankenCloud.Entity.UserPrincipal;
import com.shresth.FrankenCloud.Services.DriveService;
import com.shresth.FrankenCloud.Services.FileService;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.shresth.FrankenCloud.Services.FileService.UploadPreparation;

import java.io.IOException;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/files")
public class FileController {

    @Autowired
    private FileService fileService;


    @PostMapping("/create/metadata")
    public ResponseEntity<?> uploadFileMetadata(@RequestBody FileRequest fileRequest,
                                               @RequestParam(required = false) ObjectId parentFolderId,
                                               @AuthenticationPrincipal UserPrincipal currentUser) throws IOException {
        ObjectId userId = currentUser.getId();

        List<FileChunkResponse> chunksMetadata = fileService.getChunksMetadata(userId, parentFolderId, currentUser.getEmail(), fileRequest);
        return new ResponseEntity<>(chunksMetadata, HttpStatus.OK);
    }

    @GetMapping("/metadata/{fileId}")
    public ResponseEntity<?> fileMetadata(@PathVariable ObjectId fileId,
                                          @AuthenticationPrincipal UserPrincipal currentUser) {

        FileDownloadManifestResponse fileMetadata = fileService.getFileMetadata(fileId, currentUser.getId());
        return new ResponseEntity<>(fileMetadata, HttpStatus.OK);
    }


    @PostMapping("/create/folder")
    public ResponseEntity<?> createFolder(@RequestBody FolderRequest folderRequest,
                                          @RequestParam(required = false) ObjectId parentFolderId,
                                          @AuthenticationPrincipal UserPrincipal currentUser) {

        ObjectId userId = currentUser.getId();
        Files folder = fileService.createFolder(userId, parentFolderId, folderRequest.getFolderName());

        return ResponseEntity.ok(folder);
    }


    @GetMapping("/directory")
    public ResponseEntity<?> getDirectoryItems(@RequestParam(required = false) ObjectId parentFolderId,
                                        @AuthenticationPrincipal UserPrincipal currentUser) {
        ObjectId userId = currentUser.getId();
        List<Files> files = fileService.currentDirectoryItems(userId, parentFolderId);
        return new ResponseEntity<>(files, HttpStatus.OK);

    }

}
