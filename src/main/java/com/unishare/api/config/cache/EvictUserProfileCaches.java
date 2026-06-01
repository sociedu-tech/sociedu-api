package com.unishare.api.config.cache;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Caching(evict = {
        @CacheEvict(cacheNames = CacheNames.USER_PROFILE, key = "#userId"),
        @CacheEvict(cacheNames = CacheNames.USER_FULL_PROFILE, key = "#userId"),
        @CacheEvict(cacheNames = CacheNames.MENTOR_PROFILE, key = "#userId"),
        @CacheEvict(cacheNames = CacheNames.MENTOR_SEARCH, allEntries = true),
})
public @interface EvictUserProfileCaches {
}
