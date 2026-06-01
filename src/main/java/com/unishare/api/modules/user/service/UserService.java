package com.unishare.api.modules.user.service;

import com.unishare.api.modules.user.dto.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface UserService {

    // Profile
    UserProfileResponse getProfile(UUID userId);
    UserFullProfileResponse getFullProfile(UUID userId);
    UserProfileResponse updateProfile(UUID userId, UserProfileRequest request);

    /** Gọi từ auth sau khi tạo user — chỉ tạo khi chưa có profile. */
    void createProfileForNewUser(UUID userId, String firstName, String lastName);

    /** Đọc tên theo batch (admin, auth) — userId không có profile sẽ không có trong map. */
    Map<UUID, UserProfileNames> getProfileNamesByUserIds(Collection<UUID> userIds);

    // Education
    List<UserEducationResponse> getEducations(UUID userId);
    UserEducationResponse addEducation(UUID userId, UserEducationRequest request);
    UserEducationResponse updateEducation(UUID userId, UUID educationId, UserEducationRequest request);
    void deleteEducation(UUID userId, UUID educationId);

    // Language
    List<UserLanguageResponse> getLanguages(UUID userId);
    UserLanguageResponse addLanguage(UUID userId, UserLanguageRequest request);
    UserLanguageResponse updateLanguage(UUID userId, UUID languageId, UserLanguageRequest request);
    void deleteLanguage(UUID userId, UUID languageId);

    // Experience
    List<UserExperienceResponse> getExperiences(UUID userId);
    UserExperienceResponse addExperience(UUID userId, UserExperienceRequest request);
    UserExperienceResponse updateExperience(UUID userId, UUID experienceId, UserExperienceRequest request);
    void deleteExperience(UUID userId, UUID experienceId);

    // Certificate
    List<UserCertificateResponse> getCertificates(UUID userId);
    UserCertificateResponse addCertificate(UUID userId, UserCertificateRequest request);
    UserCertificateResponse updateCertificate(UUID userId, UUID certificateId, UserCertificateRequest request);
    void deleteCertificate(UUID userId, UUID certificateId);
}
