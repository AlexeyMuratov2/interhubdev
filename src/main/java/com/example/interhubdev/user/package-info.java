/**
 * User module - core authentication and authorization domain.
 * 
 * <h2>Public API</h2>
 * <ul>
 *   <li>{@link com.example.interhubdev.user.UserApi} - main interface for user operations</li>
 *   <li>{@link com.example.interhubdev.user.User} - user entity</li>
 *   <li>{@link com.example.interhubdev.user.Role} - user roles enum</li>
 *   <li>{@link com.example.interhubdev.user.UserStatus} - account status enum</li>
 * </ul>
 * 
 * <h2>Usage</h2>
 * Other modules should inject {@link com.example.interhubdev.user.UserApi}:
 * <pre>{@code
 * @Service
 * @RequiredArgsConstructor
 * public class MyService {
 *     private final UserApi userApi;
 *     
 *     public void doSomething() {
 *         User user = userApi.findByEmail("test@example.com").orElseThrow();
 *     }
 * }
 * }</pre>
 * 
 * <h2>Roles hierarchy</h2>
 * <ul>
 *   <li>SUPER_ADMIN - full access, can invite admins</li>
 *   <li>ADMIN - manage users (except admins)</li>
 *   <li>TEACHER - professors and lecturers</li>
 *   <li>STAFF - university employees</li>
 *   <li>STUDENT - international students</li>
 * </ul>
 * 
 * Role-specific data (student info, teacher info) is stored in separate modules
 * using composition pattern (StudentProfile, TeacherProfile, StaffProfile).
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "User"
)
package com.example.interhubdev.user;
