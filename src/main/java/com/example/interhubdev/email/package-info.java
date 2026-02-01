/**
 * Email module - handles sending emails via SMTP.
 * 
 * <h2>Public API</h2>
 * <ul>
 *   <li>{@link com.example.interhubdev.email.EmailApi} - main interface for sending emails</li>
 *   <li>{@link com.example.interhubdev.email.EmailMessage} - email data (immutable, with builder)</li>
 *   <li>{@link com.example.interhubdev.email.EmailResult} - send operation result</li>
 * </ul>
 * 
 * <h2>Features</h2>
 * <ul>
 *   <li>Synchronous and asynchronous sending</li>
 *   <li>Plain text and HTML emails</li>
 *   <li>Template support (for invitation emails, etc.)</li>
 *   <li>CC/BCC support</li>
 * </ul>
 * 
 * <h2>Configuration</h2>
 * Configure via environment variables:
 * <ul>
 *   <li>MAIL_HOST - SMTP server hostname</li>
 *   <li>MAIL_PORT - SMTP server port</li>
 *   <li>MAIL_USERNAME - SMTP username</li>
 *   <li>MAIL_PASSWORD - SMTP password</li>
 *   <li>MAIL_FROM - default sender address</li>
 * </ul>
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "Email"
)
package com.example.interhubdev.email;
