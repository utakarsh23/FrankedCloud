package com.shresth.FrankenCloud.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DriveAccount {

    @Id
    private ObjectId driveId;

    private String accountId;
    private ObjectId userId;
    private String googleEmail;
    private String refreshToken; //encrypted
    private Long usedSpace;
    private Long remainingSpace;
    private Boolean isActive;

    @CreatedDate
    private Date createdAt;
    @LastModifiedDate
    private Date updatedAt;
}

/*

{
  "_id": ObjectId("65c82a1f8f1b2c3d4e5f6a71"),
  "account_id": "acc_gdrive_01",
  "user_id": "usr_99812",
  "google_email": "storage.node1@gmail.com",
  "encrypted_refresh_token": "enc_v1_99a8b7c6...",
  "storage_used_bytes": 8589934592,
  "storage_limit_bytes": 16106127360,
  "is_active": true,
  "created_at": ISODate("2026-07-23T18:00:00Z")
}

*/
