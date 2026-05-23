package com.unishare.api.modules.trust.controller;

import com.unishare.api.common.dto.ApiResponse;
import com.unishare.api.config.OpenApiConfig;
import com.unishare.api.infrastructure.security.CustomUserPrincipal;
import com.unishare.api.modules.trust.dto.*;
import com.unishare.api.modules.trust.service.TrustService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/disputes")
@RequiredArgsConstructor
@SecurityRequirement(name = OpenApiConfig.BEARER_JWT)
@Tag(name = "Trust alias — Disputes")
@Deprecated
public class DisputeAliasController {

    private final TrustService trustService;

    @Operation(summary = "Tạo tranh chấp (Alias)")
    @PreAuthorize("hasAuthority(T(com.unishare.api.common.constants.Capabilities).CREATE_DISPUTE)")
    @PostMapping
    public ResponseEntity<ApiResponse<DisputeResponse>> createDispute(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody CreateDisputeRequest request) {
        return ResponseEntity.ok(ApiResponse.<DisputeResponse>build()
                .withData(trustService.createDispute(principal.getUserId(), request)));
    }

    @Operation(summary = "Tranh chấp của tôi (Alias)")
    @PreAuthorize("hasAuthority(T(com.unishare.api.common.constants.Capabilities).VIEW_OWN_DISPUTE)")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<DisputeResponse>>> myDisputes(
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.<List<DisputeResponse>>build()
                .withData(trustService.myDisputes(principal.getUserId())));
    }

    @Operation(summary = "Giải quyết tranh chấp (Alias)")
    @PutMapping("/{disputeId}/resolve")
    @PreAuthorize("hasAuthority(T(com.unishare.api.common.constants.Capabilities).RESOLVE_DISPUTE)")
    public ResponseEntity<ApiResponse<DisputeResponse>> resolveDispute(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable UUID disputeId,
            @Valid @RequestBody ResolveDisputeRequest request) {
        return ResponseEntity.ok(ApiResponse.<DisputeResponse>build()
                .withData(trustService.resolveDispute(principal.getUserId(), disputeId, request)));
    }
}
