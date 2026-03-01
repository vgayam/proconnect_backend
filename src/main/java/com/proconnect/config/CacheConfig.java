package com.proconnect.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * In-process cache for data that rarely changes (categories, skills).
 * Uses a simple ConcurrentMapCache — no extra dependencies.
 *
 * Cache names:
 *   "categories"      — /api/categories  (full objects)
 *   "categoryNames"   — /api/categories/names
 *   "skills"          — /api/skills
 *   "skillCategories" — /api/skills/categories
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(
            "categories",
            "categoryNames",
            "skills",
            "skillCategories"
        );
    }
}
