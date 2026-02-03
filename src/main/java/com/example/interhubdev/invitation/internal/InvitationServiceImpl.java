package com.example.interhubdev.invitation.internal;

import com.example.interhubdev.email.EmailApi;
import com.example.interhubdev.email.EmailMessage;
import com.example.interhubdev.email.EmailResult;
import com.example.interhubdev.error.Errors;
import com.example.interhubdev.invitation.*;
import com.example.interhubdev.invitation.internal.InvitationErrors;
import com.example.interhubdev.student.StudentApi;
import com.example.interhubdev.teacher.TeacherApi;
import com.example.interhubdev.user.Role;
import com.example.interhubdev.user.UserApi;
import com.example.interhubdev.user.UserDto;
import com.example.interhubdev.user.UserStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of InvitationApi.
 */
@Service
@RequiredArgsConstructor
@EnableConfigurationProperties(InvitationProperties.class)
@Transactional(readOnly = true)
@Slf4j
class InvitationServiceImpl implements InvitationApi {

    private final InvitationRepository invitationRepository;
    private final InvitationTokenRepository tokenRepository;
    private final UserApi userApi;
    private final StudentApi studentApi;
    private final TeacherApi teacherApi;
    private final EmailApi emailApi;
    private final TaskScheduler taskScheduler;
    private final InvitationProperties properties;

    // ==================== Query methods ====================

    @Override
    public Optional<InvitationDto> findById(UUID id) {
        return invitationRepository.findById(id).map(this::toDto);
    }

    @Override
    public Optional<InvitationDto> findByUserId(UUID userId) {
        return invitationRepository.findByUserId(userId).map(this::toDto);
    }

    @Override
    public List<InvitationDto> findAll() {
        List<Invitation> invitations = invitationRepository.findAll();
        return toDtoList(invitations);
    }

    @Override
    public List<InvitationDto> findByStatus(InvitationStatus status) {
        List<Invitation> invitations = invitationRepository.findByStatus(status);
        return toDtoList(invitations);
    }

    @Override
    public List<InvitationDto> findByInvitedBy(UUID adminId) {
        List<Invitation> invitations = invitationRepository.findByInvitedById(adminId);
        return toDtoList(invitations);
    }

    // ==================== Command methods ====================

    @Override
    @Transactional
    public InvitationDto create(CreateInvitationRequest request, UUID invitedBy) {
        Set<Role> effectiveRoles = request.getEffectiveRoles();
        if (effectiveRoles.isEmpty()) {
            throw InvitationErrors.roleRequired();
        }
        Role.validateAtMostOneStaffType(effectiveRoles);

        // 1. Validate inviter can invite each role
        for (Role targetRole : effectiveRoles) {
            validateInviterPermission(invitedBy, targetRole);
        }

        Optional<UserDto> existingUser = userApi.findByEmail(request.email());
        if (existingUser.isPresent()) {
            return reinviteExistingUser(existingUser.get(), request, invitedBy);
        }

        // 2. Create user with PENDING status (roles from invitation)
        UserDto user = userApi.createUser(
                request.email(),
                effectiveRoles,
                request.firstName(),
                request.lastName()
        );

        // 3. Create role-specific profiles if needed
        createRoleProfiles(user.id(), request);

        // 4. Create invitation
        Invitation invitation = Invitation.builder()
                .userId(user.id())
                .invitedById(invitedBy)
                .status(InvitationStatus.PENDING)
                .expiresAt(Instant.now().plus(properties.getExpiryDuration()))
                .build();

        invitation = invitationRepository.save(invitation);

        // 5. Create token and send email
        createToken(invitation.getId());
        sendInvitationEmailAsync(invitation.getId(), 1);

        log.info("Created invitation {} for user {} (roles: {})",
                invitation.getId(), user.email(), effectiveRoles);

        return toDto(invitation);
    }

