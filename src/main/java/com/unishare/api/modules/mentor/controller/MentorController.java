package com.unishare.api.modules.mentor.controller;

import com.unishare.api.common.constants.MentorVerificationStatuses;
import com.unishare.api.common.dto.ApiResponse;
import com.unishare.api.config.OpenApiConfig;
import com.unishare.api.infrastructure.security.CustomUserPrincipal;
import com.unishare.api.modules.mentor.dto.MentorRequest;
import com.unishare.api.modules.mentor.dto.MentorResponse;
import com.unishare.api.modules.mentor.service.MentorService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/mentors")
@RequiredArgsConstructor
@Tag(name = "Mentor profile")
public class MentorController {

    private final MentorService mentorProfileService;

    @Operation(summary = "Danh sach mentor (mac dinh da xac minh) — loc: q, minBasePrice, maxBasePrice")
    @SecurityRequirements(value = {})
    @GetMapping
    public ResponseEntity<ApiResponse<Page<MentorResponse>>> listMentors(
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "minBasePrice", required = false) BigDecimal minBasePrice,
            @RequestParam(value = "maxBasePrice", required = false) BigDecimal maxBasePrice,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.<Page<MentorResponse>>build()
                .withData(mentorProfileService.searchMentors(
                        MentorVerificationStatuses.VERIFIED, q, minBasePrice, maxBasePrice, pageable)));
    }

    @Operation(summary = "Chi tiet mentor")
    @PermitAll
    @SecurityRequirements(value = {})
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MentorResponse>> getMentorProfile(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(ApiResponse.<MentorResponse>build()
                .withData(mentorProfileService.getMentorProfile(id)));
    }

    @Operation(summary = "Cap nhat ho so mentor (me)")
    @SecurityRequirement(name = OpenApiConfig.BEARER_JWT)
    @PreAuthorize("hasRole('MENTOR')")
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<MentorResponse>> updateMyProfile(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody MentorRequest request) {
        MentorResponse resp = mentorProfileService.createOrUpdateProfile(principal.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.<MentorResponse>build()
                .withData(resp));
    }

    @Operation(summary = "Lấy hồ sơ mentor của tôi")
    @SecurityRequirement(name = OpenApiConfig.BEARER_JWT)
    @PreAuthorize("hasRole('MENTOR')")
    @GetMapping("/me/profile")
    public ResponseEntity<ApiResponse<MentorResponse>> getMyProfile(
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.<MentorResponse>build()
                .withData(mentorProfileService.getMentorProfile(principal.getUserId())));
    }

    @Operation(summary = "Gửi yêu cầu duyệt hồ sơ mentor")
    @SecurityRequirement(name = OpenApiConfig.BEARER_JWT)
    @PreAuthorize("hasRole('MENTOR')")
    @PostMapping("/me/profile/submit")
    public ResponseEntity<ApiResponse<MentorResponse>> submitProfileVerification(
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.<MentorResponse>build()
                .withData(mentorProfileService.submitProfileVerification(principal.getUserId()))
                .withMessage("Gửi yêu cầu duyệt hồ sơ thành công"));
    }
}
