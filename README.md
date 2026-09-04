# 🧟 FrankenCloud

> Distributed erasure-coded virtual object storage engine powered by Spring Boot 3, Java 21, and dynamic Google Drive storage node pools.

---

## ⚡ Architecture & Master Blueprint

1. **Storage & Sharding Engine:**
   * **Reed-Solomon ($n+k$):** Dynamic allocation where $k$ scales up to 3 parity shards for 8+ drives (tolerates up to 3 banned/dead accounts without data loss).
   * **Continuous Slicing (No Striping):** Files are cut into $n$ continuous byte streams ($\lceil \text{File Size} / n \rceil$), keeping Google Drive API usage at $n+k$ total calls per file to prevent rate limits.
   * **Zero Hot Spares:** 100% active storage across all linked drives.

2. **Video Streaming Architecture:**
   * **No HLS Required:** Avoids generating thousands of `.ts` files and hitting API quotas.
   * **MSE + Fetch Streams:** Pipes video bytes directly from Google's `alt=media` endpoint into an HTML5 `<video>` element.
   * **HTTP Range Requests:** Enables fast scrub seeking via:
     $$\text{Target Drive} = \lfloor \text{Byte Offset} / \text{Shard Size} \rfloor$$
     $$\text{Drive Offset} = \text{Byte Offset} \pmod{\text{Shard Size}}$$

3. **Cryptography & Security:**
   * **AES-CTR (Counter Mode):** Stream cipher for byte-by-byte instant stream decryption as network packets arrive.
   * **Atomic Key & IV Generation:** Backend generates a 16-byte random IV and SHA-256 salted key on upload init.
   * **Short-Lived Access Tokens:** Backend brokers 1-hour OAuth access tokens in the manifest; frontend streams directly from Google at $0 backend bandwidth cost.

4. **Fault Recovery & Self-Healing:**
   * **Client-Side Real-Time Healing:** If a drive returns 404 mid-stream, client fetches surviving $n-1$ data shards + 1 parity shard in parallel, performs RS recovery in WebAssembly/JS, and resumes playback seamlessly.
   * **Backend Self-Healing:** Prompts user to link a replacement drive, computes missing shards in the background, and writes them to the new account.

---

## 📡 REST API Reference

| Method | Endpoint | Description | Auth Required |
|---|---|---|---|
| `POST` | `/auth/register` | Register new user with username/email/password | No |
| `POST` | `/auth/login` | Login user, returns signed JWT token | No |
| `POST` | `/storage/link` | Link Google Drive node via OAuth authCode | Yes |
| `GET` | `/storage/usable` | Get user drives with $\ge 1\text{ GB}$ available space | Yes |
| `GET` | `/storage/accounts` | Get all active user drives | Yes |
| `PUT` | `/storage/delete/{account_id}` | Toggle active status of a user's drive account | Yes |
| `POST` | `/files/create/metadata` | Initiate upload session, calculate shards & return resumable upload URIs | Yes |
| `GET` | `/files/metadata/{fileId}` | Get download manifest with chunk URLs & access tokens | Yes |
| `POST` | `/files/create/folder` | Create virtual folder directory node | Yes |
| `GET` | `/files/directory` | List files and folders under `parentFolderId` | Yes |
| `POST` | `/chunk/upload` | Finalize chunk upload (`SaveChunkDTO`) | Yes |
| `GET` | `/chunk/{fileId}` | List all chunks for a given file | Yes |
| `DELETE` | `/files/{fileId}` | Delete file/folder and all associated chunks from Google Drive & MongoDB | Yes |

---

## 🚀 Getting Started

### Prerequisites
* Java 21+
* Maven 3.9+
* MongoDB running on `mongodb://localhost:27017`

### Run Backend
```bash
cd Backend/FrankenCloud
mvn clean spring-boot:run
```
