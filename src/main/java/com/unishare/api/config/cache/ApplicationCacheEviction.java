package com.unishare.api.config.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ApplicationCacheEviction {

    private final CacheManager cacheManager;

    public void evictMentorProfile(UUID mentorId) {
        if (mentorId == null) {
            return;
        }
        evict(CacheNames.MENTOR_PROFILE, mentorId);
        evictAll(CacheNames.MENTOR_SEARCH);
    }

    private void evict(String cacheName, UUID key) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.evict(key);
        }
    }

    private void evictAll(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        }
    }
}
