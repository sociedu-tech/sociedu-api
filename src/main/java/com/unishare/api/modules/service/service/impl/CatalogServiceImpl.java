package com.unishare.api.modules.service.service.impl;

import com.unishare.api.common.dto.AppException;
import com.unishare.api.modules.service.dto.MentorDto.CurriculumItemRequest;
import com.unishare.api.modules.service.dto.MentorDto.CurriculumItemResponse;
import com.unishare.api.modules.service.dto.MentorDto.ServicePackageResponse;
import com.unishare.api.modules.service.dto.MentorDto.ServicePackageVersionResponse;
import com.unishare.api.modules.service.dto.request.CreateServicePackageRequest;
import com.unishare.api.modules.service.dto.request.CreateServicePackageVersionRequest;
import com.unishare.api.modules.service.dto.request.UpdateServicePackageRequest;
import com.unishare.api.modules.service.entity.PackageCurriculum;
import com.unishare.api.modules.service.entity.ServicePackage;
import com.unishare.api.modules.service.entity.ServicePackageVersion;
import com.unishare.api.modules.service.exception.ServiceErrorCode;
import com.unishare.api.modules.service.repository.PackageCurriculumRepository;
import com.unishare.api.modules.service.repository.ServicePackageRepository;
import com.unishare.api.modules.service.repository.ServicePackageVersionRepository;
import com.unishare.api.modules.service.service.CatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CatalogServiceImpl implements CatalogService {

    private final ServicePackageRepository servicePackageRepository;
    private final ServicePackageVersionRepository servicePackageVersionRepository;
    private final PackageCurriculumRepository packageCurriculumRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<ServicePackageResponse> getMentorPackages(UUID mentorId, String keyword, Pageable pageable) {
        String kw = normalizeKeyword(keyword);
        Page<ServicePackage> page = (kw == null)
                ? servicePackageRepository.findByMentorIdAndIsActiveTrueAndDeletedAtIsNull(mentorId, pageable)
                : servicePackageRepository.searchActiveByMentorId(mentorId, kw, pageable);
        return page.map(this::mapToCatalogPackageResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ServicePackageResponse> getMyPackages(UUID mentorId, String keyword, Pageable pageable) {
        String kw = normalizeKeyword(keyword);
        Page<ServicePackage> page = (kw == null)
                ? servicePackageRepository.findByMentorId(mentorId, pageable)
                : servicePackageRepository.searchByMentorId(mentorId, kw, pageable);
        return page.map(this::mapToPackageResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ServicePackageResponse getMyPackage(UUID mentorId, UUID packageId) {
        return mapToPackageResponse(requireOwnedPackage(mentorId, packageId));
    }

    @Override
    public Page<ServicePackageResponse> getActivePackages(UUID mentorId, String keyword, Pageable pageable) {
        String kw = normalizeKeyword(keyword);
        Page<ServicePackage> page;
        if (kw == null && mentorId == null) {
            page = servicePackageRepository.findByIsActiveTrueAndDeletedAtIsNull(pageable);
        } else if (kw == null) {
            page = servicePackageRepository.findByMentorIdAndIsActiveTrueAndDeletedAtIsNull(mentorId, pageable);
        } else {
            page = servicePackageRepository.searchActivePackages(mentorId, kw, pageable);
        }
        return page.map(this::mapToCatalogPackageResponse);
    }

    @Override
    public ServicePackageResponse getActivePackage(UUID packageId) {
        ServicePackage servicePackage = servicePackageRepository.findActiveById(packageId)
                .orElseThrow(() -> new AppException(ServiceErrorCode.PACKAGE_NOT_FOUND, "Package not found"));
        return mapToCatalogPackageResponse(servicePackage);
    }

    @Override
    @Transactional
    public ServicePackageResponse createPackage(UUID mentorId, CreateServicePackageRequest request) {
        validateCreatePackageRequest(request);

        ServicePackage pkg = new ServicePackage();
        pkg.setMentorId(mentorId);
        pkg.setName(request.getName());
        pkg.setDescription(request.getDescription());
        pkg.setIsActive(true);
        ServicePackage saved = servicePackageRepository.save(pkg);

        ServicePackageVersion ver = new ServicePackageVersion();
        ver.setPackageId(saved.getId());
        ver.setPrice(request.getPrice());
        ver.setDuration(request.getDuration());
        ver.setDeliveryType(request.getDeliveryType());
        ver.setIsDefault(true);
        ServicePackageVersion savedVersion = servicePackageVersionRepository.save(ver);

        List<PackageCurriculum> curriculums = request.getCurriculums().stream()
                .map(item -> mapCurriculumRequest(savedVersion.getId(), item))
                .toList();
        packageCurriculumRepository.saveAll(curriculums);

        return mapToPackageResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ServicePackageVersionResponse> getPackageVersions(UUID mentorId, UUID packageId, Pageable pageable) {
        ServicePackage pkg = requireOwnedPackage(mentorId, packageId);
        return servicePackageVersionRepository.findByPackageId(pkg.getId(), pageable)
                .map(this::mapToVersionResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ServicePackageVersionResponse getPackageVersion(UUID mentorId, UUID packageId, UUID versionId) {
        ServicePackageVersion version = requireOwnedVersion(mentorId, packageId, versionId);
        return mapToVersionResponse(version);
    }

    @Override
    @Transactional
    public ServicePackageResponse createPackageVersion(UUID mentorId, UUID packageId, CreateServicePackageVersionRequest request) {
        ServicePackage pkg = servicePackageRepository.findById(packageId)
                .filter(p -> p.getMentorId().equals(mentorId))
                .orElseThrow(() -> new AppException(ServiceErrorCode.PACKAGE_NOT_FOUND, "Package not found"));
        assertPackageNotArchived(pkg);

        List<ServicePackageVersion> existingVersions = servicePackageVersionRepository.findByPackageId(packageId);
        ServicePackageVersion currentDefaultVersion = existingVersions.stream()
                .filter(version -> Boolean.TRUE.equals(version.getIsDefault()))
                .findFirst()
                .orElseThrow(() -> new AppException(ServiceErrorCode.SERVICE_VERSION_NOT_FOUND, "Default version not found"));

        existingVersions.forEach(version -> version.setIsDefault(false));
        servicePackageVersionRepository.saveAll(existingVersions);

        ServicePackageVersion newVersion = new ServicePackageVersion();
        newVersion.setPackageId(pkg.getId());
        newVersion.setPrice(request.getPrice());
        newVersion.setDuration(request.getDuration());
        newVersion.setDeliveryType(request.getDeliveryType());
        newVersion.setIsDefault(true);
        ServicePackageVersion savedVersion = servicePackageVersionRepository.save(newVersion);

        List<PackageCurriculum> clonedCurriculums = packageCurriculumRepository
                .findByPackageVersionIdOrderByOrderIndexAsc(currentDefaultVersion.getId()).stream()
                .map(curriculum -> cloneCurriculum(savedVersion.getId(), curriculum))
                .toList();
        packageCurriculumRepository.saveAll(clonedCurriculums);

        return mapToPackageResponse(pkg);
    }

    @Override
    @Transactional
    public ServicePackageResponse updatePackage(UUID mentorId, UUID packageId, UpdateServicePackageRequest request) {
        ServicePackage pkg = servicePackageRepository.findById(packageId)
                .filter(p -> p.getMentorId().equals(mentorId))
                .orElseThrow(() -> new AppException(ServiceErrorCode.PACKAGE_NOT_FOUND, "Package not found"));
        assertPackageNotArchived(pkg);

        pkg.setName(request.getName());
        pkg.setDescription(request.getDescription());
        ServicePackage saved = servicePackageRepository.save(pkg);
        return mapToPackageResponse(saved);
    }

    @Override
    @Transactional
    public ServicePackageResponse togglePackage(UUID mentorId, UUID packageId) {
        ServicePackage pkg = servicePackageRepository.findById(packageId)
                .filter(p -> p.getMentorId().equals(mentorId))
                .orElseThrow(() -> new AppException(ServiceErrorCode.PACKAGE_NOT_FOUND, "Package not found"));
        assertPackageNotArchived(pkg);

        pkg.setIsActive(!Boolean.TRUE.equals(pkg.getIsActive()));
        ServicePackage saved = servicePackageRepository.save(pkg);
        return mapToPackageResponse(saved);
    }

    @Override
    @Transactional
    public void deletePackage(UUID mentorId, UUID packageId) {
        ServicePackage pkg = servicePackageRepository.findById(packageId)
                .filter(p -> p.getMentorId().equals(mentorId))
                .orElseThrow(() -> new AppException(ServiceErrorCode.PACKAGE_NOT_FOUND, "Package not found"));
        assertPackageNotArchived(pkg);

        pkg.setDeletedAt(Instant.now());
        pkg.setIsActive(false);
        servicePackageRepository.save(pkg);
    }

    private ServicePackageResponse mapToPackageResponse(ServicePackage pkg) {
        List<ServicePackageVersion> versions = servicePackageVersionRepository.findByPackageId(pkg.getId());
        Map<UUID, List<CurriculumItemResponse>> curriculumsByVersionId = versions.stream()
                .collect(Collectors.toMap(
                        ServicePackageVersion::getId,
                        version -> packageCurriculumRepository.findByPackageVersionIdOrderByOrderIndexAsc(version.getId()).stream()
                                .map(this::mapCurriculum)
                                .toList()
                ));
        return ServicePackageResponse.builder()
                .id(pkg.getId())
                .mentorId(pkg.getMentorId())
                .name(pkg.getName())
                .description(pkg.getDescription())
                .isActive(pkg.getIsActive())
                .isArchived(pkg.isDeleted())
                .versions(versions.stream()
                        .map(version -> mapVersion(version, curriculumsByVersionId.getOrDefault(version.getId(), List.of())))
                        .collect(Collectors.toList()))
                .build();
    }

    private ServicePackageResponse mapToCatalogPackageResponse(ServicePackage pkg) {
        List<ServicePackageVersionResponse> versions = servicePackageVersionRepository.findByPackageId(pkg.getId()).stream()
                .filter(version -> Boolean.TRUE.equals(version.getIsDefault()))
                .map(version -> mapVersion(
                        version,
                        packageCurriculumRepository.findByPackageVersionIdOrderByOrderIndexAsc(version.getId()).stream()
                                .map(this::mapCurriculum)
                                .toList()))
                .toList();

        return ServicePackageResponse.builder()
                .id(pkg.getId())
                .mentorId(pkg.getMentorId())
                .name(pkg.getName())
                .description(pkg.getDescription())
                .isActive(pkg.getIsActive())
                .isArchived(pkg.isDeleted())
                .versions(versions)
                .build();
    }

    private ServicePackageVersionResponse mapVersion(ServicePackageVersion v, List<CurriculumItemResponse> curriculums) {
        return ServicePackageVersionResponse.builder()
                .id(v.getId())
                .price(v.getPrice())
                .duration(v.getDuration())
                .deliveryType(v.getDeliveryType())
                .isDefault(v.getIsDefault())
                .curriculums(curriculums)
                .build();
    }

    private ServicePackageVersionResponse mapToVersionResponse(ServicePackageVersion version) {
        return mapVersion(
                version,
                packageCurriculumRepository.findByPackageVersionIdOrderByOrderIndexAsc(version.getId()).stream()
                        .map(this::mapCurriculum)
                        .toList()
        );
    }

    @Override
    @Transactional
    public CurriculumItemResponse addCurriculumItem(UUID mentorId, UUID packageId, UUID versionId, CurriculumItemRequest request) {
        ServicePackageVersion ver = requireOwnedVersionForMutation(mentorId, packageId, versionId);
        if (packageCurriculumRepository.existsByPackageVersionIdAndOrderIndex(ver.getId(), request.getOrderIndex())) {
            throw new AppException(ServiceErrorCode.DUPLICATE_CURRICULUM_ORDER_INDEX,
                    "Thứ tự curriculum không được trùng nhau trong cùng một phiên bản gói");
        }
        PackageCurriculum c = new PackageCurriculum();
        c.setPackageVersionId(ver.getId());
        c.setTitle(request.getTitle());
        c.setDescription(request.getDescription());
        c.setOrderIndex(request.getOrderIndex());
        c.setDuration(request.getDuration());
        c = packageCurriculumRepository.save(c);
        return mapCurriculum(c);
    }

    @Override
    @Transactional
    public CurriculumItemResponse updateCurriculumItem(UUID mentorId, UUID packageId, UUID versionId, UUID curriculumId, CurriculumItemRequest request) {
        ServicePackageVersion ver = requireOwnedVersionForMutation(mentorId, packageId, versionId);
        PackageCurriculum curriculum = packageCurriculumRepository.findById(curriculumId)
                .filter(item -> item.getPackageVersionId().equals(ver.getId()))
                .orElseThrow(() -> new AppException(ServiceErrorCode.CURRICULUM_NOT_FOUND, "Curriculum not found"));

        if (packageCurriculumRepository.existsByPackageVersionIdAndOrderIndexAndIdNot(
                ver.getId(), request.getOrderIndex(), curriculumId)) {
            throw new AppException(ServiceErrorCode.DUPLICATE_CURRICULUM_ORDER_INDEX,
                    "Order index must be unique within the package version");
        }

        curriculum.setTitle(request.getTitle());
        curriculum.setDescription(request.getDescription());
        curriculum.setOrderIndex(request.getOrderIndex());
        curriculum.setDuration(request.getDuration());
        PackageCurriculum saved = packageCurriculumRepository.save(curriculum);
        return mapCurriculum(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CurriculumItemResponse> listCurriculum(UUID mentorId, UUID packageId, UUID versionId, Pageable pageable) {
        ServicePackageVersion ver = requireOwnedVersion(mentorId, packageId, versionId);
        return packageCurriculumRepository.findByPackageVersionIdOrderByOrderIndexAsc(ver.getId(), pageable)
                .map(this::mapCurriculum);
    }

    @Override
    @Transactional
    public void deleteCurriculumItem(UUID mentorId, UUID curriculumId) {
        PackageCurriculum c = packageCurriculumRepository.findById(curriculumId)
                .orElseThrow(() -> new AppException(ServiceErrorCode.CURRICULUM_NOT_FOUND, "Curriculum not found"));
        ServicePackageVersion ver = servicePackageVersionRepository.findById(c.getPackageVersionId())
                .orElseThrow(() -> new AppException(ServiceErrorCode.SERVICE_VERSION_NOT_FOUND, "Version not found"));
        ServicePackage pkg = requireOwnedPackage(mentorId, ver.getPackageId());
        assertPackageNotArchived(pkg);
        packageCurriculumRepository.delete(c);
    }

    @Override
    @Transactional
    public void deleteCurriculumItem(UUID mentorId, UUID packageId, UUID versionId, UUID curriculumId) {
        ServicePackageVersion version = requireOwnedVersionForMutation(mentorId, packageId, versionId);
        PackageCurriculum curriculum = packageCurriculumRepository.findById(curriculumId)
                .filter(item -> item.getPackageVersionId().equals(version.getId()))
                .orElseThrow(() -> new AppException(ServiceErrorCode.CURRICULUM_NOT_FOUND, "Curriculum not found"));
        packageCurriculumRepository.delete(curriculum);
    }

    private ServicePackage requireOwnedPackage(UUID mentorId, UUID packageId) {
        return servicePackageRepository.findById(packageId)
                .filter(p -> p.getMentorId().equals(mentorId))
                .orElseThrow(() -> new AppException(ServiceErrorCode.PACKAGE_NOT_FOUND, "Package not found"));
    }

    private void assertPackageNotArchived(ServicePackage pkg) {
        if (pkg.isDeleted()) {
            throw new AppException(ServiceErrorCode.PACKAGE_ALREADY_ARCHIVED, "Package already archived");
        }
    }

    private ServicePackageVersion requireOwnedVersion(UUID mentorId, UUID packageId, UUID versionId) {
        ServicePackage pkg = requireOwnedPackage(mentorId, packageId);
        return requireVersionForPackage(pkg, versionId);
    }

    private ServicePackageVersion requireOwnedVersionForMutation(UUID mentorId, UUID packageId, UUID versionId) {
        ServicePackage pkg = requireOwnedPackage(mentorId, packageId);
        assertPackageNotArchived(pkg);
        return requireVersionForPackage(pkg, versionId);
    }

    private ServicePackageVersion requireVersionForPackage(ServicePackage pkg, UUID versionId) {
        return servicePackageVersionRepository.findById(versionId)
                .filter(v -> v.getPackageId().equals(pkg.getId()))
                .orElseThrow(() -> new AppException(ServiceErrorCode.SERVICE_VERSION_NOT_FOUND, "Version not found"));
    }

    private CurriculumItemResponse mapCurriculum(PackageCurriculum c) {
        return CurriculumItemResponse.builder()
                .id(c.getId())
                .packageVersionId(c.getPackageVersionId())
                .title(c.getTitle())
                .description(c.getDescription())
                .orderIndex(c.getOrderIndex())
                .duration(c.getDuration())
                .build();
    }

    private PackageCurriculum mapCurriculumRequest(UUID versionId, CreateServicePackageRequest.CurriculumRequest request) {
        PackageCurriculum curriculum = new PackageCurriculum();
        curriculum.setPackageVersionId(versionId);
        curriculum.setTitle(request.getTitle());
        curriculum.setDescription(request.getDescription());
        curriculum.setOrderIndex(request.getOrderIndex());
        curriculum.setDuration(request.getDuration());
        return curriculum;
    }

    private PackageCurriculum cloneCurriculum(UUID versionId, PackageCurriculum source) {
        PackageCurriculum curriculum = new PackageCurriculum();
        curriculum.setPackageVersionId(versionId);
        curriculum.setTitle(source.getTitle());
        curriculum.setDescription(source.getDescription());
        curriculum.setOrderIndex(source.getOrderIndex());
        curriculum.setDuration(source.getDuration());
        return curriculum;
    }

    private static String normalizeKeyword(String raw) {
        if (raw == null) {
            return null;
        }
        String t = raw.trim();
        if (t.isEmpty()) {
            return null;
        }
        return t.length() > 100 ? t.substring(0, 100) : t;
    }

    private void validateCreatePackageRequest(CreateServicePackageRequest request) {
        if (request.getCurriculums() == null || request.getCurriculums().isEmpty()) {
            throw new AppException(ServiceErrorCode.PACKAGE_CURRICULUM_REQUIRED,
                    "Gói dịch vụ phải có ít nhất một curriculum");
        }

        Set<Integer> orderIndexes = request.getCurriculums().stream()
                .map(CreateServicePackageRequest.CurriculumRequest::getOrderIndex)
                .collect(Collectors.toSet());
        if (orderIndexes.size() != request.getCurriculums().size()) {
            throw new AppException(ServiceErrorCode.DUPLICATE_CURRICULUM_ORDER_INDEX,
                    "Thứ tự curriculum không được trùng nhau trong cùng một gói");
        }
    }
}
