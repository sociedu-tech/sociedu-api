package com.unishare.api.modules.mentorapplication.service.impl;

import com.unishare.api.common.constants.MentorRequestStatuses;
import com.unishare.api.common.constants.MentorVerificationStatuses;
import com.unishare.api.common.constants.Roles;
import com.unishare.api.common.dto.AppException;
import com.unishare.api.common.dto.PageResponse;
import com.unishare.api.infrastructure.event.DomainEventPublisher;
import com.unishare.api.common.event.MentorRequestApprovedEvent;
import com.unishare.api.common.event.MentorRequestRejectedEvent;
import com.unishare.api.modules.auth.entity.User;
import com.unishare.api.modules.auth.repository.UserRepository;
import com.unishare.api.modules.auth.service.UserAccountService;
import com.unishare.api.modules.mentor.entity.MentorProfile;
import com.unishare.api.modules.mentor.repository.MentorProfileRepository;
import com.unishare.api.modules.mentorapplication.dto.*;
import com.unishare.api.modules.mentorapplication.entity.MentorApplication;
import com.unishare.api.modules.mentorapplication.exception.MentorApplicationErrorCode;
import com.unishare.api.modules.mentorapplication.repository.MentorApplicationRepository;
import com.unishare.api.modules.mentorapplication.service.MentorApplicationService;
import com.unishare.api.modules.user.entity.UserProfile;
import com.unishare.api.modules.user.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
@Service
@RequiredArgsConstructor
public class MentorApplicationServiceImpl implements MentorApplicationService {

    private static final Set<String> BLOCKING_STATUSES = Set.of(
            MentorRequestStatuses.SUBMITTED,
            MentorRequestStatuses.UNDER_REVIEW);

    private final MentorApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final MentorProfileRepository mentorProfileRepository;
    private final UserAccountService userAccountService;
    private final DomainEventPublisher eventPublisher;

    @Override
    @Transactional
    public MentorApplicationResponse submit(UUID userId, MentorApplicationPayload payload) {
        ensureNotMentor(userId);
        if (applicationRepository.existsByUserIdAndStatusIn(userId, BLOCKING_STATUSES)) {
            throw new AppException(MentorApplicationErrorCode.PENDING_REQUEST_EXISTS);
        }
        MentorApplication app = new MentorApplication();
        app.setUserId(userId);
        applyPayload(app, payload);
        app.setStatus(MentorRequestStatuses.SUBMITTED);
        return toResponse(applicationRepository.save(app));
    }

    @Override
    @Transactional
    public MentorApplicationResponse resubmit(UUID userId, MentorApplicationPayload payload) {
        ensureNotMentor(userId);
        MentorApplication latest = applicationRepository.findFirstByUserIdOrderByCreatedAtDesc(userId)
                .orElseThrow(() -> new AppException(MentorApplicationErrorCode.REQUEST_NOT_FOUND));
        if (!MentorRequestStatuses.REJECTED.equals(latest.getStatus())) {
            throw new AppException(MentorApplicationErrorCode.NOT_REJECTED);
        }
        applyPayload(latest, payload);
        latest.setStatus(MentorRequestStatuses.SUBMITTED);
        latest.setReason(null);
        latest.setNote(null);
        latest.setReviewedBy(null);
        latest.setReviewedAt(null);
        latest.setResubmitCount(latest.getResubmitCount() == null ? 1 : latest.getResubmitCount() + 1);
        return toResponse(applicationRepository.save(latest));
    }

