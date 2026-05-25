package com.unishare.api.modules.service.controller;

import com.unishare.api.common.dto.ApiResponse;
import com.unishare.api.config.OpenApiConfig;
import com.unishare.api.infrastructure.security.CustomUserPrincipal;
import com.unishare.api.modules.service.dto.MentorDto.CurriculumItemRequest;
import com.unishare.api.modules.service.dto.MentorDto.CurriculumItemResponse;
import com.unishare.api.modules.service.dto.MentorDto.ServicePackageResponse;
import com.unishare.api.modules.service.dto.MentorDto.ServicePackageVersionResponse;
import com.unishare.api.modules.service.dto.request.CreateServicePackageRequest;
import com.unishare.api.modules.service.dto.request.CreateServicePackageVersionRequest;
import com.unishare.api.modules.service.dto.request.UpdateServicePackageRequest;
import com.unishare.api.modules.service.dto.request.UpdateServicePackageVersionRequest;
import com.unishare.api.modules.service.service.CatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/service-packages")
@RequiredArgsConstructor
@Tag(name = "Service packages")
@SecurityRequirements(value = {})
public class ServicePackageController {

    private final CatalogService catalogService;

    @Operation(summary = "Danh sach goi dich vu dang mo (loc: mentorId, q ten/mo ta)")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<ServicePackageResponse>>> getActivePackages(
            @RequestParam(value = "mentorId", required = false) UUID mentorId,
            @RequestParam(value = "q", required = false) String q,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.<Page<ServicePackageResponse>>build()
                .withData(catalogService.getActivePackages(mentorId, q, pageable)));
    }

    @Operation(summary = "Chi tiet goi dich vu dang mo")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ServicePackageResponse>> getActivePackage(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(ApiResponse.<ServicePackageResponse>build()
                .withData(catalogService.getActivePackage(id)));
    }

    @Operation(summary = "Tao goi dich vu")
    @SecurityRequirement(name = OpenApiConfig.BEARER_JWT)
    @PreAuthorize("hasRole('MENTOR')")
    @PostMapping
    public ResponseEntity<ApiResponse<ServicePackageResponse>> createPackage(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody CreateServicePackageRequest request) {
        return ResponseEntity.ok(ApiResponse.<ServicePackageResponse>build()
                .withData(catalogService.createPackage(principal.getUserId(), request))
                .withMessage("Tao goi dich vu thanh cong"));
    }

    @Operation(summary = "Tao version moi cho goi dich vu cua mentor")
    @SecurityRequirement(name = OpenApiConfig.BEARER_JWT)
    @PreAuthorize("hasRole('MENTOR')")
    @PostMapping("/{id}/versions")
    public ResponseEntity<ApiResponse<ServicePackageResponse>> createPackageVersion(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable("id") UUID id,
            @Valid @RequestBody CreateServicePackageVersionRequest request) {
        return ResponseEntity.ok(ApiResponse.<ServicePackageResponse>build()
                .withData(catalogService.createPackageVersion(principal.getUserId(), id, request))
                .withMessage("Tao version goi dich vu thanh cong"));
    }

    @Operation(summary = "Danh sach version cua goi dich vu (public)")
    @GetMapping("/{id}/versions")
    public ResponseEntity<ApiResponse<Page<ServicePackageVersionResponse>>> getPackageVersions(
            @PathVariable("id") UUID id,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.<Page<ServicePackageVersionResponse>>build()
                .withData(catalogService.getActivePackageVersions(id, pageable)));
    }

    @Operation(summary = "Chi tiet version cua goi dich vu (public)")
    @GetMapping("/{id}/versions/{versionId}")
    public ResponseEntity<ApiResponse<ServicePackageVersionResponse>> getPackageVersion(
            @PathVariable("id") UUID id,
            @PathVariable("versionId") UUID versionId) {
        return ResponseEntity.ok(ApiResponse.<ServicePackageVersionResponse>build()
                .withData(catalogService.getActivePackageVersion(id, versionId)));
    }

    @Operation(summary = "Cap nhat version cua goi dich vu")
    @SecurityRequirement(name = OpenApiConfig.BEARER_JWT)
    @PreAuthorize("hasRole('MENTOR')")
    @PutMapping("/{id}/versions/{versionId}")
    public ResponseEntity<ApiResponse<ServicePackageVersionResponse>> updatePackageVersion(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable("id") UUID id,
            @PathVariable("versionId") UUID versionId,
            @Valid @RequestBody UpdateServicePackageVersionRequest request) {
        return ResponseEntity.ok(ApiResponse.<ServicePackageVersionResponse>build()
                .withData(catalogService.updatePackageVersion(principal.getUserId(), id, versionId, request))
                .withMessage("Cap nhat version thanh cong"));
    }

    @Operation(summary = "Xoa version cua goi dich vu")
    @SecurityRequirement(name = OpenApiConfig.BEARER_JWT)
    @PreAuthorize("hasRole('MENTOR')")
    @DeleteMapping("/{id}/versions/{versionId}")
    public ResponseEntity<ApiResponse<Void>> deletePackageVersion(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable("id") UUID id,
            @PathVariable("versionId") UUID versionId) {
        catalogService.deletePackageVersion(principal.getUserId(), id, versionId);
        return ResponseEntity.ok(ApiResponse.<Void>build()
                .withMessage("Xoa version thanh cong"));
    }

    @Operation(summary = "Dat version lam mac dinh")
    @SecurityRequirement(name = OpenApiConfig.BEARER_JWT)
    @PreAuthorize("hasRole('MENTOR')")
    @PatchMapping("/{id}/versions/{versionId}/default")
    public ResponseEntity<ApiResponse<ServicePackageVersionResponse>> setDefaultVersion(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable("id") UUID id,
            @PathVariable("versionId") UUID versionId) {
        return ResponseEntity.ok(ApiResponse.<ServicePackageVersionResponse>build()
                .withData(catalogService.setDefaultVersion(principal.getUserId(), id, versionId))
                .withMessage("Dat version mac dinh thanh cong"));
    }


    @Operation(summary = "Them curriculum vao version cua goi dich vu")
    @SecurityRequirement(name = OpenApiConfig.BEARER_JWT)
    @PreAuthorize("hasRole('MENTOR')")
    @PostMapping("/{id}/versions/{versionId}/curriculums")
    public ResponseEntity<ApiResponse<CurriculumItemResponse>> createCurriculum(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable("id") UUID id,
            @PathVariable("versionId") UUID versionId,
            @Valid @RequestBody CurriculumItemRequest request) {
        return ResponseEntity.ok(ApiResponse.<CurriculumItemResponse>build()
                .withData(catalogService.addCurriculumItem(principal.getUserId(), id, versionId, request))
                .withMessage("Tao curriculum thanh cong"));
    }

    @Operation(summary = "Danh sach curriculum cua version goi dich vu (public)")
    @GetMapping("/{id}/versions/{versionId}/curriculums")
    public ResponseEntity<ApiResponse<Page<CurriculumItemResponse>>> getCurriculums(
            @PathVariable("id") UUID id,
            @PathVariable("versionId") UUID versionId,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.<Page<CurriculumItemResponse>>build()
                .withData(catalogService.listActiveCurriculum(id, versionId, pageable)));
    }

    @Operation(summary = "Cap nhat curriculum trong mot version cua goi dich vu")
    @SecurityRequirement(name = OpenApiConfig.BEARER_JWT)
    @PreAuthorize("hasRole('MENTOR')")
    @PutMapping("/{id}/versions/{versionId}/curriculums/{curriculumId}")
    public ResponseEntity<ApiResponse<CurriculumItemResponse>> updateCurriculum(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable("id") UUID id,
            @PathVariable("versionId") UUID versionId,
            @PathVariable("curriculumId") UUID curriculumId,
            @Valid @RequestBody CurriculumItemRequest request) {
        return ResponseEntity.ok(ApiResponse.<CurriculumItemResponse>build()
                .withData(catalogService.updateCurriculumItem(principal.getUserId(), id, versionId, curriculumId, request))
                .withMessage("Cap nhat curriculum thanh cong"));
    }

    @Operation(summary = "Xoa curriculum trong mot version cua goi dich vu")
    @SecurityRequirement(name = OpenApiConfig.BEARER_JWT)
    @PreAuthorize("hasRole('MENTOR')")
    @DeleteMapping("/{id}/versions/{versionId}/curriculums/{curriculumId}")
    public ResponseEntity<ApiResponse<Void>> deleteCurriculum(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable("id") UUID id,
            @PathVariable("versionId") UUID versionId,
            @PathVariable("curriculumId") UUID curriculumId) {
        catalogService.deleteCurriculumItem(principal.getUserId(), id, versionId, curriculumId);
        return ResponseEntity.ok(ApiResponse.<Void>build()
                .withMessage("Xoa curriculum thanh cong"));
    }

    @Operation(summary = "Cap nhat goi dich vu cua mentor")
    @SecurityRequirement(name = OpenApiConfig.BEARER_JWT)
    @PreAuthorize("hasRole('MENTOR')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ServicePackageResponse>> updatePackage(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateServicePackageRequest request) {
        return ResponseEntity.ok(ApiResponse.<ServicePackageResponse>build()
                .withData(catalogService.updatePackage(principal.getUserId(), id, request))
                .withMessage("Cap nhat goi dich vu thanh cong"));
    }

    @Operation(summary = "Bat hoac tat goi dich vu cua mentor")
    @SecurityRequirement(name = OpenApiConfig.BEARER_JWT)
    @PreAuthorize("hasRole('MENTOR')")
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<ApiResponse<ServicePackageResponse>> togglePackage(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable("id") UUID id) {
        return ResponseEntity.ok(ApiResponse.<ServicePackageResponse>build()
                .withData(catalogService.togglePackage(principal.getUserId(), id))
                .withMessage("Cap nhat trang thai goi dich vu thanh cong"));
    }

    @Operation(summary = "Xoa (archive) goi dich vu")
    @SecurityRequirement(name = OpenApiConfig.BEARER_JWT)
    @PreAuthorize("hasRole('MENTOR')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePackage(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable("id") UUID id) {
        catalogService.deletePackage(principal.getUserId(), id);
        return ResponseEntity.ok(ApiResponse.<Void>build()
                .withMessage("Xoa goi dich vu thanh cong"));
    }
}
