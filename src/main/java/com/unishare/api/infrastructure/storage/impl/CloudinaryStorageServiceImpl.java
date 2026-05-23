package com.unishare.api.infrastructure.storage.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.unishare.api.common.dto.AppException;
import com.unishare.api.infrastructure.storage.FileStorageService;
import com.unishare.api.infrastructure.storage.StoredFileLocation;
import com.unishare.api.modules.file.exception.FileErrorCode;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class CloudinaryStorageServiceImpl implements FileStorageService {

    /** Bắt cấu trúc URL Cloudinary: /<resource_type>/<delivery_type>/v123/<public_id>. */
    private static final Pattern CLOUDINARY_PATH = Pattern.compile(
            "^/[^/]+/(image|video|raw)/(?:[^/]+/)?(?:v\\d+/)?(.+)$");

    @Value("${cloudinary.cloud-name}")
    private String cloudName;

    @Value("${cloudinary.api-key}")
    private String apiKey;

    @Value("${cloudinary.api-secret}")
    private String apiSecret;

    private Cloudinary cloudinary;

    @PostConstruct
    public void init() {
        cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true));
    }

    @Override
    public StoredFileLocation uploadFile(MultipartFile file, String folder) {
        try {
            Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "folder", folder,
                    "resource_type", "auto"
            ));
            String url = asString(uploadResult.get("secure_url"));
            String publicId = asString(uploadResult.get("public_id"));
            String resourceType = asString(uploadResult.get("resource_type"));
            if (url == null || publicId == null) {
                throw new AppException(FileErrorCode.FILE_UPLOAD_FAILED, "Provider không trả về thông tin file");
            }
            return StoredFileLocation.builder()
                    .url(url)
                    .publicId(publicId)
                    .resourceType(resourceType)
                    .build();
        } catch (IOException e) {
            log.error("Failed to upload file to Cloudinary", e);
            throw new AppException(FileErrorCode.FILE_UPLOAD_FAILED, "Upload thất bại");
        }
    }

    @Override
    public boolean deleteByPublicId(String publicId, String resourceType) {
        if (publicId == null || publicId.isBlank()) {
            return false;
        }
        try {
            Map<?, ?> result = cloudinary.uploader().destroy(publicId, ObjectUtils.asMap(
                    "resource_type", resourceType != null && !resourceType.isBlank() ? resourceType : "image",
                    "invalidate", true
            ));
            String status = asString(result.get("result"));
            return "ok".equalsIgnoreCase(status) || "not found".equalsIgnoreCase(status);
        } catch (Exception e) {
            log.error("Failed to delete file from Cloudinary: publicId={}, resourceType={}", publicId, resourceType, e);
            return false;
        }
    }

    @Override
    public boolean deleteFile(String fileUrl) {
        ParsedLocation parsed = parseUrl(fileUrl);
        if (parsed == null) {
            return false;
        }
        return deleteByPublicId(parsed.publicId, parsed.resourceType);
    }

    private ParsedLocation parseUrl(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            return null;
        }
        try {
            URI uri = URI.create(fileUrl);
            String path = uri.getPath(); // /<cloud>/<rt>/upload/v123/folder/sub/name.ext
            Matcher m = CLOUDINARY_PATH.matcher(path);
            if (!m.find()) {
                return null;
            }
            String resourceType = m.group(1);
            String remainder = m.group(2); // folder/sub/name.ext
            String publicId = "raw".equals(resourceType)
                    ? remainder
                    : stripExtension(remainder);
            return new ParsedLocation(publicId, resourceType);
        } catch (Exception e) {
            log.debug("Cannot parse Cloudinary URL: {}", fileUrl, e);
            return null;
        }
    }

    private static String stripExtension(String value) {
        int slash = value.lastIndexOf('/');
        int dot = value.lastIndexOf('.');
        if (dot > slash) {
            return value.substring(0, dot);
        }
        return value;
    }

    private static String asString(Object value) {
        return value == null ? null : value.toString();
    }

    private record ParsedLocation(String publicId, String resourceType) {}
}
