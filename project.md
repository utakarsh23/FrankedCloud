# 🧟 FrankenCloud: Master System Blueprint

## 1. System Architecture Overview

FrankenCloud is a virtualized, erasure-coded distributed object store that aggregates multiple dynamic Google Drive accounts into a unified 100GB+ storage pool.

Spring Boot acts as a **control plane, security gatekeeper, streaming gateway, and background self-healing engine**. Storage payload streams are ingested via direct-to-cloud resumable URLs (Pattern 3) or processed via Spring Boot's assembly gateway.

```text
┌─────────────────────────────────────────────────────────────────────────┐
│                          FRONTEND / BROWSER                             │
│ 1. Request upload session (fileName, size, mime)                        │
│ 2. Stream/Upload chunks directly to Google Drive Session URIs           │
└────────────────────┬────────────────────────────────────────────────────┘
                     │  
      1. Session     │ 2. Returns Scoped Google Drive
      Negotiation    │    Resumable Upload Session URIs
                     ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                       SPRING BOOT 3 (Java 21+)                          │
│          Control Plane, SSE Gateway & Auto-Healing Engine               │
└────────────────────┬────────────────────────────────────────────────────┘
                     │
                     │ 3. Syncs Chunk Indices, SHA-256 Hashes & Locks
                     ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                    METADATA & OBSERVABILITY LAYER                       │
│       • MongoDB (Files, FileChunks, DriveAccounts, Users)               │
│       • Redis (Redlock Distributed Locks)                               │
│       • Resilience4j (Circuit Breakers for 429 Rate Limits)             │
│       • Prometheus + Grafana (Telemetry)                                │
└─────────────────────────────────────────────────────────────────────────┘
                     │
                     │ 4. Client uploads / downloads chunks DIRECTLY
                     ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                      STORAGE NODE POOL (GOOGLE DRIVES)                  │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │ GDrive Acc 1 │  │ GDrive Acc 2 │  │ GDrive Acc 3 │  │ GDrive... N  │  │
│  │ (Data Chunk0)│  │ (Data Chunk1)│  │ (Data Chunk2)│  │ (Parity K)   │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  └──────────────┘  │
└─────────────────────────────────────────────────────────────────────────┘

```

---

## 2. Core Functional Specifications

### A. Dynamic Multi-Account OAuth Setup

* **Account Linking:** Users dynamically connect and unlink Google Drive storage nodes from their account settings.
* **Persistent Access:** OAuth requests request `access_type=offline` and `prompt=consent`. Spring Boot stores AES-encrypted `refreshToken` credentials in MongoDB (`drive_accounts` collection).
* **Token Rotation:** Background services exchange refresh tokens for 1-hour access tokens before generating presigned upload session URLs.

### B. Upload Negotiation & Load Balancing (60 / 20 / 20 Allocation Rule)

* **Session Handshake:** The browser initiates an upload session with metadata (`fileName`, `fileSize`, `fileType`).
* **Storage Floor Safeguard:** Spring Boot filters usable drives to ensure every storage node maintains a **strict minimum of 1 GB (1,073,741,824 bytes)** free space, preventing Google API `403 Storage Quota Exceeded` write errors.
* **Fractional Knapsack Shard Calculator:**
* **$M \le 8$ drives:** Scales from mirroring ($N=1, K=1$) to high tolerance ($N = M - 2, K = 2$).
* **$M \ge 9$ drives:** Enforces a **60% Data ($N$), 20% Parity ($K$), and 20% Standby Buffer** ratio. The 20% standby buffer reserves dynamic failover capacity for background self-healing without requiring users to add new accounts immediately.


* **Presigned Ingestion:** Spring Boot queries Google Drive REST APIs to generate **Resumable Upload Session URIs** for target nodes and returns the instructions to the client.

### C. Server-Managed Encryption & Auto-Healing (SSE)

