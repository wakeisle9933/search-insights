package com.si.main.searchinsights.config

import com.google.analytics.data.v1beta.BetaAnalyticsDataClient
import com.google.analytics.data.v1beta.BetaAnalyticsDataSettings
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.gax.core.FixedCredentialsProvider
import com.google.api.services.searchconsole.v1.SearchConsole
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials
import com.si.main.searchinsights.enum.ErrorCode
import com.si.main.searchinsights.exception.BusinessException
import com.si.main.searchinsights.extension.logger
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.InputStream
import javax.annotation.PreDestroy

@Configuration
class GoogleApiConfig {
    
    private val logger = logger()
    private var analyticsClient: BetaAnalyticsDataClient? = null
    
    @Bean
    fun searchConsoleClient(): SearchConsole {
        logger.info("🚀 Initializing SearchConsole client singleton...")
        
        val httpTransport = GoogleNetHttpTransport.newTrustedTransport()
        val jsonFactory = GsonFactory.getDefaultInstance()
        
        val credentialStream: InputStream = 
            this::class.java.classLoader.getResourceAsStream("credential/search-insights.json")
                ?: throw BusinessException(
                    errorCode = ErrorCode.CREDENTIAL_NOT_FOUND,
                    message = "Google 서비스 계정 인증 파일을 찾을 수 없습니다"
                )
        
        val credential = GoogleCredentials.fromStream(credentialStream)
            .createScoped(listOf("https://www.googleapis.com/auth/webmasters.readonly"))
        
        return SearchConsole.Builder(httpTransport, jsonFactory, HttpCredentialsAdapter(credential))
            .setApplicationName("Search Console API Kotlin")
            .build()
    }
    
    @Bean
    fun analyticsDataClient(): BetaAnalyticsDataClient {
        logger.info("🚀 Initializing Analytics client singleton...")
        
        val credentialStream: InputStream = 
            this::class.java.classLoader.getResourceAsStream("credential/search-insights.json")
                ?: throw BusinessException(
                    errorCode = ErrorCode.CREDENTIAL_NOT_FOUND,
                    message = "Google 서비스 계정 인증 파일을 찾을 수 없습니다"
                )
        
        val credential = GoogleCredentials.fromStream(credentialStream)
            .createScoped(listOf("https://www.googleapis.com/auth/analytics.readonly"))
        
        analyticsClient = BetaAnalyticsDataClient.create(
            BetaAnalyticsDataSettings.newBuilder()
                .setCredentialsProvider(FixedCredentialsProvider.create(credential))
                .build()
        )
        
        return analyticsClient!!
    }
    
    @PreDestroy
    fun cleanup() {
        // Analytics 클라이언트는 명시적으로 종료해야 함
        analyticsClient?.let {
            try {
                logger.info("📤 Closing Analytics client...")
                it.close()
            } catch (e: Exception) {
                logger.error("Error closing Analytics client", e)
            }
        }
    }
}