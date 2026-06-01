package com.unishare.api.modules.user.service.impl;

import com.unishare.api.common.dto.AppException;
import com.unishare.api.config.cache.CacheNames;
import com.unishare.api.config.cache.EvictUserProfileCaches;
import com.unishare.api.modules.user.dto.*;
import com.unishare.api.modules.user.entity.*;
import com.unishare.api.modules.user.exception.UserErrorCode;
import com.unishare.api.modules.user.mapper.UserMapper;
import com.unishare.api.modules.user.repository.*;
import com.unishare.api.modules.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserProfileRepository profileRepository;
    private final UserEducationRepository educationRepository;
    private final UserLanguageRepository languageRepository;
    private final UserExperienceRepository experienceRepository;
    private final UserCertificateRepository certificateRepository;
    private final UserMapper userMapper;

    // Profile
    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheNames.USER_PROFILE, key = "#userId")
    public UserProfileResponse getProfile(UUID userId) {
        return profileRepository.findById(userId)
                .map(userMapper::toResponse)
                .orElseGet(() -> {
                    UserProfileResponse empty = new UserProfileResponse();
                    empty.setUserId(userId);
                    return empty;
                });
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheNames.USER_FULL_PROFILE, key = "#userId")
    public UserFullProfileResponse getFullProfile(UUID userId) {
        return UserFullProfileResponse.builder()
                .profile(getProfile(userId))
                .educations(getEducations(userId))
                .languages(getLanguages(userId))
                .experiences(getExperiences(userId))
                .certificates(getCertificates(userId))
                .build();
    }

    @Override
    @Transactional
    @EvictUserProfileCaches
    public UserProfileResponse updateProfile(UUID userId, UserProfileRequest request) {
        UserProfile profile = profileRepository.findById(userId)
                .orElse(new UserProfile());
        
        if (profile.getUserId() == null) {
            profile.setUserId(userId);
        }
        
        userMapper.updateEntity(profile, request);
        UserProfile savedProfile = profileRepository.save(profile);
        return userMapper.toResponse(savedProfile);
    }

    @Override
    @Transactional
    public void createProfileForNewUser(UUID userId, String firstName, String lastName) {
        if (profileRepository.existsById(userId)) {
            return;
        }
        UserProfile profile = new UserProfile();
        profile.setUserId(userId);
        profile.setFirstName(firstName != null ? firstName.trim() : null);
        profile.setLastName(lastName != null ? lastName.trim() : null);
        profileRepository.save(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, UserProfileNames> getProfileNamesByUserIds(Collection<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        return profileRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(
                        UserProfile::getUserId,
                        p -> new UserProfileNames(p.getFirstName(), p.getLastName())));
    }

    // Education
    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheNames.USER_EDUCATIONS, key = "#userId")
    public List<UserEducationResponse> getEducations(UUID userId) {
        return educationRepository.findByUserId(userId).stream()
                .map(userMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.USER_EDUCATIONS, key = "#userId"),
            @CacheEvict(cacheNames = CacheNames.USER_FULL_PROFILE, key = "#userId"),
    })
    public UserEducationResponse addEducation(UUID userId, UserEducationRequest request) {
        UserEducation education = userMapper.toEntity(request, userId);
        UserEducation saved = educationRepository.save(education);
        return userMapper.toResponse(saved);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.USER_EDUCATIONS, key = "#userId"),
            @CacheEvict(cacheNames = CacheNames.USER_FULL_PROFILE, key = "#userId"),
    })
    public UserEducationResponse updateEducation(UUID userId, UUID educationId, UserEducationRequest request) {
        UserEducation education = educationRepository.findById(educationId)
                .filter(e -> e.getUserId().equals(userId))
                .orElseThrow(() -> new AppException(UserErrorCode.EDUCATION_NOT_FOUND));
        
        education.setUniversityId(request.getUniversityId());
        education.setMajorId(request.getMajorId());
        education.setDegree(request.getDegree());
        education.setStartDate(request.getStartDate());
        education.setEndDate(request.getEndDate());
        if (request.getIsCurrent() != null) {
            education.setIsCurrent(request.getIsCurrent());
        }
        education.setDescription(request.getDescription());

        UserEducation saved = educationRepository.save(education);
        return userMapper.toResponse(saved);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.USER_EDUCATIONS, key = "#userId"),
            @CacheEvict(cacheNames = CacheNames.USER_FULL_PROFILE, key = "#userId"),
    })
    public void deleteEducation(UUID userId, UUID educationId) {
        UserEducation education = educationRepository.findById(educationId)
                .filter(e -> e.getUserId().equals(userId))
                .orElseThrow(() -> new AppException(UserErrorCode.EDUCATION_NOT_FOUND));
        educationRepository.delete(education);
    }

    // Language
    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheNames.USER_LANGUAGES, key = "#userId")
    public List<UserLanguageResponse> getLanguages(UUID userId) {
        return languageRepository.findByUserId(userId).stream()
                .map(userMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.USER_LANGUAGES, key = "#userId"),
            @CacheEvict(cacheNames = CacheNames.USER_FULL_PROFILE, key = "#userId"),
    })
    public UserLanguageResponse addLanguage(UUID userId, UserLanguageRequest request) {
        UserLanguage language = userMapper.toEntity(request, userId);
        UserLanguage saved = languageRepository.save(language);
        return userMapper.toResponse(saved);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.USER_LANGUAGES, key = "#userId"),
            @CacheEvict(cacheNames = CacheNames.USER_FULL_PROFILE, key = "#userId"),
    })
    public UserLanguageResponse updateLanguage(UUID userId, UUID languageId, UserLanguageRequest request) {
        UserLanguage language = languageRepository.findById(languageId)
                .filter(l -> l.getUserId().equals(userId))
                .orElseThrow(() -> new AppException(UserErrorCode.LANGUAGE_NOT_FOUND));
        
        language.setLanguage(request.getLanguage());
        language.setLevel(request.getLevel());
        
        UserLanguage saved = languageRepository.save(language);
        return userMapper.toResponse(saved);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.USER_LANGUAGES, key = "#userId"),
            @CacheEvict(cacheNames = CacheNames.USER_FULL_PROFILE, key = "#userId"),
    })
    public void deleteLanguage(UUID userId, UUID languageId) {
        UserLanguage language = languageRepository.findById(languageId)
                .filter(l -> l.getUserId().equals(userId))
                .orElseThrow(() -> new AppException(UserErrorCode.LANGUAGE_NOT_FOUND));
        languageRepository.delete(language);
    }

    // Experience
    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheNames.USER_EXPERIENCES, key = "#userId")
    public List<UserExperienceResponse> getExperiences(UUID userId) {
        return experienceRepository.findByUserId(userId).stream()
                .map(userMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.USER_EXPERIENCES, key = "#userId"),
            @CacheEvict(cacheNames = CacheNames.USER_FULL_PROFILE, key = "#userId"),
    })
    public UserExperienceResponse addExperience(UUID userId, UserExperienceRequest request) {
        UserExperience experience = userMapper.toEntity(request, userId);
        UserExperience saved = experienceRepository.save(experience);
        return userMapper.toResponse(saved);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.USER_EXPERIENCES, key = "#userId"),
            @CacheEvict(cacheNames = CacheNames.USER_FULL_PROFILE, key = "#userId"),
    })
    public UserExperienceResponse updateExperience(UUID userId, UUID experienceId, UserExperienceRequest request) {
        UserExperience experience = experienceRepository.findById(experienceId)
                .filter(e -> e.getUserId().equals(userId))
                .orElseThrow(() -> new AppException(UserErrorCode.EXPERIENCE_NOT_FOUND));
        
        experience.setCompany(request.getCompany());
        experience.setPosition(request.getPosition());
        experience.setStartDate(request.getStartDate());
        experience.setEndDate(request.getEndDate());
        if (request.getIsCurrent() != null) {
            experience.setIsCurrent(request.getIsCurrent());
        }
        experience.setDescription(request.getDescription());

        UserExperience saved = experienceRepository.save(experience);
        return userMapper.toResponse(saved);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.USER_EXPERIENCES, key = "#userId"),
            @CacheEvict(cacheNames = CacheNames.USER_FULL_PROFILE, key = "#userId"),
    })
    public void deleteExperience(UUID userId, UUID experienceId) {
        UserExperience experience = experienceRepository.findById(experienceId)
                .filter(e -> e.getUserId().equals(userId))
                .orElseThrow(() -> new AppException(UserErrorCode.EXPERIENCE_NOT_FOUND));
        experienceRepository.delete(experience);
    }

    // Certificate
    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheNames.USER_CERTIFICATES, key = "#userId")
    public List<UserCertificateResponse> getCertificates(UUID userId) {
        return certificateRepository.findByUserId(userId).stream()
                .map(userMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.USER_CERTIFICATES, key = "#userId"),
            @CacheEvict(cacheNames = CacheNames.USER_FULL_PROFILE, key = "#userId"),
    })
    public UserCertificateResponse addCertificate(UUID userId, UserCertificateRequest request) {
        UserCertificate certificate = userMapper.toEntity(request, userId);
        UserCertificate saved = certificateRepository.save(certificate);
        return userMapper.toResponse(saved);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.USER_CERTIFICATES, key = "#userId"),
            @CacheEvict(cacheNames = CacheNames.USER_FULL_PROFILE, key = "#userId"),
    })
    public UserCertificateResponse updateCertificate(UUID userId, UUID certificateId, UserCertificateRequest request) {
        UserCertificate certificate = certificateRepository.findById(certificateId)
                .filter(c -> c.getUserId().equals(userId))
                .orElseThrow(() -> new AppException(UserErrorCode.CERTIFICATE_NOT_FOUND));
        
        certificate.setName(request.getName());
        certificate.setOrganization(request.getOrganization());
        certificate.setIssueDate(request.getIssueDate());
        certificate.setExpirationDate(request.getExpirationDate());
        certificate.setCredentialFileId(request.getCredentialFileId());
        certificate.setDescription(request.getDescription());

        UserCertificate saved = certificateRepository.save(certificate);
        return userMapper.toResponse(saved);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.USER_CERTIFICATES, key = "#userId"),
            @CacheEvict(cacheNames = CacheNames.USER_FULL_PROFILE, key = "#userId"),
    })
    public void deleteCertificate(UUID userId, UUID certificateId) {
        UserCertificate certificate = certificateRepository.findById(certificateId)
                .filter(c -> c.getUserId().equals(userId))
                .orElseThrow(() -> new AppException(UserErrorCode.CERTIFICATE_NOT_FOUND));
        certificateRepository.delete(certificate);
    }
}
