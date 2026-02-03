/**
 * Account module - management of user accounts (own profile and user management by moderators/admins).
 *
 * <h2>Public API</h2>
 * <ul>
 *   <li>{@link com.example.interhubdev.account.AccountApi} - main interface</li>
 *   <li>{@link com.example.interhubdev.account.UpdateProfileRequest} - request for updating own profile</li>
 *   <li>{@link com.example.interhubdev.account.UpdateUserRequest} - request for updating user (profile + roles)</li>
 * </ul>
 *
 * <h2>Access</h2>
 * <ul>
 *   <li>GET/PATCH /api/account/me - any authenticated user (own profile)</li>
 *   <li>GET /api/account/users (cursor pagination, max 30) - MODERATOR, ADMIN, SUPER_ADMIN</li>
 *   <li>GET/PATCH /api/account/users/{id} - MODERATOR, ADMIN, SUPER_ADMIN</li>
 *   <li>DELETE /api/account/users/{id} - only ADMIN, SUPER_ADMIN; SUPER_ADMIN only deletable by another SUPER_ADMIN; nobody can delete self</li>
 * </ul>
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "Account",
    allowedDependencies = {"auth", "user", "student", "teacher", "invitation", "error"}
)
package com.example.interhubdev.account;
