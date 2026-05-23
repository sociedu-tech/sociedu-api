package com.unishare.api.modules.file.service.impl;

import com.unishare.api.common.constants.FileVisibility;
import com.unishare.api.common.dto.AppException;
import com.unishare.api.infrastructure.storage.FileStorageService;
import com.unishare.api.infrastructure.storage.StoredFileLocation;
import com.unishare.api.modules.file.FileUploadConstraints;
import com.unishare.api.modules.file.dto.FileUploadResponse;
import com.unishare.api.modules.file.entity.StoredFile;
import com.unishare.api.modules.file.exception.FileErrorCode;
import com.unishare.api.modules.file.repository.StoredFileRepository;
import com.unishare.api.modules.file.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private final StoredFileRepository storedFileRepository;
    private final FileStorageService fileStorageService;
    private final FileMetadataWriter metadataWriter;

    @Override
    public FileUploadResponse upload(UUID uploaderId, MultipartFile file, String folder, String visibility,
                                     String entityType, UUID entityId) {
        // Validate max size: 10MB
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new AppException(FileErrorCode.FILE_SIZE_LIMIT_EXCEEDED, "File size exceeds maximum limit of 10MB");
        }

        // Validate extension whitelist
        String filename = file.getOriginalFilename();
        if (filename != null && !filename.isBlank()) {
            int dotIdx = filename.lastIndexOf('.');
            if (dotIdx > 0) {
                String ext = filename.substring(dotIdx + 1).toLowerCase();
                java.util.List<String> whitelist = java.util.List.of("jpg", "jpeg", "png", "gif", "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "zip", "rar");
                if (!whitelist.contains(ext)) {
                    throw new AppException(FileErrorCode.INVALID_FILE_TYPE, "File type not allowed. Whitelist: jpg, jpeg, png, gif, pdf, doc, docx, xls, xlsx, ppt, pptx, txt, zip, rar");
                }
            }
        }

        String url = fileStorageService.uploadFile(file, folder != null ? folder : "uploads");
        StoredFile sf = new StoredFile();
        sf.setUploaderId(uploaderId);
        sf.setFileName(file.getOriginalFilename() != null ? file.getOriginalFilename() : "file");
        sf.setFileUrl(url);
        sf.setMimeType(file.getContentType() != null ? file.getContentType() : "application/octet-stream");
        sf.setFileSize(file.getSize());
        sf.setVisibility(visibility != null ? visibility : FileVisibility.PRIVATE);
        sf.setEntityType(entityType);
        sf.setEntityId(entityId);
        sf = storedFileRepository.save(sf);
        return toResponse(sf);
    }

    @Override
    @Transactional(readOnly = true)
    public FileUploadResponse getFile(UUID fileId, UUID requesterUserId) {
        StoredFile sf = storedFileRepository.findByIdAndDeletedAtIsNull(fileId)
                .orElseThrow(() -> new AppException(FileErrorCode.FILE_NOT_FOUND));
        if (!FileVisibility.PUBLIC.equalsIgnoreCase(sf.getVisibility())
                && (sf.getUploaderId() == null || !sf.getUploaderId().equals(requesterUserId))) {
            throw new AppException(FileErrorCode.FILE_ACCESS_DENIED);
        }
        return toResponse(sf);
    }

    @Override
    public void softDelete(UUID fileId, UUID requesterUserId) {
        // Đánh dấu deleted_at trong transaction, sau đó best-effort xóa object trên storage.
        StoredFile sf = metadataWriter.markDeleted(fileId, requesterUserId);
        safeDeleteFromStorage(sf.getPublicId(), sf.getResourceType());
    }

    private void safeDeleteFromStorage(String publicId, String resourceType) {
        if (publicId == null || publicId.isBlank()) {
            return;
        }
        try {
            boolean ok = fileStorageService.deleteByPublicId(publicId, resourceType);
            if (!ok) {
                log.warn("Storage delete returned false: publicId={}, resourceType={}", publicId, resourceType);
            }
        } catch (Exception e) {
            log.warn("Storage delete threw exception: publicId={}, resourceType={}", publicId, resourceType, e);
        }
    }

    private static void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() <= 0) {
            throw new AppException(FileErrorCode.FILE_EMPTY, "File rỗng hoặc không hợp lệ");
        }
        if (file.getSize() > FileUploadConstraints.MAX_FILE_SIZE_BYTES) {
            throw new AppException(FileErrorCode.FILE_TOO_LARGE, "Kích thước file vượt giới hạn cho phép");
        }
        if (!FileUploadConstraints.isMimeAllowed(file.getContentType())) {
            throw new AppException(FileErrorCode.FILE_TYPE_NOT_ALLOWED,
                    "Định dạng file không được hỗ trợ: " + file.getContentType());
        }
    }

    private static String sanitizeFolder(String folder) {
        if (folder == null || folder.isBlank()) {
            return FileUploadConstraints.DEFAULT_FOLDER;
        }
        String trimmed = folder.trim();
        if (!FileUploadConstraints.FOLDER_PATTERN.matcher(trimmed).matches()) {
            throw new AppException(FileErrorCode.FILE_INVALID_FOLDER,
                    "Tên thư mục chỉ cho phép chữ/số/dấu '_', '-', '/' (tối đa 64 ký tự)");
        }
        return trimmed;
    }

    private static String safeName(String original) {
        if (original == null || original.isBlank()) {
            return "file";
        }
        String base = original.replace('\\', '/');
        int slash = base.lastIndexOf('/');
        if (slash >= 0) {
            base = base.substring(slash + 1);
        }
        return base.length() > 255 ? base.substring(0, 255) : base;
    }

    private static FileUploadResponse toResponse(StoredFile sf) {
        return FileUploadResponse.builder()
                .id(sf.getId())
                .fileName(sf.getFileName())
                .fileUrl(sf.getFileUrl())
                .mimeType(sf.getMimeType())
                .fileSize(sf.getFileSize())
                .visibility(sf.getVisibility())
                .createdAt(sf.getCreatedAt())
                .build();
    }
}
