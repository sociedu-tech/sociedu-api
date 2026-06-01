package com.unishare.api.modules.mentor.service.impl;

import com.unishare.api.common.constants.MentorVerificationStatuses;
import com.unishare.api.common.dto.AppException;
import com.unishare.api.modules.mentor.dto.MentorRequest;
import com.unishare.api.modules.mentor.dto.MentorResponse;
import com.unishare.api.modules.mentor.entity.MentorProfile;
import com.unishare.api.modules.mentor.exception.MentorErrorCode;
import com.unishare.api.modules.mentor.repository.MentorProfileRepository;
import com.unishare.api.modules.mentor.service.MentorService;
import com.unishare.api.modules.user.dto.UserProfileNames;
import com.unishare.api.modules.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MentorServiceImpl implements MentorService {

    private final MentorProfileRepository mentorProfileRepository;
    private final UserService userService;

    @Override
    @Transactional(readOnly = true)
    public MentorResponse getMentorProfile(UUID mentorId) {
        MentorProfile profile = mentorProfileRepository.findById(mentorId)
                .orElseThrow(() -> new AppException(MentorErrorCode.MENTOR_NOT_FOUND, "Mentor not found"));
        return mapToResponse(profile);
    }

    @Override
    @Transactional
    public MentorResponse createOrUpdateProfile(UUID userId, MentorRequest request) {
        MentorProfile profile = mentorProfileRepository.findById(userId)
                .orElse(new MentorProfile());

        profile.setUserId(userId);
        profile.setHeadline(request.getHeadline());
        profile.setExpertise(request.getExpertise());
        profile.setBasePrice(request.getBasePrice());

        if (profile.getVerificationStatus() == null) {
            profile.setVerificationStatus(MentorVerificationStatuses.PENDING);
        }

        mentorProfileRepository.save(profile);
        return getMentorProfile(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MentorResponse> getAllVerifiedMentors(Pageable pageable) {
        return searchMentors(MentorVerificationStatuses.VERIFIED, null, null, null, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MentorResponse> searchMentors(String verificationStatus, String keyword,
                                              BigDecimal minBasePrice, BigDecimal maxBasePrice, Pageable pageable) {
        String status = verificationStatus == null || verificationStatus.isBlank()
                ? MentorVerificationStatuses.VERIFIED
                : verificationStatus.trim();
        String kw = normalizeMentorKeyword(keyword);
        if (minBasePrice != null && maxBasePrice != null && minBasePrice.compareTo(maxBasePrice) > 0) {
            throw new AppException(MentorErrorCode.INVALID_SEARCH_FILTER, "minBasePrice must be <= maxBasePrice");
        }
        Page<MentorProfile> page = mentorProfileRepository.searchByStatusAndFilters(
                status, kw, minBasePrice, maxBasePrice, withoutSort(pageable));
        List<UUID> userIds = page.getContent().stream().map(MentorProfile::getUserId).toList();
        Map<UUID, UserProfileNames> namesByUserId = userService.getProfileNamesByUserIds(userIds);
        return page.map(profile -> mapToResponse(profile, namesByUserId.get(profile.getUserId())));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean mentorProfileExists(UUID userId) {
        return mentorProfileRepository.existsById(userId);
    }

    @Override
    @Transactional
    public MentorResponse submitProfileVerification(UUID userId) {
        MentorProfile profile = mentorProfileRepository.findById(userId)
                .orElseThrow(() -> new AppException(MentorErrorCode.MENTOR_NOT_FOUND, "Mentor profile not found. Save profile details first."));

        String status = profile.getVerificationStatus();
        if (MentorVerificationStatuses.VERIFIED.equalsIgnoreCase(status)) {
            throw new AppException(MentorErrorCode.PROFILE_ALREADY_VERIFIED, "Hồ sơ mentor đã được duyệt rồi.");
        }

        // Completeness validation
        if (profile.getHeadline() == null || profile.getHeadline().isBlank()
                || profile.getExpertise() == null || profile.getExpertise().isBlank()
                || profile.getBasePrice() == null) {
            throw new AppException(MentorErrorCode.PROFILE_INCOMPLETE, "Hồ sơ chưa hoàn thiện. Vui lòng cập nhật đầy đủ headline, chuyên môn và giá cơ bản.");
        }

        // Idempotency: only update status if it is not already pending
        if (!MentorVerificationStatuses.PENDING.equalsIgnoreCase(status)) {
            profile.setVerificationStatus(MentorVerificationStatuses.PENDING);
            mentorProfileRepository.save(profile);
        }

        return mapToResponse(profile);
    }

    /** Bỏ {@code sort=} — native query đã có {@code ORDER BY m.sessions_completed}. */
    private static Pageable withoutSort(Pageable pageable) {
        if (pageable == null || !pageable.isPaged()) {
            return Pageable.unpaged();
        }
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
    }

    private static String normalizeMentorKeyword(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String t = raw.trim();
        return t.length() > 100 ? t.substring(0, 100) : t;
    }

    private MentorResponse mapToResponse(MentorProfile profile) {
        Map<UUID, UserProfileNames> names =
                userService.getProfileNamesByUserIds(List.of(profile.getUserId()));
        return mapToResponse(profile, names.get(profile.getUserId()));
    }

    private MentorResponse mapToResponse(MentorProfile profile, UserProfileNames names) {
        String displayName = names != null ? names.toDisplayName() : null;
        return MentorResponse.builder()
                .userId(profile.getUserId())
                .displayName(displayName)
                .headline(profile.getHeadline())
                .expertise(profile.getExpertise())
                .basePrice(profile.getBasePrice())
                .ratingAvg(profile.getRatingAvg())
                .sessionsCompleted(profile.getSessionsCompleted())
                .verificationStatus(profile.getVerificationStatus())
                .build();
    }
}
