package com.shresth.FrankenCloud.Controllers;

import com.shresth.FrankenCloud.Entity.DriveAccount;
import com.shresth.FrankenCloud.Entity.UserPrincipal;
import com.shresth.FrankenCloud.Services.DriveService;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/storage")
public class DriveController {

    @Autowired
    private DriveService driveService;

    @PostMapping("/link")
    public ResponseEntity<?> linkStorage(@AuthenticationPrincipal UserPrincipal currentUser, @RequestBody String authCode) throws IOException {
        ObjectId userId = currentUser.getId();
        DriveAccount driveAccount = driveService.linkDrive(userId, authCode);
        return new ResponseEntity<>(driveAccount, HttpStatus.OK);
    }

    //usable storage are the ones that can be used for storing data at T.now() time.
    @GetMapping("/usable")
    public ResponseEntity<?> listUsableStorage(@AuthenticationPrincipal UserPrincipal currentUser) {
        ObjectId userId = currentUser.getId();
        return new ResponseEntity<>(driveService.getDriveAccounts(userId, true, true), HttpStatus.OK);
    }

    //active storage are the ones that are active at T.now() time, inclusive of unusable drives.
    @GetMapping("/accounts")
    public ResponseEntity<?> listActiveStorage(@AuthenticationPrincipal UserPrincipal currentUser) {
        ObjectId userId = currentUser.getId();
        return new ResponseEntity<>(driveService.getDriveAccounts(userId, true), HttpStatus.OK);
    }



    @PutMapping("/delete/{account_id}")
    public ResponseEntity<?> deleteAccount(@AuthenticationPrincipal UserPrincipal currentUser, @PathVariable String account_id) {
        DriveAccount driveAccount = driveService.toggleDriveAccount(currentUser.getId(), account_id);
        return new ResponseEntity<>(driveAccount, HttpStatus.OK);
    }





//    @PostMapping("/link")
//    public ResponseEntity<?> linkStorage(@RequestParam String driveType,

}