    /**
     * Re-invite when user already exists and their invitation is EXPIRED or CANCELLED.
     */
    private InvitationDto reinviteExistingUser(UserDto user, CreateInvitationRequest request, UUID invitedBy) {
        Invitation invitation = invitationRepository.findByUserId(user.id())
                .orElseThrow(() -> InvitationErrors.userAlreadyExists(request.email()));

        if (invitation.getStatus() != InvitationStatus.EXPIRED && invitation.getStatus() != InvitationStatus.CANCELLED) {
            throw InvitationErrors.userAlreadyExists(request.email());
        }

        // If user was disabled (cancelled case), set back to PENDING so they can accept
        if (user.status() == UserStatus.DISABLED) {
            userApi.reactivateForReinvite(user.id());
        }

        tokenRepository.deleteByInvitationId(invitation.getId());
        invitation.setStatus(InvitationStatus.PENDING);
        invitation.setExpiresAt(Instant.now().plus(properties.getExpiryDuration()));
        invitation.setInvitedById(invitedBy);
        invitation.setEmailAttempts(0);
        invitation.setEmailSentAt(null);
        invitation.setEmailMessageId(null);
        invitation.setUpdatedAt(Instant.now());
        invitationRepository.save(invitation);

        createToken(invitation.getId());
        sendInvitationEmailAsync(invitation.getId(), 1);

        log.info("Re-invited user {} (invitation {})", user.email(), invitation.getId());
        return toDto(invitation);
    }

    @Override
    @Transactional
    public void resend(UUID invitationId) {
        Invitation invitation = findInvitationOrThrow(invitationId);

        if (!invitation.canBeAccepted()) {
            throw InvitationErrors.resendNotAllowed(invitation.getStatus());
        }

        // Delete old tokens
        tokenRepository.deleteByInvitationId(invitationId);

        // Create new token and send
        createToken(invitationId);
        invitation.setStatus(InvitationStatus.PENDING);
        invitation.setEmailAttempts(0);
        invitation.setUpdatedAt(Instant.now());
        invitationRepository.save(invitation);

        sendInvitationEmailAsync(invitationId, 1);

        log.info("Resending invitation {}", invitationId);
    }

    @Override
    @Transactional
    public void cancel(UUID invitationId) {
        Invitation invitation = findInvitationOrThrow(invitationId);

        if (invitation.getStatus() == InvitationStatus.ACCEPTED) {
            throw InvitationErrors.cancelNotAllowed();
        }

        invitation.setStatus(InvitationStatus.CANCELLED);
        invitation.setUpdatedAt(Instant.now());
        invitationRepository.save(invitation);

        // Also disable the user
        userApi.disableUser(invitation.getUserId());

        log.info("Cancelled invitation {}", invitationId);
    }

    // ==================== Token validation ====================

    @Override
    @Transactional
    public TokenValidationResult validateToken(String token) {
        Optional<InvitationToken> tokenOpt = tokenRepository.findByToken(token);

        if (tokenOpt.isEmpty()) {
            return TokenValidationResult.failure("Ссылка приглашения недействительна или уже использована.");
        }

        InvitationToken invToken = tokenOpt.get();
        Invitation invitation = findInvitationOrThrow(invToken.getInvitationId());
        UserDto user = userApi.findById(invitation.getUserId())
                .orElseThrow(() -> InvitationErrors.userNotFound(invitation.getUserId()));

        // Check if invitation expired
        if (invitation.isExpired()) {
            invitation.setStatus(InvitationStatus.EXPIRED);
            invitationRepository.save(invitation);
            return TokenValidationResult.failure("Срок действия приглашения истёк.");
        }

        // Check if invitation can be accepted
        if (!invitation.canBeAccepted()) {
            return TokenValidationResult.failure("Приглашение недоступно для активации (уже принято или отменено).");
        }

        // Check if token expired
        if (invToken.isExpired()) {
            // Regenerate token and send new email
            tokenRepository.deleteByInvitationId(invitation.getId());
            createToken(invitation.getId());
            sendInvitationEmailAsync(invitation.getId(), 1);

            return TokenValidationResult.tokenRegeneratedAndSent(user.email());
        }

        return TokenValidationResult.success(
                invitation.getId(),
                user.id(),
                user.email(),
                user.roles() != null ? List.copyOf(user.roles()) : List.of(),
                user.firstName(),
                user.lastName(),
                false
        );
    }

