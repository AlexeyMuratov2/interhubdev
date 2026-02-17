# Upload Security Checklist

Single source of truth for all checks performed during file upload. This document describes the policy, threat model, and implementation details.

---

## 0. Definitions

| Term | Description |
|------|-------------|
| **Upload** | Incoming multipart file (stream), metadata (contentType, filename, size), currentUserId |
| **StoredFile** | Storage metadata; file is stored in S3/MinIO |
| **Threat model** | User may upload malicious content; goal is to protect other users, server, and prevent XSS/DoS/data leakage |

---

## 1. What Must Be Checked (Single Source of Truth)

### 1.1 Authentication & Authorization (must)

- Upload endpoint requires authentication (`AuthApi.getCurrentUser`).
- `ensureUploadAllowed` receives user context via `UploadContext` (uploadedBy).
- Policy: any authenticated user may upload (baseline). Future: purpose-based (COURSE_MATERIAL vs HOMEWORK_SUBMISSION) via `UploadContext.purpose()` and `UploadContext.roles()`.
- Reject codes: `UNAUTHORIZED`, `FORBIDDEN_UPLOAD`.

### 1.2 File Size Limits (must)

- Check `context.size()` against `app.document.max-file-size-bytes` (default 50MB).
- Spring `spring.servlet.multipart.max-file-size` also applies (defense in depth).
- Optional (future): max per user per day, rate limiting at API gateway/filters.
- Reject code: `UPLOAD_FILE_TOO_LARGE` → **413 Payload Too Large**.

### 1.3 Allowed MIME Types + Extension Match (must)

- `AllowedFileTypesPolicy` is the single source of truth.
- Normalize `contentType` (no params, lower-case).
- Checks: MIME must be in whitelist; if filename has extension, it must match MIME.
- Note: `contentType` from client can be spoofed → not the only check (see magic bytes).
- Reject codes: `UPLOAD_FORBIDDEN_FILE_TYPE`, `UPLOAD_EXTENSION_MISMATCH` → 400.

### 1.4 Malicious Filename Checks (must)

