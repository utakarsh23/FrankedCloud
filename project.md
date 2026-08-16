# 🧟 FrankenCloud: Master System Blueprint & Architecture

## 1. System Architecture Overview

FrankenCloud is a virtualized, erasure-coded distributed object store that aggregates multiple dynamic Google Drive accounts into a unified storage pool.

Spring Boot acts as the **control plane, security gatekeeper, metadata coordinator, and token broker**. Storage payloads are ingested directly from the browser/client to Google Drive via **Resumable Upload Session URIs** without taxing Spring Boot server bandwidth.

```text
┌─────────────────────────────────────────────────────────────────────────┐
│                          FRONTEND / BROWSER                             │
│ 1. POST /files/create/metadata (fileName, fileSize, parentFolderId)     │
│ 2. Encrypts payload with fileKey + IV and streams chunks directly       │
│    to Google Drive Resumable Upload Session URIs                        │
│ 3. POST /chunk/upload (chunkId, driveFileId, sha256Hash, status)        │
│ 4. GET /files/metadata/{fileId} -> Retrieves chunks, keys & tokens      │
└────────────────────┬────────────────────────────────────────────────────┘
                     │  
      1. Session     │ 2. Returns Scoped Google Drive
      Negotiation    │    Resumable Upload URIs, chunkIds, keys & IVs
                     ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                       SPRING BOOT 3 (Java 21)                           │
│     Control Plane, Security Gatekeeper & Metadata Coordinator           │
└────────────────────┬────────────────────────────────────────────────────┘
                     │
                     │ 3. Syncs Chunks, Tokens, Storage Quotas & State
                     ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                    METADATA & SECURITY LAYER                            │
│       • MongoDB (users, drive_accounts, files, file_chunk)              │
│       • Spring Security 6 (Stateless JWT + BCrypt + CORS)               │
│       • AES-256 GCM Encrypted OAuth Refresh Tokens                      │
│       • SHA-256 Salted File Encryption Key Generation & 128-bit IV Nonce│
└────────────────────┬────────────────────────────────────────────────────┘
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

### A. Dynamic Multi-Account OAuth & Token Brokerage
* **Account Linking (`POST /storage/link`):** Users link Google Drive accounts via OAuth authorization code.
* **Persistent & Encrypted Storage:** Refresh tokens are AES-256 GCM encrypted (`EncryptionService`) before MongoDB persistence in `drive_accounts`.
* **Token Rotation & In-Memory Caching (`GoogleAuthService`):** Background access tokens are cached in a thread-safe `ConcurrentHashMap` with a 5-minute pre-expiry renewal buffer.
* **Drive Account Toggle (`PUT /storage/delete/{account_id}`):** Toggles active status with strict user ownership validation.

### B. Upload Handshake & Dynamic Shard Allocation
* **Session Handshake (`POST /files/create/metadata`):** Initiates upload with `fileName`, `fileSize`, and optional `parentFolderId`.
* **Storage Floor Safeguard:** Filters drives with a **strict minimum of 1 GB (1,073,741,824 bytes)** free space.
* **Shard Allocation Logic (`FileService`):**
  * $1 \text{ or } 2 \text{ drives} \implies N=1, K=0 \text{ or } 1$
  * $\le 4 \text{ drives} \implies N = M - 1, K = 1$
  * $< 8 \text{ drives} \implies N = M - 2, K = 2$
  * $\ge 8 \text{ drives} \implies N = M - 3, K = 3$
* **Resumable URI Generation (`DriveService`):** Requests Google Drive API v3 to create resumable upload URLs per chunk and persists initial `FileChunk` records in state `UPLOADING`.

### C. Direct Client Upload Finalization (`POST /chunk/upload`)
* After client completes streaming to Google Drive, it notifies backend with `SaveChunkDTO` containing `chunkId`, `driveFileId`, `hash`, and `chunkStatus` (`HEALTHY`).

### D. File Manifest & Secure Download Brokerage (`GET /files/metadata/{fileId}`)
* Validates user ownership (`currentUser.getId()`).
* Retrieves all associated chunks, generates signed direct download URLs (`https://www.googleapis.com/drive/v3/files/{driveFileId}?alt=media`), and injects fresh short-lived OAuth access tokens for each storage node.

### E. Security, Authentication & Cryptography
* **Stateless JWT Security (`JwtAuthenticationFilter`, `SecurityConfig`):** Enforces stateless sessions, extracts `userId` from claims, and populates `UserPrincipal`.
* **CORS Support:** Configured global `CorsConfigurationSource` allowing cross-origin requests from frontend clients.
* **Key Derivation & IV:** `CryptographyService` generates SHA-256 salted encryption keys (`userId:fileId:fileName:email:salt`) and 16-byte random IVs for client-side AES-CTR encryption.

