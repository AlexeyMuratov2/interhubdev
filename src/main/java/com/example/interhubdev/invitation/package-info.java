/**
 * Invitation module - handles user invitations and account activation.
 * 
 * <h2>Public API</h2>
 * <ul>
 *   <li>{@link com.example.interhubdev.invitation.InvitationApi} - main interface</li>
 *   <li>{@link com.example.interhubdev.invitation.InvitationDto} - invitation data</li>
 *   <li>{@link com.example.interhubdev.invitation.InvitationStatus} - status enum</li>
 *   <li>{@link com.example.interhubdev.invitation.CreateInvitationRequest} - create request</li>
 *   <li>{@link com.example.interhubdev.invitation.TokenValidationResult} - token validation result</li>
 * </ul>
 * 
 * <h2>Invitation Flow</h2>
 * <ol>
 *   <li>Admin creates invitation with user data</li>
 *   <li>User is created with PENDING status</li>
 *   <li>Invitation email is sent (with retry on failure)</li>
 *   <li>User clicks link, token is validated</li>
 *   <li>User sets password, account is activated</li>
 * </ol>
 * 
 * <h2>Two-level Expiration</h2>
 * <ul>
 *   <li>Invitation: 3 months validity</li>
 *   <li>Token: 24 hours validity (auto-regenerated if expired but invitation valid)</li>
 * </ul>
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "Invitation",
    allowedDependencies = {"auth", "user", "student", "teacher", "email"}
)
package com.example.interhubdev.invitation;
