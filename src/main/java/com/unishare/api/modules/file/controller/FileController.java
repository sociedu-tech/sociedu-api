package com.unishare.api.modules.file.controller;

import com.unishare.api.common.dto.ApiResponse;
import com.unishare.api.config.OpenApiConfig;
import com.unishare.api.infrastructure.security.CustomUserPrincipal;
import com.unishare.api.modules.file.dto.FileUploadResponse;
import com.unishare.api.modules.file.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
@SecurityRequirement(name = OpenApiConfig.BEARER_JWT)
@Tag(name = "Files")
public class FileController {

    private final FileService fileService;

    @Operation(summary = "Upload file")
    @PreAuthorize("isAuthenticated()")
    @PostMapping(value = {"", "/upload"}, consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<FileUploadResponse>> upload(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) String folder,
            @RequestParam(required = false, defaultValue = "private") String visibility,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) UUID entityId) {
        FileUploadResponse data = fileService.upload(
                principal.getUserId(), file, folder, visibility, entityType, entityId);
        return ResponseEntity.ok(ApiResponse.<FileUploadResponse>build()
                .withData(data)
                .withMessage("Upload thành công"));
    }

    @Operation(summary = "Metadata file theo id")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FileUploadResponse>> get(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.<FileUploadResponse>build()
                .withData(fileService.getFile(id, principal.getUserId())));
    }

    @Operation(summary = "Xóa mềm file")
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable UUID id) {
        fileService.softDelete(id, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.<Void>build().withMessage("Đã xóa file"));
    }
}
