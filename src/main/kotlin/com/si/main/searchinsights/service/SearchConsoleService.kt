package com.si.main.searchinsights.service

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.searchconsole.v1.SearchConsole
import com.google.api.services.searchconsole.v1.model.ApiDataRow
import com.google.api.services.searchconsole.v1.model.SearchAnalyticsQueryRequest
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials
import org.apache.poi.common.usermodel.HyperlinkType
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.InputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import org.apache.poi.ss.usermodel.CreationHelper
import org.apache.poi.ss.usermodel.Font
import org.apache.poi.ss.usermodel.Hyperlink
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.xssf.usermodel.XSSFHyperlink

@Service
class SearchConsoleService (
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


    fun fetchSearchAnalyticsData() {
        val service = getSearchConsoleService()
        val threeDaysAgo = LocalDate.now().minusDays(3)
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val formattedDate = threeDaysAgo.format(formatter)

        var startRow = 0
        val rowLimit = 1000 // API Max Limit
        val allRows = mutableListOf<ApiDataRow>()
        do {
            val request = SearchAnalyticsQueryRequest()
                .setStartDate(formattedDate)
                .setEndDate(formattedDate)
                .setDimensions(listOf("query", "page"))
                .setRowLimit(rowLimit)
                .setStartRow(startRow)
            val execute = service.searchanalytics().query(domain, request).execute()

            execute.rows?.let { allRows.addAll(it) } // null safe
            startRow += rowLimit
        } while (execute.rows?.size == rowLimit)

        println("entire dataset: ${allRows.size}")

        createExcelFile(allRows, "search_analytics_data.xlsx")

    }

    fun createExcelFile(allRows: List<ApiDataRow>, fileName: String) {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("Search Analytics Raw Data")
        val creationHelper = workbook.creationHelper

        // Header
        val headerRow = sheet.createRow(0)
        headerRow.createCell(0).setCellValue("Query")
        headerRow.createCell(1).setCellValue("Position")
        headerRow.createCell(2).setCellValue("Clicks")
        headerRow.createCell(3).setCellValue("Impressions")
        headerRow.createCell(4).setCellValue("CTR")
        headerRow.createCell(5).setCellValue("Page Link")

        // Header width
        sheet.setColumnWidth(0, 5 * 1440)
        sheet.setColumnWidth(5, 5 * 1440)

        // Data
        allRows.forEachIndexed { index, row ->
            val dataRow = sheet.createRow(index + 1)
            dataRow.createCell(0).setCellValue(row.getKeys()[0])
            dataRow.createCell(1).setCellValue(row.position)
            dataRow.createCell(2).setCellValue(row.clicks)
            dataRow.createCell(3).setCellValue(row.impressions)
            dataRow.createCell(4).setCellValue(row.ctr)
            // Making Hyperlink
            val linkCell = dataRow.createCell(5)
            linkCell.setCellValue(row.getKeys()[1])

            val hyperlink = creationHelper.createHyperlink(HyperlinkType.URL)
            hyperlink.address = row.getKeys()[1]
            linkCell.hyperlink = hyperlink

            // Link Style
            val linkStyle = workbook.createCellStyle()
            val linkFont = workbook.createFont()
            linkFont.underline = Font.U_SINGLE
            linkFont.color = IndexedColors.BLUE.index
            linkStyle.setFont(linkFont)
            linkCell.cellStyle = linkStyle
        }

        FileOutputStream(fileName).use { outputStream ->
            workbook.write(outputStream)
        }
        workbook.close()

    }

}