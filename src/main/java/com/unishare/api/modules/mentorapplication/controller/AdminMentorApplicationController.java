package com.unishare.api.modules.mentorapplication.controller;

import com.unishare.api.common.dto.ApiResponse;
import com.unishare.api.common.dto.PageResponse;
import com.unishare.api.config.OpenApiConfig;
import com.unishare.api.infrastructure.security.CustomUserPrincipal;
import com.unishare.api.modules.mentorapplication.dto.*;
import com.unishare.api.modules.mentorapplication.service.MentorApplicationService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/mentor-requests")
@RequiredArgsConstructor
@SecurityRequirement(name = OpenApiConfig.BEARER_JWT)
@Tag(name = "Admin - Mentor applications")
@PreAuthorize("hasRole('ADMIN')")
public class AdminMentorApplicationController {

    private final MentorApplicationService mentorApplicationService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<MentorApplicationResponse>>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.<PageResponse<MentorApplicationResponse>>build()
                .withData(mentorApplicationService.adminList(status, q, pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MentorApplicationResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.<MentorApplicationResponse>build()
                .withData(mentorApplicationService.adminGet(id)));
    }

    @PostMapping("/{id}/actions/approve")
    public ResponseEntity<ApiResponse<MentorApplicationResponse>> approve(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable UUID id,
            @RequestBody(required = false) AdminApproveMentorApplicationRequest request) {
        return ResponseEntity.ok(ApiResponse.<MentorApplicationResponse>build()
                .withData(mentorApplicationService.adminApprove(id, principal.getUserId(), request)));
    }

    @PostMapping("/{id}/actions/reject")
    public ResponseEntity<ApiResponse<MentorApplicationResponse>> reject(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody AdminRejectMentorApplicationRequest request) {
        return ResponseEntity.ok(ApiResponse.<MentorApplicationResponse>build()
                .withData(mentorApplicationService.adminReject(id, principal.getUserId(), request)));
    }
}
