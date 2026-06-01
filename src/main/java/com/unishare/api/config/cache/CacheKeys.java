package com.unishare.api.config.cache;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.stream.Collectors;

public final class CacheKeys {

    private CacheKeys() {
    }

    public static String pageable(Pageable pageable) {
        if (pageable == null || pageable.isUnpaged()) {
            return "unpaged";
        }
        Sort sort = pageable.getSort();
        String sortKey = sort.isUnsorted()
                ? "unsorted"
                : sort.stream()
                .map(order -> order.getProperty() + ":" + order.getDirection())
                .collect(Collectors.joining(","));
        return pageable.getPageNumber() + ":" + pageable.getPageSize() + ":" + sortKey;
    }

    public static String keyword(String keyword) {
        return keyword != null ? keyword : "";
    }

    public static String mentorId(UUID mentorId) {
        return mentorId != null ? mentorId.toString() : "all";
    }

    public static String mentorPackages(UUID mentorId, String keyword, Pageable pageable) {
        return mentorId(mentorId) + "|" + keyword(keyword) + "|" + pageable(pageable);
    }

    public static String activePackages(UUID mentorId, String keyword, Pageable pageable) {
        return mentorId(mentorId) + "|" + keyword(keyword) + "|" + pageable(pageable);
    }

    public static String mentorSearch(String verificationStatus, String keyword,
                                      BigDecimal minBasePrice, BigDecimal maxBasePrice, Pageable pageable) {
        return verificationStatus + "|" + keyword(keyword) + "|"
                + minBasePrice + "|" + maxBasePrice + "|" + pageable(pageable);
    }

    public static String packageVersion(UUID packageId, UUID versionId) {
        return packageId + "|" + versionId;
    }

    public static String mentorPackage(UUID mentorId, UUID packageId) {
        return mentorId + "|" + packageId;
    }

    public static String curriculum(UUID packageId, UUID versionId, Pageable pageable) {
        return packageId + "|" + versionId + "|" + pageable(pageable);
    }

    public static String pagedByPackage(UUID packageId, Pageable pageable) {
        return packageId + "|list|" + pageable(pageable);
    }
}
