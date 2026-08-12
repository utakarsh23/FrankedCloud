package com.shresth.FrankenCloud.Controllers;

import com.shresth.FrankenCloud.DTO.DownloadManifestDTO;
import com.shresth.FrankenCloud.Entity.UserPrincipal;
import com.shresth.FrankenCloud.Services.FileDownloadService;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/files")
public class FileDownloadController {

    @Autowired
    private FileDownloadService fileDownloadService;

    @GetMapping("/{fileId}/manifest")
    public ResponseEntity<?> getDownloadManifest(@AuthenticationPrincipal UserPrincipal currentUser,
                                                 @PathVariable ObjectId fileId) {
        ObjectId userId = currentUser.getId();
        DownloadManifestDTO manifest = fileDownloadService.getDownloadManifest(fileId, userId);
        return new ResponseEntity<>(manifest, HttpStatus.OK);
    }

}
