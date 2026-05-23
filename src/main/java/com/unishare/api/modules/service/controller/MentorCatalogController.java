package com.unishare.api.modules.service.controller;

import com.unishare.api.common.dto.ApiResponse;
import com.unishare.api.config.OpenApiConfig;
import com.unishare.api.infrastructure.security.CustomUserPrincipal;
import com.unishare.api.modules.service.dto.MentorDto.CurriculumItemRequest;
import com.unishare.api.modules.service.dto.MentorDto.CurriculumItemResponse;
import com.unishare.api.modules.service.dto.MentorDto.ServicePackageResponse;
import com.unishare.api.modules.service.dto.request.CreateServicePackageRequest;
import com.unishare.api.modules.service.service.CatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.PermitAll;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/mentors")
@RequiredArgsConstructor
@Tag(name = "Mentor catalog (packages)")
public class MentorCatalogController {

    private final CatalogService catalogService;

    // TODO(mentor-finance): thay placeholder bằng MentorStatsService thật khi feature ready.
    @Operation(summary = "Thong ke tong quan mentor (placeholder)")
    @SecurityRequirement(name = OpenApiConfig.BEARER_JWT)
    @PreAuthorize("hasRole('MENTOR')")
    @GetMapping("/me/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMyStats(
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("mentorId", principal.getUserId());
        stats.put("totalStudents", 0);
        stats.put("totalSessions", 0);
        stats.put("totalRevenue", 0);
        stats.put("ratingAvg", 0);
        return ResponseEntity.ok(ApiResponse.<Map<String, Object>>build().withData(stats));
    }

    @Operation(summary = "Danh sach yeu cau rut tien (placeholder)")
    @SecurityRequirement(name = OpenApiConfig.BEARER_JWT)
    @PreAuthorize("hasRole('MENTOR')")
    @GetMapping("/me/withdrawals")
    public ResponseEntity<ApiResponse<List<Object>>> getMyWithdrawals(
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.<List<Object>>build().withData(Collections.emptyList()));
    }

    @Operation(summary = "Goi dich vu dang mo cua mentor (loc: q ten/mo ta)")
    @PermitAll
    @SecurityRequirements(value = {})
    @GetMapping("/{id}/packages")
    public ResponseEntity<ApiResponse<Page<ServicePackageResponse>>> getMentorPackages(
            @PathVariable("id") UUID id,
            @RequestParam(value = "q", required = false) String q,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.<Page<ServicePackageResponse>>build()
                .withData(catalogService.getMentorPackages(id, q, pageable)));
    }

    @Operation(summary = "Danh sach goi dich vu cua toi (loc: q ten/mo ta)")
    @SecurityRequirement(name = OpenApiConfig.BEARER_JWT)
    @PreAuthorize("hasRole('MENTOR')")
    @GetMapping("/me/packages")
    public ResponseEntity<ApiResponse<Page<ServicePackageResponse>>> getMyPackages(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @RequestParam(value = "q", required = false) String q,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.<Page<ServicePackageResponse>>build()
                .withData(catalogService.getMyPackages(principal.getUserId(), q, pageable)));
    }

    @Operation(summary = "Chi tiet goi dich vu cua toi")
    @SecurityRequirement(name = OpenApiConfig.BEARER_JWT)
    @PreAuthorize("hasRole('MENTOR')")
    @GetMapping("/me/packages/{pkgId}")
    public ResponseEntity<ApiResponse<ServicePackageResponse>> getMyPackage(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable("pkgId") UUID pkgId) {
        return ResponseEntity.ok(ApiResponse.<ServicePackageResponse>build()
                .withData(catalogService.getMyPackage(principal.getUserId(), pkgId)));
    }

    @Operation(summary = "Tao goi dich vu")
    @SecurityRequirement(name = OpenApiConfig.BEARER_JWT)
    @PreAuthorize("hasRole('MENTOR')")
    @PostMapping("/me/packages")
    public ResponseEntity<ApiResponse<ServicePackageResponse>> addPackage(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody CreateServicePackageRequest request) {
        return ResponseEntity.ok(ApiResponse.<ServicePackageResponse>build()
                .withData(catalogService.createPackage(principal.getUserId(), request)));
    }

    @Operation(summary = "Xoa goi dich vu")
    @SecurityRequirement(name = OpenApiConfig.BEARER_JWT)
    @PreAuthorize("hasRole('MENTOR')")
    @DeleteMapping("/me/packages/{pkgId}")
    public ResponseEntity<ApiResponse<Void>> deletePackage(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable("pkgId") UUID pkgId) {
        catalogService.deletePackage(principal.getUserId(), pkgId);
        return ResponseEntity.ok(ApiResponse.<Void>build().withMessage("Package deleted"));
    }

    @Operation(summary = "Them muc curriculum cho phien ban goi")
    @SecurityRequirement(name = OpenApiConfig.BEARER_JWT)
    @PreAuthorize("hasRole('MENTOR')")
    @PostMapping("/me/packages/{pkgId}/versions/{verId}/curriculums")
    public ResponseEntity<ApiResponse<CurriculumItemResponse>> addCurriculum(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable("pkgId") UUID pkgId,
            @PathVariable("verId") UUID verId,
            @Valid @RequestBody CurriculumItemRequest request) {
        return ResponseEntity.ok(ApiResponse.<CurriculumItemResponse>build()
                .withData(catalogService.addCurriculumItem(principal.getUserId(), pkgId, verId, request)));
    }

    @Operation(summary = "Liet ke curriculum theo phien ban goi")
    @SecurityRequirement(name = OpenApiConfig.BEARER_JWT)
    @PreAuthorize("hasRole('MENTOR')")
    @GetMapping("/me/packages/{pkgId}/versions/{verId}/curriculums")
    public ResponseEntity<ApiResponse<Page<CurriculumItemResponse>>> listCurriculum(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable("pkgId") UUID pkgId,
            @PathVariable("verId") UUID verId,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.<Page<CurriculumItemResponse>>build()
                .withData(catalogService.listCurriculum(principal.getUserId(), pkgId, verId, pageable)));
    }

    @Operation(summary = "Xoa muc curriculum")
    @SecurityRequirement(name = OpenApiConfig.BEARER_JWT)
    @PreAuthorize("hasRole('MENTOR')")
    @DeleteMapping("/me/curriculums/{curriculumId}")
    public ResponseEntity<ApiResponse<Void>> deleteCurriculum(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable("curriculumId") UUID curriculumId) {
        catalogService.deleteCurriculumItem(principal.getUserId(), curriculumId);
        return ResponseEntity.ok(ApiResponse.<Void>build().withMessage("Da xoa muc curriculum"));
    }
}