* **Server-Side Encryption (SSE):** Keys are generated and managed per file by the Spring Boot control plane.
* **Autonomous Background Self-Healing:** Because the control plane maintains access keys, `@Scheduled` auditing workers can independently fetch surviving $N$ shards, run `JavaReedSolomon` in Virtual Threads to reconstruct lost or rotten chunks (e.g., if a Google account dies), and re-upload them to a healthy standby node without user intervention.

### D. MPEG-DASH & HLS Video Streaming (GOP-Batched)

* **Assembly Gateway:** Spring Boot acts as an assembly gateway for video streams.
* **GOP Segment Batching:** Video uploads are transcoded via FFmpeg into 2-second `.m4s` fragments grouped into 10-second GOP batches (~2.5 MB payloads).
* **Low-Latency Seeking:** Shaka Player requests `.mpd` manifests. Spring Boot uses **Java 21 Virtual Threads** to concurrently fetch surviving shards, decrypt them in memory, and stream standard byte ranges into the browser.

### E. JWT Stateless Session & Authentication Engine

* **Stateless Security:** Configured `SessionCreationPolicy.STATELESS` in Spring Security 6 to eliminate server-side HTTP sessions (JSESSIONID).
* **Cryptographic Signatures & Claims:** Issues HMAC-SHA256 signed JWT tokens via `JwtUtils` containing custom claims (`userId`) upon registration and login.
* **UserPrincipal Context:** Implemented `UserPrincipal` (`UserDetails`) to store the user's MongoDB `ObjectId id`.
* **Filter Interception & Extraction:** Implemented `JwtAuthenticationFilter` (`OncePerRequestFilter`) to intercept HTTP requests, validate Bearer tokens, and populate `SecurityContextHolder`. Controllers access the authenticated user's ID cleanly via `@AuthenticationPrincipal UserPrincipal currentUser` without relying on frontend payload claims.

---

## 3. MongoDB Data Architecture

To prevent document array bloat and ensure high-throughput indexing, metadata is split into four distinct collections.

```text
       ┌──────────────┐                 ┌─────────────────┐
       │    User      │                 │  DriveAccount   │
       └──────┬───────┘                 └────────┬────────┘
              │ 1                                │ 1
              │                                  │
              │ N                                │ N
       ┌──────┴───────┐                 ┌────────┴────────┐
       │    Files     │1               N│    FileChunk    │
       └──────────────┴─────────────────┴─────────────────┘

```

### Collection 1: `users`

```json
{
  "_id": ObjectId("65c82a1f8f1b2c3d4e5f6a70"),
  "username": "alex_dev",
  "email": "alex@example.com",
  "password": "$2a$12$e8f9a...",
  "deleted": false,
  "created_at": ISODate("2026-07-23T18:00:00Z"),
  "updated_at": ISODate("2026-07-23T18:00:00Z")
}

```

### Collection 2: `drive_accounts`

```json
{
  "_id": ObjectId("65c82a1f8f1b2c3d4e5f6a71"),
  "account_id": "acc_gdrive_01",
  "user_id": ObjectId("65c82a1f8f1b2c3d4e5f6a70"),
  "google_email": "storage.node1@gmail.com",
  "refresh_token": "enc_v1_99a8b7c6...",
  "used_space_bytes": 8589934592,
  "remaining_space_bytes": 7516192768,
  "is_active": true,
  "created_at": ISODate("2026-07-23T18:00:00Z"),
  "updated_at": ISODate("2026-07-23T18:00:00Z")
}

```

### Collection 3: `files`

```json
{
  "_id": ObjectId("65c82a1f8f1b2c3d4e5f6a72"),
  "user_id": ObjectId("65c82a1f8f1b2c3d4e5f6a70"),
  "file_name": "nature.mp4",
  "file_path": "/videos/nature.mp4",
  "file_size_bytes": 104857600,
  "file_type": "VIDEO",
  "data_shards": 4,
  "parity_shards": 2,
  "encryption_key": "enc_key_a7f3b89c0d1e2f3a...",
  "created_at": ISODate("2026-07-23T18:00:00Z"),
  "updated_at": ISODate("2026-07-23T18:00:00Z")
}

```

### Collection 4: `file_chunks`

