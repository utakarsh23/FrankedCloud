package com.shresth.FrankenCloud.Controllers;

import com.shresth.FrankenCloud.DTO.RegisterChunkDTO;
import com.shresth.FrankenCloud.Entity.FileChunk;
import com.shresth.FrankenCloud.Entity.User;
import com.shresth.FrankenCloud.Entity.UserPrincipal;
import com.shresth.FrankenCloud.Services.FileChunkService;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/chunk")
public class FileChunkController {

    @Autowired
    private FileChunkService fileChunkService;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadChunkMetadata(@RequestBody List<RegisterChunkDTO> registerChunkDTO,
                                                  @RequestParam ObjectId fileId,
                                                  @AuthenticationPrincipal UserPrincipal currentUser) {

        ObjectId userId = currentUser.getId();
        List<FileChunk> chunks = fileChunkService.registerChunks(registerChunkDTO, fileId, userId);

        return ResponseEntity.ok(chunks);
    }


    @GetMapping("/{fileId}")
    public ResponseEntity<?> getFileChunk(@PathVariable ObjectId fileId,
                                          @AuthenticationPrincipal UserPrincipal currentUser) {
        List<FileChunk> fileChunksByFileId = fileChunkService.getFileChunksByFileId(fileId);
        return ResponseEntity.ok(fileChunksByFileId);
    }

}
