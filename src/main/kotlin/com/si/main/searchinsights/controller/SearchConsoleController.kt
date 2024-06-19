package com.si.main.searchinsights.controller

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.searchconsole.v1.SearchConsole
import com.google.api.services.searchconsole.v1.model.SearchAnalyticsQueryRequest
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials
import com.si.main.searchinsights.service.MailService
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.io.FileNotFoundException
import java.io.InputStream

@RestController
class SearchConsoleController (
    private val mailService: MailService,

    @Value("\${domain}")
    private val domain: String

) {

    fun getSearchConsoleService(): SearchConsole {
        val httpTransport = GoogleNetHttpTransport.newTrustedTransport()
        val jsonFactory = GsonFactory.getDefaultInstance()

        val credentialStream: InputStream =
            this::class.java.classLoader.getResourceAsStream("credential/search-insights.json")
                ?: throw FileNotFoundException("Resource not found: credential/search-insights.json")

        val credential = GoogleCredentials.fromStream(credentialStream)
            .createScoped(listOf("https://www.googleapis.com/auth/webmasters.readonly"))

        return SearchConsole.Builder(httpTransport, jsonFactory, HttpCredentialsAdapter(credential))
            .setApplicationName("Search Console API Kotlin")
            .build()
    }

    @GetMapping("/test")
    fun fetchSearchAnalyticsData() { // SearchAnalyticsQueryResponse
        val service = getSearchConsoleService()
        val request = SearchAnalyticsQueryRequest()
            .setStartDate("2024-05-01")
            .setEndDate("2024-06-17")
            .setDimensions(listOf("query", "page"))
        // .setDimensions(listOf("query", "page", "country", "device", "date"))
        val execute = service.searchanalytics().query(domain, request).execute()
        println("")
    }

    @GetMapping("/mail-sending-test")
    fun sendDailyMailing() {
        mailService.sendMail()
    }
}

