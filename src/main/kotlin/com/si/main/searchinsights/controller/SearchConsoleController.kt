package com.si.main.searchinsights.controller

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.searchconsole.v1.SearchConsole
import com.google.api.services.searchconsole.v1.model.ApiDataRow
import com.google.api.services.searchconsole.v1.model.SearchAnalyticsQueryRequest
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials
import com.si.main.searchinsights.service.MailService
import com.si.main.searchinsights.service.SearchConsoleService
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.io.FileNotFoundException
import java.io.InputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@RestController
class SearchConsoleController (
    private val mailService: MailService,
    private val searchConsoleService: SearchConsoleService,

) {
    @GetMapping("/test")
    fun sendDailySearchAnalytics() {
        searchConsoleService.fetchSearchAnalyticsData()
    }

    @GetMapping("/mail-sending-test")
    fun sendDailyMailing() {
        mailService.sendMail()
    }

}

