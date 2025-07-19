package com.si.main.searchinsights.config

import org.springframework.context.annotation.Configuration
import org.springframework.http.CacheControl
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.util.concurrent.TimeUnit
import org.springframework.scheduling.annotation.EnableAsync

@Configuration
@EnableAsync  // 🚀 비동기 처리 활성화!
class WebConfig : WebMvcConfigurer {
    
    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        // 정적 리소스에 대한 캐시 설정
        registry.addResourceHandler("/js/**")
            .addResourceLocations("classpath:/static/js/")
            .setCacheControl(
                CacheControl.maxAge(365, TimeUnit.DAYS)
                    .cachePublic()
                    .mustRevalidate()
            )
            
        registry.addResourceHandler("/css/**")
            .addResourceLocations("classpath:/static/css/")
            .setCacheControl(
                CacheControl.maxAge(365, TimeUnit.DAYS)
                    .cachePublic()
                    .mustRevalidate()
            )
            
        // 이미지와 폰트 리소스 캐싱
        registry.addResourceHandler("/images/**", "/fonts/**")
            .addResourceLocations("classpath:/static/images/", "classpath:/static/fonts/")
            .setCacheControl(
                CacheControl.maxAge(365, TimeUnit.DAYS)
                    .cachePublic()
                    .immutable() // 이미지와 폰트는 변경되지 않음
            )
            
        // JSON 데이터 파일은 짧은 캐시 시간 설정
        registry.addResourceHandler("/*.json", "/static/*.json")
            .addResourceLocations("classpath:/static/")
            .setCacheControl(
                CacheControl.maxAge(5, TimeUnit.MINUTES)
                    .cachePublic()
            )
    }
}