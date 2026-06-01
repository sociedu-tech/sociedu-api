package com.unishare.api.config.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Arrays;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.setCacheNames(Arrays.asList(CacheNames.all()));
        manager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(10_000)
                // Safety net — primary invalidation is explicit on writes.
                .expireAfterWrite(Duration.ofHours(24))
                .recordStats());
        return manager;
    }
}