    @Override
    @Transactional
    public void accept(AcceptInvitationRequest request) {
        Optional<InvitationToken> tokenOpt = tokenRepository.findByToken(request.token());

        if (tokenOpt.isEmpty()) {
            throw InvitationErrors.tokenInvalid();
        }

        InvitationToken invToken = tokenOpt.get();

        if (invToken.isExpired()) {
            throw InvitationErrors.tokenExpired();
        }

        Invitation invitation = findInvitationOrThrow(invToken.getInvitationId());

        if (!invitation.canBeAccepted()) {
            throw InvitationErrors.invitationNotAcceptable(invitation.getStatus());
        }

        UserDto user = userApi.findById(invitation.getUserId())
                .orElseThrow(() -> InvitationErrors.userNotFound(invitation.getUserId()));
        if (user.status() != UserStatus.PENDING || user.activatedAt() != null) {
            throw InvitationErrors.alreadyActivated();
        }

        // Activate user account
        userApi.activateUser(invitation.getUserId(), request.password());

        // Mark invitation as accepted
        invitation.markAccepted();
        invitationRepository.save(invitation);

        // Clean up tokens
        tokenRepository.deleteByInvitationId(invitation.getId());

        log.info("Invitation {} accepted", invitation.getId());
    }

    // ==================== Helper methods ====================

    private void validateInviterPermission(UUID inviterId, Role targetRole) {
        UserDto inviter = userApi.findById(inviterId)
                .orElseThrow(() -> InvitationErrors.inviterNotFound(inviterId));

        boolean canInvite = inviter.roles() != null && inviter.roles().stream()
                .anyMatch(r -> r.canInvite(targetRole));
        if (!canInvite) {
            String rolesStr = inviter.roles() != null ? inviter.roles().toString() : "—";
            throw InvitationErrors.cannotInviteRole(rolesStr, targetRole.name());
        }
    }

    private void createRoleProfiles(UUID userId, CreateInvitationRequest request) {
        Set<Role> roles = request.getEffectiveRoles();
        if (roles.contains(Role.STUDENT) && request.studentData() != null) {
            studentApi.create(userId, request.studentData());
        }
        if (roles.contains(Role.TEACHER) && request.teacherData() != null) {
            teacherApi.create(userId, request.teacherData());
        }
    }

    private InvitationToken createToken(UUID invitationId) {
        InvitationToken token = InvitationToken.builder()
                .invitationId(invitationId)
                .token(InvitationToken.generateTokenString())
                .expiresAt(Instant.now().plus(properties.getTokenExpiryDuration()))
                .build();

        return tokenRepository.save(token);
    }

