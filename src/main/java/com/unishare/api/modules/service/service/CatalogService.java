package com.unishare.api.modules.service.service;

import com.unishare.api.modules.mentor.service.MentorService;
import com.unishare.api.modules.service.dto.MentorDto.CurriculumItemRequest;
import com.unishare.api.modules.service.dto.MentorDto.CurriculumItemResponse;
import com.unishare.api.modules.service.dto.MentorDto.ServicePackageResponse;
import com.unishare.api.modules.service.dto.MentorDto.ServicePackageVersionResponse;
import com.unishare.api.modules.service.dto.request.CreateServicePackageRequest;
import com.unishare.api.modules.service.dto.request.CreateServicePackageVersionRequest;
import com.unishare.api.modules.service.dto.request.UpdateServicePackageRequest;
import com.unishare.api.modules.service.dto.request.UpdateServicePackageVersionRequest;
import com.unishare.api.modules.service.dto.request.SaveMentorPackagesRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

/**
 * Gói dịch vụ / curriculum (catalog) — tách khỏi {@link MentorService}.
 */
public interface CatalogService {

    List<ServicePackageResponse> savePackages(UUID mentorId, List<SaveMentorPackagesRequest.MentorPackageRequest> packages);

    Page<ServicePackageResponse> getMentorPackages(UUID mentorId, String keyword, Pageable pageable);

    Page<ServicePackageResponse> getMyPackages(UUID mentorId, String keyword, Pageable pageable);

    ServicePackageResponse getMyPackage(UUID mentorId, UUID packageId);

    /** Gói đang mở; lọc theo mentorId và/hoặc từ khóa tên/mô tả. */
    Page<ServicePackageResponse> getActivePackages(UUID mentorId, String keyword, Pageable pageable);

    ServicePackageResponse getActivePackage(UUID packageId);

    ServicePackageResponse createPackage(UUID mentorId, CreateServicePackageRequest request);

    ServicePackageResponse createPackageVersion(UUID mentorId, UUID packageId, CreateServicePackageVersionRequest request);

    Page<ServicePackageVersionResponse> getPackageVersions(UUID mentorId, UUID packageId, Pageable pageable);

    ServicePackageVersionResponse getPackageVersion(UUID mentorId, UUID packageId, UUID versionId);

    ServicePackageResponse updatePackage(UUID mentorId, UUID packageId, UpdateServicePackageRequest request);

    ServicePackageResponse togglePackage(UUID mentorId, UUID packageId);

    void deletePackage(UUID mentorId, UUID packageId);

    CurriculumItemResponse addCurriculumItem(UUID mentorId, UUID packageId, UUID versionId, CurriculumItemRequest request);

    CurriculumItemResponse updateCurriculumItem(UUID mentorId, UUID packageId, UUID versionId, UUID curriculumId, CurriculumItemRequest request);

    Page<CurriculumItemResponse> listCurriculum(UUID mentorId, UUID packageId, UUID versionId, Pageable pageable);

    void deleteCurriculumItem(UUID mentorId, UUID curriculumId);

    void deleteCurriculumItem(UUID mentorId, UUID packageId, UUID versionId, UUID curriculumId);

    /** Public: danh sách versions của package active (không cần auth). */
    Page<ServicePackageVersionResponse> getActivePackageVersions(UUID packageId, Pageable pageable);

    /** Public: chi tiết version của package active (không cần auth). */
    ServicePackageVersionResponse getActivePackageVersion(UUID packageId, UUID versionId);

    /** Public: danh sách curriculum của version thuộc package active (không cần auth). */
    Page<CurriculumItemResponse> listActiveCurriculum(UUID packageId, UUID versionId, Pageable pageable);

    ServicePackageVersionResponse updatePackageVersion(UUID mentorId, UUID packageId, UUID versionId, UpdateServicePackageVersionRequest request);

    void deletePackageVersion(UUID mentorId, UUID packageId, UUID versionId);

    ServicePackageVersionResponse setDefaultVersion(UUID mentorId, UUID packageId, UUID versionId);
}
