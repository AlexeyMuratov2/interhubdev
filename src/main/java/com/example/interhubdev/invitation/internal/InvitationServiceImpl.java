package com.example.interhubdev.invitation.internal;

import com.example.interhubdev.email.EmailApi;
import com.example.interhubdev.email.EmailMessage;
import com.example.interhubdev.email.EmailResult;
import com.example.interhubdev.invitation.*;
import com.example.interhubdev.student.StudentApi;
import com.example.interhubdev.teacher.TeacherApi;
import com.example.interhubdev.user.Role;
import com.example.interhubdev.user.UserApi;
import com.example.interhubdev.user.UserDto;
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
        // 1. Validate inviter can invite this role
        validateInviterPermission(invitedBy, request.role());

        // 2. Create user with PENDING status
        UserDto user = userApi.createUser(
                request.email(),
                request.role(),
                request.firstName(),
                request.lastName()
        );

        // 3. Create role-specific profile if needed
        createRoleProfile(user.id(), request);

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

        log.info("Created invitation {} for user {} (role: {})", 
                invitation.getId(), user.email(), request.role());

        return toDto(invitation);
    }

    @Override
    @Transactional
    public void resend(UUID invitationId) {
        Invitation invitation = findInvitationOrThrow(invitationId);

        if (!invitation.canBeAccepted()) {
            throw new IllegalStateException("Cannot resend invitation in status: " + invitation.getStatus());
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
            throw new IllegalStateException("Cannot cancel already accepted invitation");
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
            return TokenValidationResult.failure("Invalid token");
        }

        InvitationToken invToken = tokenOpt.get();
        Invitation invitation = findInvitationOrThrow(invToken.getInvitationId());
        UserDto user = userApi.findById(invitation.getUserId()).orElseThrow();

        // Check if invitation expired
        if (invitation.isExpired()) {
            invitation.setStatus(InvitationStatus.EXPIRED);
            invitationRepository.save(invitation);
            return TokenValidationResult.failure("Invitation has expired");
        }

        // Check if invitation can be accepted
        if (!invitation.canBeAccepted()) {
            return TokenValidationResult.failure("Invitation is not valid (status: " + invitation.getStatus() + ")");
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
                user.role(),
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
            throw new IllegalArgumentException("Invalid token");
        }

        InvitationToken invToken = tokenOpt.get();

        if (invToken.isExpired()) {
            throw new IllegalArgumentException("Token has expired");
        }

        Invitation invitation = findInvitationOrThrow(invToken.getInvitationId());

        if (!invitation.canBeAccepted()) {
            throw new IllegalStateException("Invitation cannot be accepted (status: " + invitation.getStatus() + ")");
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
                .orElseThrow(() -> new IllegalArgumentException("Inviter not found: " + inviterId));

        if (!inviter.role().canInvite(targetRole)) {
            throw new IllegalArgumentException(
                    String.format("User with role %s cannot invite users with role %s", 
                            inviter.role(), targetRole));
        }
    }

    private void createRoleProfile(UUID userId, CreateInvitationRequest request) {
        switch (request.role()) {
            case STUDENT -> {
                if (request.studentData() != null) {
                    studentApi.create(userId, request.studentData());
                }
            }
            case TEACHER -> {
                if (request.teacherData() != null) {
                    teacherApi.create(userId, request.teacherData());
                }
            }
            default -> {
                // ADMIN, SUPER_ADMIN, STAFF don't need additional profiles
            }
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
                    .orElseThrow(() -> new IllegalStateException("Token not found for invitation"));

            UserDto user = userApi.findById(invitation.getUserId()).orElseThrow();

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
                .orElseThrow(() -> new IllegalArgumentException("Invitation not found: " + id));
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
                user != null ? user.role() : null,
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
