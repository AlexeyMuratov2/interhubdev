/**
 * Upload security sublayer: answers "are we allowed to accept this file?"
 *
 * <p>Entry point: {@link com.example.interhubdev.document.UploadSecurityPort} (implemented by
 * {@link UploadSecurityService}). Used at the start of the upload flow in document module.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>{@link AllowedFileTypesPolicy} — whitelist of MIME types and permitted extensions; extension vs content type match</li>
 *   <li>{@link MaliciousFileChecks} — path traversal, null bytes, double extension, reserved names, dangerous extensions</li>
 *   <li>{@link MagicBytesSniffer} — content sniffing (magic bytes) to validate declared MIME</li>
 *   <li>{@link AntivirusPort} — ClamAV scan via {@link ClamAvAdapter} (or {@link NoOpAntivirusAdapter} when disabled)</li>
 * </ul>
 *
 * <p>Errors are thrown via {@link UploadSecurityErrors} as {@link com.example.interhubdev.error.AppException}.
 */
package com.example.interhubdev.document.internal.uploadSecurity;
