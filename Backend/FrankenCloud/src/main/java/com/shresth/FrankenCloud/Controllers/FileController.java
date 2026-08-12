package com.shresth.FrankenCloud.Controllers;

import com.mongodb.lang.Nullable;
import com.shresth.FrankenCloud.Config.Exceptions.*;
import com.shresth.FrankenCloud.DTO.FileRequest;
import com.shresth.FrankenCloud.DTO.FolderRequest;
import com.shresth.FrankenCloud.Entity.Files;
import com.shresth.FrankenCloud.Entity.UserPrincipal;
import com.shresth.FrankenCloud.Services.FileService;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/files")
public class FileController {

    @Autowired
    private FileService fileService;


    @PostMapping("/create/metadata")
    public ResponseEntity<?> uploadFileMetadata(@RequestBody FileRequest fileRequest,
                                               @RequestParam(required = false) ObjectId parentFolderId,
                                               @AuthenticationPrincipal UserPrincipal currentUser) {
        ObjectId userId = currentUser.getId();

        Files file = fileService.createUploadMetadata(
                    userId,
                    parentFolderId,
                    fileRequest.getFileName(),
                    currentUser.getEmail(),
                    fileRequest.getFileSize()
        );
        return ResponseEntity.ok(file);
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