```json
{
  "_id": ObjectId("65c82a1f8f1b2c3d4e5f6a73"),
  "file_id": ObjectId("65c82a1f8f1b2c3d4e5f6a72"),
  "account_id": ObjectId("65c82a1f8f1b2c3d4e5f6a71"),
  "chunk_index": 0,
  "segment_name": "720p_seg_001.m4s",
  "is_parity": false,
  "chunk_size_bytes": 16777216,
  "drive_file_id": "1A2b3C4d5E6F7g8H9i0J",
  "sha256_hash": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
  "status": "HEALTHY",
  "created_at": ISODate("2026-07-23T18:00:00Z"),
  "updated_at": ISODate("2026-07-23T18:05:00Z")
}

```

### Essential MongoDB Indexes

```javascript
// High-throughput query for video chunk assembly & playback
db.file_chunks.createIndex({ "file_id": 1, "chunk_index": 1 });

// Background audit & node-draining lookup
db.file_chunks.createIndex({ "account_id": 1, "status": 1 });

// User storage node lookup with active status
db.drive_accounts.createIndex({ "user_id": 1, "is_active": 1 });

// Storage capacity query index for upload negotiation
db.drive_accounts.createIndex({ "user_id": 1, "is_active": 1, "remaining_space_bytes": 1 });

```

---

## 4. Java 21 Spring Boot Entities

Below are the ready-to-use entity implementations with MongoDB annotations, explicit field mappings, and appropriate collection structures.

### `User.java`

```java
package com.shresth.FrankenCloud.Entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
public class User {

    @Id
    private ObjectId id;

    private String username;

    private String email;

    private String password; // Hashed password (e.g. BCrypt)

    private Boolean deleted = false;

    @CreatedDate
    @Field("created_at")
    private Date createdAt;

    @LastModifiedDate
    @Field("updated_at")
    private Date updatedAt;
}

```

### `DriveAccount.java`

```java
package com.shresth.FrankenCloud.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "drive_accounts")
public class DriveAccount {

    @Id
    private ObjectId id;

    @JsonIgnore
    @Field("user_id")
    private ObjectId userId;

    @Field("account_id")
    private String accountId;

    @Field("google_email")
    private String googleEmail;

    @Field("refresh_token")
    private String refreshToken;

    @Field("used_space_bytes")
    private Long usedSpace;

    @Field("remaining_space_bytes")
    private Long remainingSpace;

    @Field("is_active")
    private Boolean isActive = true;

    @CreatedDate
    @Field("created_at")
    private Date createdAt;

    @LastModifiedDate
    @Field("updated_at")
    private Date updatedAt;
}

```

### `Files.java`

```java
package com.shresth.FrankenCloud.Entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "files")
public class Files {

    @Id
    private ObjectId id;

    @Field("user_id")
    private ObjectId userId;

    @Field("file_name")
    private String fileName;
    

    @Field("file_size_bytes")
    private Long fileSize;

    @Field("file_type")
    private FileType fileType;

    @Field("data_shards")
    private Long shards; // N data shards

    @Field("parity_shards")
    private Long parityShards; // K parity shards

    @Field("encryption_key")
    private String encryptionKey; // System-managed encryption key for background self-healing

    @CreatedDate
    @Field("created_at")
    private Date createdAt;

    @LastModifiedDate
    @Field("updated_at")
    private Date updatedAt;
}

```

### `FileChunk.java`

```java
package com.shresth.FrankenCloud.Entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "file_chunks")
public class FileChunk {

    @Id
    private ObjectId id;

    @Field("file_id")
    private ObjectId fileId;

    @Field("account_id")
    private ObjectId accountId;

    @Field("chunk_index")
    private Long chunkIndex;

    @Field("segment_name")
    private String segmentName; // e.g., "720p_seg_001.m4s"

    @Field("is_parity")
    private Boolean isParity;

    @Field("chunk_size_bytes")
    private Long chunkSize;

    @Field("drive_file_id")
    private String driveFileId;

    @Field("sha256_hash")
    private String hash; // Integrity health checking

    @Field("status")
    private String status; // "HEALTHY", "ROTTEN", or "DRAINING"

    @CreatedDate
    @Field("created_at")
    private Date createdAt;

    @LastModifiedDate
    @Field("updated_at")
    private Date updatedAt;
}

```