    @Override
    @Transactional(readOnly = true)
    public MentorApplicationResponse getMyCurrent(UUID userId) {
        return applicationRepository.findFirstByUserIdOrderByCreatedAtDesc(userId)
                .map(this::toResponse)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<MentorApplicationResponse> adminList(String status, String q, Pageable pageable) {
        String normalizedStatus = status == null || status.isBlank() ? null : status.trim().toUpperCase();
        String normalizedQ = q == null || q.isBlank() ? null : q.trim();
        Page<MentorApplication> page = applicationRepository.searchAdmin(normalizedStatus, normalizedQ, pageable);
        Page<MentorApplicationResponse> mapped = page.map(this::toResponse);
        if (normalizedQ != null) {
            String needle = normalizedQ.toLowerCase();
            List<MentorApplicationResponse> filtered = mapped.getContent().stream()
                    .filter(r -> matchesSearch(r, needle))
                    .toList();
            if (filtered.size() != mapped.getContent().size()) {
                return PageResponse.<MentorApplicationResponse>builder()
                        .items(filtered)
                        .page(mapped.getNumber())
                        .size(mapped.getSize())
                        .total(filtered.size())
                        .totalPages(filtered.isEmpty() ? 0 : 1)
                        .build();
            }
        }
        return PageResponse.of(mapped);
    }

    @Override
    @Transactional(readOnly = true)
    public MentorApplicationResponse adminGet(UUID id) {
        return toResponse(findOrThrow(id));
    }

    @Override
    @Transactional
    public MentorApplicationResponse adminApprove(UUID id, UUID adminId, AdminApproveMentorApplicationRequest request) {
        MentorApplication app = findOrThrow(id);
        if (!MentorRequestStatuses.isReviewable(app.getStatus())) {
            throw new AppException(MentorApplicationErrorCode.NOT_REVIEWABLE);
        }
        String note = request != null ? request.getNote() : null;
        app.setStatus(MentorRequestStatuses.APPROVED);
        app.setReviewedBy(adminId);
        app.setReviewedAt(Instant.now());
        app.setNote(note);
        applicationRepository.save(app);

        userAccountService.replaceSingleRole(app.getUserId(), Roles.MENTOR);
        upsertMentorProfile(app);
        eventPublisher.publish(new MentorRequestApprovedEvent(id, app.getUserId(), adminId, note));
        return toResponse(app);
    }

    @Override
    @Transactional
    public MentorApplicationResponse adminReject(UUID id, UUID adminId, AdminRejectMentorApplicationRequest request) {
        MentorApplication app = findOrThrow(id);
        if (!MentorRequestStatuses.isReviewable(app.getStatus())) {
            throw new AppException(MentorApplicationErrorCode.NOT_REVIEWABLE);
        }
        app.setStatus(MentorRequestStatuses.REJECTED);
        app.setReason(request.getReason());
        app.setNote(request.getNote());
        app.setReviewedBy(adminId);
        app.setReviewedAt(Instant.now());
        applicationRepository.save(app);
        eventPublisher.publish(new MentorRequestRejectedEvent(
                id, app.getUserId(), adminId, request.getReason(), request.getNote()));
        return toResponse(app);
    }

    private void upsertMentorProfile(MentorApplication app) {
        MentorProfile profile = mentorProfileRepository.findById(app.getUserId())
                .orElse(new MentorProfile());
        profile.setUserId(app.getUserId());
        profile.setHeadline(app.getHeadline());
        profile.setExpertise(String.join(",", app.getExpertise() != null ? app.getExpertise() : List.of()));
        profile.setBasePrice(app.getHourlyRate() != null ? app.getHourlyRate() : BigDecimal.ZERO);
        profile.setVerificationStatus(MentorVerificationStatuses.VERIFIED);
        mentorProfileRepository.save(profile);
    }

    private void ensureNotMentor(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(MentorApplicationErrorCode.REQUEST_NOT_FOUND));
        boolean isMentor = user.getUserRoles().stream()
                .anyMatch(ur -> Roles.MENTOR.equalsIgnoreCase(ur.getRole().getName()));
        if (isMentor) {
            throw new AppException(MentorApplicationErrorCode.ALREADY_MENTOR);
        }
    }

    private MentorApplication findOrThrow(UUID id) {
        return applicationRepository.findById(id)
                .orElseThrow(() -> new AppException(MentorApplicationErrorCode.REQUEST_NOT_FOUND));
    }

    private void applyPayload(MentorApplication app, MentorApplicationPayload payload) {
        app.setHeadline(payload.getHeadline());
        app.setBio(payload.getBio());
        app.setExpertise(payload.getExpertise() != null ? List.copyOf(payload.getExpertise()) : List.of());
        app.setYearsOfExperience(payload.getYearsOfExperience());
        app.setHourlyRate(payload.getHourlyRate());
        app.setCvFileId(payload.getCvFileId());
        app.setCvUrl(payload.getCvUrl());
        app.setPortfolioUrls(payload.getPortfolioUrls() != null ? List.copyOf(payload.getPortfolioUrls()) : List.of());
        app.setCertificates(payload.getCertificates() != null ? List.copyOf(payload.getCertificates()) : List.of());
    }

    private boolean matchesSearch(MentorApplicationResponse r, String needle) {
        if (r.getHeadline() != null && r.getHeadline().toLowerCase().contains(needle)) {
            return true;
        }
        MentorApplicationApplicantDto a = r.getApplicant();
        if (a == null) {
            return false;
        }
        return (a.getEmail() != null && a.getEmail().toLowerCase().contains(needle))
                || (a.getFullName() != null && a.getFullName().toLowerCase().contains(needle));
    }

    private MentorApplicationResponse toResponse(MentorApplication app) {
        User user = userRepository.findById(app.getUserId()).orElse(null);
        UserProfile profile = userProfileRepository.findById(app.getUserId()).orElse(null);
        MentorApplicationApplicantDto applicant = null;
        if (user != null) {
            String fullName = profile != null ? profile.getDisplayName() : null;
            applicant = MentorApplicationApplicantDto.builder()
                    .userId(app.getUserId())
                    .email(user.getEmail())
                    .firstName(profile != null ? profile.getFirstName() : null)
                    .lastName(profile != null ? profile.getLastName() : null)
                    .fullName(fullName)
                    .createdAt(user.getCreatedAt())
                    .build();
        }
        return MentorApplicationResponse.builder()
                .id(app.getId())
                .userId(app.getUserId())
                .status(app.getStatus())
                .headline(app.getHeadline())
                .bio(app.getBio())
                .expertise(app.getExpertise())
                .yearsOfExperience(app.getYearsOfExperience())
                .hourlyRate(app.getHourlyRate())
                .cvFileId(app.getCvFileId())
                .cvUrl(app.getCvUrl())
                .portfolioUrls(app.getPortfolioUrls())
                .certificates(app.getCertificates())
                .reason(app.getReason())
                .note(app.getNote())
                .reviewedBy(app.getReviewedBy())
                .reviewedAt(app.getReviewedAt())
                .resubmitCount(app.getResubmitCount() != null ? app.getResubmitCount() : 0)
                .createdAt(app.getCreatedAt())
                .updatedAt(app.getUpdatedAt())
                .applicant(applicant)
                .build();
    }
}