    /**
     * Schedules invitation email to be sent after the current transaction commits.
     * Avoids race where the scheduled task runs before invitation/token are visible in DB.
     */
    private void sendInvitationEmailAsync(UUID invitationId, int attempt) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            taskScheduler.schedule(
                                    () -> sendInvitationEmailWithRetry(invitationId, attempt),
                                    Instant.now()
                            );
                        }
                    });
        } else {
            taskScheduler.schedule(
                    () -> sendInvitationEmailWithRetry(invitationId, attempt),
                    Instant.now()
            );
        }
    }

    @Transactional
    void sendInvitationEmailWithRetry(UUID invitationId, int attempt) {
        Invitation invitation = invitationRepository.findById(invitationId).orElse(null);
        if (invitation == null) {
            log.warn("Invitation {} not found for email sending", invitationId);
            return;
        }

        if (attempt > properties.getMaxEmailAttempts()) {
            invitation.setStatus(InvitationStatus.FAILED);
            invitation.setUpdatedAt(Instant.now());
            invitationRepository.save(invitation);
            log.error("Failed to send invitation email after {} attempts for invitation {}", 
                    properties.getMaxEmailAttempts(), invitationId);
            return;
        }

        invitation.setStatus(InvitationStatus.SENDING);
        invitation.incrementEmailAttempts();
        invitationRepository.save(invitation);

        try {
            InvitationToken token = tokenRepository.findByInvitationId(invitationId)
                    .orElseThrow(() -> Errors.conflict("Токен приглашения не найден. Повторите попытку позже."));

            UserDto user = userApi.findById(invitation.getUserId())
                    .orElseThrow(() -> InvitationErrors.userNotFound(invitation.getUserId()));

            EmailMessage email = buildInvitationEmail(user, token.getToken());
            EmailResult result = emailApi.send(email);

            if (result.success()) {
                invitation.markEmailSent(result.messageId());
                invitationRepository.save(invitation);
                log.info("Invitation email sent successfully to {} (attempt {})", user.email(), attempt);
            } else {
                log.warn("Email send attempt {} failed for {}: {}", attempt, user.email(), result.error());
                scheduleRetry(invitationId, attempt + 1);
            }
        } catch (Exception e) {
            log.error("Email send attempt {} failed for invitation {}: {}", attempt, invitationId, e.getMessage());
            scheduleRetry(invitationId, attempt + 1);
        }
    }

    private void scheduleRetry(UUID invitationId, int nextAttempt) {
        if (nextAttempt <= properties.getMaxEmailAttempts()) {
            long delaySeconds = (long) properties.getRetryDelaySeconds() * nextAttempt;
            taskScheduler.schedule(
                    () -> sendInvitationEmailWithRetry(invitationId, nextAttempt),
                    Instant.now().plusSeconds(delaySeconds)
            );
        }
    }

    private EmailMessage buildInvitationEmail(UserDto user, String token) {
        String inviteUrl = properties.getBaseUrl() + "/invite?token=" + token;

        String htmlBody = String.format("""
            <html>
            <body>
                <h1>Добро пожаловать в InterHubDev!</h1>
                <p>Здравствуйте, %s!</p>
                <p>Вы были приглашены в систему InterHubDev.</p>
                <p>Для активации вашего аккаунта перейдите по ссылке:</p>
                <p><a href="%s">Активировать аккаунт</a></p>
                <p>Ссылка действительна в течение 24 часов.</p>
                <p>Если вы не запрашивали это приглашение, просто проигнорируйте это письмо.</p>
                <br>
                <p>С уважением,<br>Команда InterHubDev</p>
            </body>
            </html>
            """, 
            user.firstName() != null ? user.firstName() : user.email(),
            inviteUrl
        );

        return EmailMessage.html(
                user.email(),
                "Приглашение в InterHubDev",
                htmlBody
        );
    }

    private Invitation findInvitationOrThrow(UUID id) {
        return invitationRepository.findById(id)
                .orElseThrow(() -> InvitationErrors.invitationNotFound(id));
    }

    private List<InvitationDto> toDtoList(List<Invitation> invitations) {
        if (invitations.isEmpty()) {
            return List.of();
        }
        Set<UUID> userIds = invitations.stream()
                .map(Invitation::getUserId)
                .collect(Collectors.toSet());
        Map<UUID, UserDto> userMap = userApi.findByIds(userIds).stream()
                .collect(Collectors.toMap(UserDto::id, u -> u));
        return invitations.stream()
                .map(inv -> toDto(inv, userMap.get(inv.getUserId())))
                .toList();
    }

    private InvitationDto toDto(Invitation inv) {
        UserDto user = userApi.findById(inv.getUserId()).orElse(null);
        return toDto(inv, user);
    }

    private InvitationDto toDto(Invitation inv, UserDto user) {
        return new InvitationDto(
                inv.getId(),
                inv.getUserId(),
                user != null ? user.email() : null,
                user != null && user.roles() != null ? List.copyOf(user.roles()) : List.of(),
                user != null ? user.firstName() : null,
                user != null ? user.lastName() : null,
                inv.getStatus(),
                inv.getInvitedById(),
                inv.getEmailSentAt(),
                inv.getEmailAttempts(),
                inv.getExpiresAt(),
                inv.getAcceptedAt(),
                inv.getCreatedAt()
        );
    }
}
