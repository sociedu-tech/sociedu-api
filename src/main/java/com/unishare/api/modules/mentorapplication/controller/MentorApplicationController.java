package com.unishare.api.modules.mentorapplication.controller;

import com.unishare.api.common.dto.ApiResponse;
import com.unishare.api.config.OpenApiConfig;
import com.unishare.api.infrastructure.security.CustomUserPrincipal;
import com.unishare.api.modules.mentorapplication.dto.MentorApplicationPayload;
import com.unishare.api.modules.mentorapplication.dto.MentorApplicationResponse;
import com.unishare.api.modules.mentorapplication.service.MentorApplicationService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/mentor-requests")
@RequiredArgsConstructor
@SecurityRequirement(name = OpenApiConfig.BEARER_JWT)
@Tag(name = "Mentor applications")
public class MentorApplicationController {

    private final MentorApplicationService mentorApplicationService;

    @PostMapping
    public ResponseEntity<ApiResponse<MentorApplicationResponse>> submit(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody MentorApplicationPayload payload) {
        return ResponseEntity.ok(ApiResponse.<MentorApplicationResponse>build()
                .withData(mentorApplicationService.submit(principal.getUserId(), payload)));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MentorApplicationResponse>> getMyCurrent(
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.<MentorApplicationResponse>build()
                .withData(mentorApplicationService.getMyCurrent(principal.getUserId())));
    }

    @PostMapping("/me/resubmit")
    public ResponseEntity<ApiResponse<MentorApplicationResponse>> resubmit(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody MentorApplicationPayload payload) {
        return ResponseEntity.ok(ApiResponse.<MentorApplicationResponse>build()
                .withData(mentorApplicationService.resubmit(principal.getUserId(), payload)));
    }
}
