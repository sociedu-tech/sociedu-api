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
        @CacheEvict(cacheNames = CacheNames.SERVICE_PACKAGE, allEntries = true),
        @CacheEvict(cacheNames = CacheNames.MENTOR_SERVICE_PACKAGES, allEntries = true),
        @CacheEvict(cacheNames = CacheNames.ACTIVE_SERVICE_PACKAGES, allEntries = true),
        @CacheEvict(cacheNames = CacheNames.MY_SERVICE_PACKAGES, allEntries = true),
        @CacheEvict(cacheNames = CacheNames.MY_SERVICE_PACKAGE, allEntries = true),
        @CacheEvict(cacheNames = CacheNames.SERVICE_PACKAGE_VERSION, allEntries = true),
        @CacheEvict(cacheNames = CacheNames.ACTIVE_PACKAGE_VERSION, allEntries = true),
        @CacheEvict(cacheNames = CacheNames.ACTIVE_PACKAGE_VERSIONS, allEntries = true),
        @CacheEvict(cacheNames = CacheNames.CURRICULUM, allEntries = true),
        @CacheEvict(cacheNames = CacheNames.ACTIVE_CURRICULUM, allEntries = true),
})
public @interface EvictAllCatalogCaches {
}
