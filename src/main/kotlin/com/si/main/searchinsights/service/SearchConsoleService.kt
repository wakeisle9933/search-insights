package com.si.main.searchinsights.service

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.searchconsole.v1.SearchConsole
import com.google.api.services.searchconsole.v1.model.ApiDataRow
import com.google.api.services.searchconsole.v1.model.SearchAnalyticsQueryRequest
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials
import com.si.main.searchinsights.extension.logger
import com.si.main.searchinsights.util.DateUtils
import org.apache.commons.io.output.ByteArrayOutputStream
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.FileNotFoundException
import java.io.InputStream

@Service
class SearchConsoleService (
    private val mailService: MailService,
    @Value("\${domain}")
    private val domain: String

) {

    private val logger = logger()

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


    fun fetchSearchAnalyticsData(
        startDate: String = DateUtils.getFormattedDateBeforeDays(3),
        endDate: String = startDate
    ): List<ApiDataRow> {
        val service = getSearchConsoleService()
        var startRow = 0
        val rowLimit = 1000 // API Max Limit
        val allRows = mutableListOf<ApiDataRow>()
        do {
            val request = SearchAnalyticsQueryRequest()
                .setStartDate(startDate)
                .setEndDate(endDate)
                .setDimensions(listOf("query", "page"))
                .setRowLimit(rowLimit)
                .setStartRow(startRow)
            val execute = service.searchanalytics().query(domain, request).execute()

            execute.rows?.let { allRows.addAll(it) } // null safe
            startRow += rowLimit
        } while (execute.rows?.size == rowLimit)

        logger.info("entire dataset: ${allRows.size}")
        return allRows
    }

    fun createExcelFile(allRows: List<ApiDataRow>): ByteArrayOutputStream {
        val workbook = XSSFWorkbook()
        val spreadSheetService = SpreadSheetService()

        spreadSheetService.createRawDataSheet(workbook, allRows)
        spreadSheetService.createPrefixSummarySheet(workbook, allRows)

        val outputStream = ByteArrayOutputStream()
        workbook.write(outputStream)
        workbook.close()

        return outputStream
    }

}