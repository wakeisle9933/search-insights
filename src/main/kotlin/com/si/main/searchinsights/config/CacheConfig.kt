package com.si.main.searchinsights.config

import com.si.main.searchinsights.extension.logger
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.CachingConfigurer
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.concurrent.ConcurrentMapCache
import org.springframework.cache.interceptor.CacheErrorHandler
import org.springframework.cache.interceptor.CacheResolver
import org.springframework.cache.interceptor.KeyGenerator
import org.springframework.cache.interceptor.SimpleCacheErrorHandler
import org.springframework.cache.interceptor.SimpleCacheResolver
import org.springframework.cache.interceptor.SimpleKeyGenerator
import org.springframework.cache.support.SimpleCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import java.util.concurrent.ConcurrentHashMap

@Configuration
@EnableCaching
@EnableScheduling
class CacheConfig : CachingConfigurer {
    
    private val logger = logger()
    
    @Bean
    override fun cacheManager(): CacheManager {
        val cacheManager = SimpleCacheManager()
        
        // 캐시 이름과 TTL 설정
        val caches = listOf(
            // 실시간 데이터: 10초 캐싱
            ExpiringConcurrentMapCache("realtimeAnalytics", 10_000),
            
            // 30분 데이터: 30초 캐싱
            ExpiringConcurrentMapCache("last30minAnalytics", 30_000),
            
            // 날짜별 데이터: 60초 캐싱
            ExpiringConcurrentMapCache("customDateAnalytics", 60_000),
            
            // WordPress 카테고리 데이터: 5분 캐싱
            ExpiringConcurrentMapCache("wordpressCategories", 300_000)
        )
        
        cacheManager.setCaches(caches)
        return cacheManager
    }
    
    override fun cacheResolver(): CacheResolver = SimpleCacheResolver(cacheManager())
    
    override fun keyGenerator(): KeyGenerator = SimpleKeyGenerator()
    
    override fun errorHandler(): CacheErrorHandler = SimpleCacheErrorHandler()
    
    // 만료된 캐시 엔트리 정리 (1분마다)
    @Scheduled(fixedDelay = 60_000)
    fun evictExpiredCacheEntries() {
        val cacheManager = cacheManager() as SimpleCacheManager
        cacheManager.cacheNames.forEach { cacheName ->
            val cache = cacheManager.getCache(cacheName)
            if (cache is ExpiringConcurrentMapCache) {
                cache.evictExpired()
            }
        }
    }
}

// TTL을 지원하는 커스텀 캐시 구현
class ExpiringConcurrentMapCache(
    name: String,
    private val ttlMillis: Long
) : ConcurrentMapCache(name, ConcurrentHashMap(), true) {
    
    private val expirationTimes = ConcurrentHashMap<Any, Long>()
    private val logger = logger()
    
    override fun get(key: Any): Cache.ValueWrapper? {
        val expirationTime = expirationTimes[key]
        if (expirationTime != null && System.currentTimeMillis() > expirationTime) {
            // 만료된 엔트리 제거
            evict(key)
            logger.debug("캐시 만료됨 - cache: $name, key: $key")
            return null
        }
        return super.get(key)
    }
    
    override fun put(key: Any, value: Any?) {
        super.put(key, value)
        expirationTimes[key] = System.currentTimeMillis() + ttlMillis
        logger.debug("캐시 저장됨 - cache: $name, key: $key, ttl: ${ttlMillis}ms")
    }
    
    override fun evict(key: Any) {
        super.evict(key)
        expirationTimes.remove(key)
    }
    
    override fun clear() {
        super.clear()
        expirationTimes.clear()
    }
    
    fun evictExpired() {
        val currentTime = System.currentTimeMillis()
        val expiredKeys = expirationTimes.filter { (_, expirationTime) ->
            currentTime > expirationTime
        }.keys
        
        expiredKeys.forEach { key ->
            evict(key)
            logger.debug("만료된 캐시 제거 - cache: $name, key: $key")
        }
    }
}