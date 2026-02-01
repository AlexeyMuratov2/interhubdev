/**
 * Error handling module.
 * <p>
 * Provides a unified way to signal and handle application errors in REST API:
 * <ul>
 *   <li>{@link com.example.interhubdev.error.AppException} — base exception with HTTP status and error code</li>
 *   <li>{@link com.example.interhubdev.error.ErrorResponse} — standard JSON body for error responses</li>
 *   <li>{@link com.example.interhubdev.error.Errors} — static API for throwing errors from services and controllers</li>
 * </ul>
 * All exceptions are handled by {@link com.example.interhubdev.error.internal.GlobalExceptionHandler}.
 */
package com.example.interhubdev.error;
