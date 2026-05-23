package com.unishare.api.modules.file.service.impl;

import com.unishare.api.common.dto.AppException;
import com.unishare.api.infrastructure.storage.StoredFileLocation;
import com.unishare.api.modules.file.entity.StoredFile;
import com.unishare.api.modules.file.exception.FileErrorCode;
import com.unishare.api.modules.file.repository.StoredFileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.UUID;

/**
 * Tách phần ghi DB của module file thành bean riêng để {@link org.springframework.transaction.annotation.Transactional}
 * thực sự được Spring AOP bắt (tránh self-invocation từ FileServiceImpl).
 *
 * <p>Mỗi method chỉ bọc đúng phần thao tác DB — không gọi storage provider trong transaction.</p>
 */
@Component
@RequiredArgsConstructor
class FileMetadataWriter {

    private final StoredFileRepository storedFileRepository;

    @Transactional
    public StoredFile insert(UUID uploaderId, MultipartFile file, StoredFileLocation location,
                             String visibility, String entityType, UUID entityId, String fileName) {
        StoredFile sf = new StoredFile();
        sf.setUploaderId(uploaderId);
        sf.setFileName(fileName);
        sf.setFileUrl(location.getUrl());
        sf.setPublicId(location.getPublicId());
        sf.setResourceType(location.getResourceType());
        sf.setMimeType(file.getContentType() != null ? file.getContentType() : "application/octet-stream");
        sf.setFileSize(file.getSize());
        sf.setVisibility(visibility);
        sf.setEntityType(entityType);
        sf.setEntityId(entityId);
        return storedFileRepository.save(sf);
    }

    @Transactional
    public StoredFile markDeleted(UUID fileId, UUID requesterUserId) {
        StoredFile sf = storedFileRepository.findByIdAndDeletedAtIsNull(fileId)
                .orElseThrow(() -> new AppException(FileErrorCode.FILE_NOT_FOUND));
        if (sf.getUploaderId() == null || !sf.getUploaderId().equals(requesterUserId)) {
            throw new AppException(FileErrorCode.FILE_ACCESS_DENIED);
        }
        sf.setDeletedAt(Instant.now());
        return storedFileRepository.save(sf);
    }
}