- `MaliciousFileChecks` enforces:
  - Path traversal (`../`, `/`, `\`)
  - Null bytes
  - Reserved names (Windows: con, prn, aux, nul, com1–9, lpt1–9)
  - Dangerous extensions (exe, bat, cmd, js, jar, sh, etc.)
  - Double extension / masked extension: allow 1–2 dots (e.g. `my.file.pdf`), reject if any extension in chain is dangerous (e.g. `a.pdf.exe`)
  - Trailing dots/spaces, control chars
  - Filename length ≤ 200 chars
- Reject code: `UPLOAD_SUSPICIOUS_FILENAME` → 400.

### 1.5 Magic Bytes / Content Sniffing (strongly recommended)

- `MagicBytesSniffer` reads first 16KB and detects real type by signature:
  - PDF: `%PDF`
  - PNG: `\x89PNG`
  - JPEG: `\xFF\xD8\xFF`
  - GIF: `GIF87a` / `GIF89a`
  - ZIP: `PK..` (docx/xlsx are ZIP)
  - WebP: RIFF..WEBP
- If magic bytes contradict MIME/extension → reject.
- Office OpenXML (docx/xlsx): detected as `application/zip`; allowed when declared MIME is docx/xlsx.
- Reject code: `UPLOAD_CONTENT_TYPE_MISMATCH` → 400.

### 1.6 Antivirus (ClamAV)

- Runs as part of upload security (`UploadSecurityService`).
- Mode: **sync scan before storage** (Option A).
- If infected → reject immediately; nothing written to S3/DB.
- Uses temp file: scan and upload both read from disk.
- Reject codes: `UPLOAD_MALWARE_DETECTED` → 400, `UPLOAD_AV_UNAVAILABLE` → 503 (fail-closed).

---

## 2. Antivirus (ClamAV) Details

### 2.1 Scan Mode

- **Option A (implemented)**: Sync scan before storage. Scan temp file → if CLEAN, upload to S3. If INFECTED or ERROR, reject.
- Designed for future Option C (async scan with PENDING/CLEAN/INFECTED status).

### 2.2 What Antivirus Checks

- Uses clamd (TCP) as daemon, not clamscan per request.
- Sends file content via INSTREAM protocol.
- Responses:
  - CLEAN → allow
  - FOUND → reject as `MALWARE_DETECTED`
  - ERROR/timeout/unavailable → fail-closed: reject with `AV_UNAVAILABLE` (503)

### 2.3 Limits and Safety

- Max file size for scan: same as upload limit.
- Timeout: `clamav.timeout-ms` (default 30s).
- Temp file: written to system temp dir, deleted after upload.
- Archives: allowed if in whitelist (docx, xlsx). ZIP bomb risk: ClamAV has limits; consider future restrictions for unknown archives.

---

## 3. Error Codes and HTTP Status

| HTTP | Code | When |
|------|------|------|
| 400 | UPLOAD_FORBIDDEN_FILE_TYPE | MIME not in whitelist |
| 400 | UPLOAD_EXTENSION_MISMATCH | Extension doesn't match MIME |
| 400 | UPLOAD_SUSPICIOUS_FILENAME | Path traversal, null byte, dangerous ext, etc. |
| 400 | UPLOAD_CONTENT_TYPE_MISMATCH | Magic bytes contradict MIME |
| 400 | UPLOAD_FILE_TOO_LARGE | Size exceeds max (also 413) |
| 400 | UPLOAD_MALWARE_DETECTED | ClamAV found malware |
| 403 | UPLOAD_FORBIDDEN_UPLOAD | User not allowed to upload |
| 413 | UPLOAD_FILE_TOO_LARGE | Size exceeds max |
| 503 | UPLOAD_AV_UNAVAILABLE | ClamAV unavailable (fail-closed) |

---

## 4. Logging & Audit

- Security events logged: userId, filename (sanitized), size, contentType, reason code, detail.
- For MALWARE_DETECTED: signature name logged for admin; not returned to client.
- No file content or sensitive data in logs.

---

## 5. Code Locations

| Check | Location |
|-------|----------|
| Auth | `DocumentController` (AuthApi.getCurrentUser) |
| Size | `UploadSecurityService.ensureUploadAllowed` |
| Filename | `MaliciousFileChecks.checkFilename` |
| MIME/extension | `AllowedFileTypesPolicy.checkAllowed` |
| Magic bytes | `MagicBytesSniffer.detectMimeFromContent` → `UploadSecurityService` |
| Antivirus | `ClamAvAdapter.scan` → `UploadSecurityService` |
| Orchestration | `UploadSecurityService.ensureUploadAllowed` |

---

## 6. Configuration

| Property | Default | Description |
|----------|---------|-------------|
| app.document.max-file-size-bytes | 52428800 (50MB) | Max file size |
| clamav.host | localhost | ClamAV daemon host |
| clamav.port | 3310 | ClamAV daemon port |
| clamav.timeout-ms | 30000 | Socket timeout |
| clamav.enabled | false | Enable antivirus scan |

---

## 7. Docker / Dev Setup

- ClamAV container in `compose.yaml`: `clamav` on port 3310.
- First start: ~60s for signature DB download.
- When `CLAMAV_ENABLED=true`, app connects to clamd. Use host `clamav` when app runs in Docker.
- Health: clamd port 3310 open.

---

## 8. Summary: Full Check Order

1. **Auth** (controller)
2. **Size** (UploadSecurityService)
3. **Filename** (MaliciousFileChecks)
4. **MIME + extension** (AllowedFileTypesPolicy)
5. **Magic bytes** (MagicBytesSniffer, when contentPath provided)
6. **Antivirus** (ClamAvAdapter / NoOpAntivirusAdapter, when contentPath provided)

All rejections go through `UploadSecurityErrors` → `AppException` → global handler → `ErrorResponse`.