### `FileType.java`

```java
package com.shresth.FrankenCloud.Entity;

public enum FileType {
    VIDEO,
    IMAGE,
    PDF,
    ZIP,
    OTHER
}

```

---

## 5. Node Draining Protocol & Auto-Healing

When a user unlinks a Google Drive account, the control plane initiates a zero-downtime node drain:

```text
[ User Requests Unlink of Account_03 ] 
                │
                ▼
      Set status = "DRAINING"  ──► Block new uploads to Account_03
                │
                ▼
   [ Background Self-Healing Worker ]
   1. Query db.file_chunks.find({ account_id: "Account_03" })
   2. Fetch surviving N shards from active remaining accounts
   3. Reconstruct missing shard in Spring Boot RAM (JavaReedSolomon)
   4. Upload reconstructed shard to a healthy Active Account
   5. Update MongoDB file_chunks document with new account_id & HEALTHY status
                │
                ▼
      Set status = "DELETED"   ──► Revoke OAuth Token Safely

```


5. How MPEG-DASH / HLS Video Streaming Works in Action

If you want to support smooth video seeking and adaptive bitrate buffering, here is how the architecture handles it:
Plaintext

┌────────────────┐              ┌────────────────┐              ┌────────────────┐
│ HTML5 Video    │              │ Frontend MSE   │              │ FrankenCloud   │
│ Player         │              │ ServiceWorker  │              │ Backend        │
└───────┬────────┘              └───────┬────────┘              └───────┬────────┘
│                               │                               │
│ 1. Seek to 00:30 (Byte 10MB)  │                               │
├──────────────────────────────►│                               │
│                               │ 2. GET /files/{id}/stream     │
│                               │    ?startChunk=2&limit=2      │
│                               ├──────────────────────────────►│
│                               │                               │
│                               │ 3. Returns Manifest for       │
│                               │    Chunks #2 & #3 + Key       │
│                               │◄──────────────────────────────┤
│                               │                               │
│                               │ 4. Fetch encrypted binary from│
│                               │    Google Drive (driveFileId) │
│                               │ 5. Decrypt in Web Worker      │
│                               │                               │
│ 6. appendBuffer(decryptedData)│                               │
│◄──────────────────────────────┤                               │

---

## 6. Updated Resume Reference
 
```text
PROJECTS
__________________________________________________________________________________________
FrankenCloud | Distributed Erasure-Coded Multi-Cloud Storage Platform
Spring Boot 3, Java 21, MongoDB, JavaReedSolomon, Redis, OAuth 2.0, Resilience4j

• Architected a virtualized 100GB+ object storage engine in Spring Boot 3 & MongoDB, aggregating N isolated Google Drive REST APIs into a unified multi-tenant filesystem.
• Engineered an automated background self-healing engine leveraging JavaReedSolomon and Java 21 Virtual Threads to audit chunk integrity (SHA-256) and reconstruct missing shards upon node failure.
• Implemented Pattern 3 Presigned Resumable Upload Session URIs to enable direct-to-cloud payload ingestion with zero Spring Boot bandwidth overhead.
• Designed an MPEG-DASH adaptive bitrate streaming gateway with GOP segment batching, streaming video fragments into Shaka Player via parallel Virtual Threads.
• Developed a Fractional Knapsack load balancer implementing a 60/20/20 storage ratio (N data, K parity, dynamic hot-standby buffer) with a 1GB per-node safety floor to optimize write allocation.
• Built a Node Draining Protocol to execute zero-downtime shard migrations during storage account unlinking.
• Integrated Resilience4j circuit breakers and Redis distributed locks (Redlock) to eliminate race conditions and gracefully manage 429 API rate limits.

```

//for video files we'll use RS and will use streaming. and AES-CTR algo.