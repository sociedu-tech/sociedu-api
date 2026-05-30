package com.unishare.api.modules.admin.controller;

import com.unishare.api.common.dto.ApiResponse;
import com.unishare.api.common.dto.PageResponse;
import com.unishare.api.config.OpenApiConfig;
import com.unishare.api.modules.admin.dto.AdminBookingResponse;
import com.unishare.api.modules.admin.service.AdminBookingService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/bookings")
@RequiredArgsConstructor
@SecurityRequirement(name = OpenApiConfig.BEARER_JWT)
@Tag(name = "Admin - Bookings")
@PreAuthorize("hasRole('ADMIN')")
public class AdminBookingController {

    private final AdminBookingService adminBookingService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AdminBookingResponse>>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.<PageResponse<AdminBookingResponse>>build()
                .withData(adminBookingService.list(status, q, pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminBookingResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.<AdminBookingResponse>build()
                .withData(adminBookingService.getById(id)));
    }
}