---

## 3. MongoDB Data Model & Entities

### Collection 1: `users`
```json
{
  "_id": ObjectId("65c82a1f8f1b2c3d4e5f6a70"),
  "username": "alex_dev",
  "email": "alex@example.com",
  "password": "$2a$10$e8f9a...",
  "deleted": false,
  "storage": {
    "storageSize": 32212254720,
    "remainingStorage": 15032385536,
    "usedStorage": 17179869184,
    "totalAccounts": 2,
    "activeAccounts": 2
  },
  "created_at": ISODate("2026-08-16T12:00:00Z"),
  "updated_at": ISODate("2026-08-16T12:00:00Z")
}
```

### Collection 2: `drive_accounts`
```json
{
  "_id": ObjectId("65c82a1f8f1b2c3d4e5f6a71"),
  "accountId": "10492837492817491",
  "userId": ObjectId("65c82a1f8f1b2c3d4e5f6a70"),
  "googleEmail": "node1@gmail.com",
  "refreshToken": "enc_v1_99a8b7c6...",
  "usedSpace": 8589934592,
  "remainingSpace": 7516192768,
  "isActive": true,
  "createdAt": ISODate("2026-08-16T12:00:00Z"),
  "updatedAt": ISODate("2026-08-16T12:00:00Z")
}
```

### Collection 3: `files`
```json
{
  "_id": ObjectId("65c82a1f8f1b2c3d4e5f6a72"),
  "userId": ObjectId("65c82a1f8f1b2c3d4e5f6a70"),
  "fileName": "nature.mp4",
  "parentFolderId": null,
  "fileSize": 104857600,
  "fileType": "VIDEO",
  "shards": 3,
  "parityShards": 1,
  "encryptionKey": "Nzg3NGI...",
  "iv": "MGY4MW...",
  "createdAt": ISODate("2026-08-16T12:00:00Z"),
  "updatedAt": ISODate("2026-08-16T12:00:00Z")
}
```

### Collection 4: `file_chunk`
```json
{
  "_id": ObjectId("65c82a1f8f1b2c3d4e5f6a73"),
  "fileId": ObjectId("65c82a1f8f1b2c3d4e5f6a72"),
  "accountId": "10492837492817491",
  "chunkIndex": 0,
  "segmentName": "65c82a1f8f1b2c3d4e5f6a72__chunk_0",
  "isParity": false,
  "chunkSize": 34952534,
  "driveFileId": "1A2b3C4d5E6F7g8H9i0J",
  "hash": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
  "status": "HEALTHY",
  "createdAt": ISODate("2026-08-16T12:00:00Z"),
  "updatedAt": ISODate("2026-08-16T12:05:00Z")
}
```

---

## 4. API Endpoints Reference

| Method | Endpoint | Description | Auth Required |
|---|---|---|---|
| `POST` | `/auth/register` | Register new user with username/email/password | No |
| `POST` | `/auth/login` | Login user, returns signed JWT token | No |
| `POST` | `/storage/link` | Link Google Drive node via OAuth authCode | Yes |
| `GET` | `/storage/usable` | Get user drives with $\ge 1\text{GB}$ available space | Yes |
| `GET` | `/storage/accounts` | Get all active user drives | Yes |
| `PUT` | `/storage/delete/{account_id}` | Toggle active status of a user's drive account | Yes |
| `POST` | `/files/create/metadata` | Initiate upload session, calculate shards & return resumable upload URIs | Yes |
| `GET` | `/files/metadata/{fileId}` | Get download manifest with chunk URLs & access tokens | Yes |
| `POST` | `/files/create/folder` | Create virtual folder directory node | Yes |
| `GET` | `/files/directory` | List files and folders under `parentFolderId` | Yes |
| `POST` | `/chunk/upload` | Finalize chunk upload (`SaveChunkDTO`) | Yes |
| `GET` | `/chunk/{fileId}` | List all chunks for a given file | Yes |

---

## 5. Technology Stack

* **Language & Runtime:** Java 21
* **Framework:** Spring Boot 3.x / 4.x
* **Security:** Spring Security 6, JJWT (0.12.6), BCrypt, AES-256 GCM TextEncryptor
* **Database:** MongoDB (Spring Data MongoDB)
* **Cloud Storage:** Google Drive REST API v3, Google Auth Library OAuth2 HTTP
* **Resilience & Observability:** Resilience4j, Micrometer Prometheus