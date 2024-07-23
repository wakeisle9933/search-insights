package com.si.main.searchinsights.service

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.searchconsole.v1.SearchConsole
import com.google.api.services.searchconsole.v1.model.ApiDataRow
import com.google.api.services.searchconsole.v1.model.SearchAnalyticsQueryRequest
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials
import com.si.main.searchinsights.util.DateUtils
import org.apache.commons.io.output.ByteArrayOutputStream
import org.apache.poi.common.usermodel.HyperlinkType
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.Font
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.FileNotFoundException
import java.io.InputStream
import kotlin.math.floor

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

        println("entire dataset: ${allRows.size}")

        return allRows
    }

    fun createExcelFile(allRows: List<ApiDataRow>): ByteArrayOutputStream {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("Search Analytics Raw Data")
        val creationHelper = workbook.creationHelper

        // Summary Data
        val avgPosition = allRows.map { it.position }.average()
        val totalClicks = allRows.sumOf { it.clicks }
        val totalImpressions = allRows.sumOf { it.impressions }
        val avgCTR = allRows.map { it.ctr }.average()

        // Summary Header
        val summaryHeaderRow = sheet.createRow(0)
        val formattedDate = DateUtils.getFormattedDateBeforeDays(3);
        summaryHeaderRow.createCell(0).setCellValue("$formattedDate Summary")
        summaryHeaderRow.createCell(1).setCellValue("Position")
        summaryHeaderRow.createCell(2).setCellValue("Click")
        summaryHeaderRow.createCell(3).setCellValue("Impressions")
        summaryHeaderRow.createCell(4).setCellValue("CTR")

        val summaryDataRow = sheet.createRow(1)
        summaryDataRow.createCell(1).setCellValue(avgPosition)
        summaryDataRow.createCell(2).setCellValue(totalClicks)
        summaryDataRow.createCell(3).setCellValue(totalImpressions)
        summaryDataRow.createCell(4).setCellValue(avgCTR)

        // Empty
        val separatorRow = sheet.createRow(2)
        val redStyle = workbook.createCellStyle()
        redStyle.fillForegroundColor = IndexedColors.RED.index
        redStyle.fillPattern = FillPatternType.SOLID_FOREGROUND

        // Empty Background
        for (i in 0..5) {
            val cell = separatorRow.createCell(i)
            cell.cellStyle = redStyle
        }

        // Header
        val headerRow = sheet.createRow(3)
        headerRow.createCell(0).setCellValue("Query")
        headerRow.createCell(1).setCellValue("Position")
        headerRow.createCell(2).setCellValue("Clicks")
        headerRow.createCell(3).setCellValue("Impressions")
        headerRow.createCell(4).setCellValue("CTR")
        headerRow.createCell(5).setCellValue("Page Link")

        // Header width
        sheet.setColumnWidth(0, 5 * 1440)
        sheet.setColumnWidth(3, 2 * 1440)
        sheet.setColumnWidth(5, 5 * 1440)

        // Data
        allRows.forEachIndexed { index, row ->
            val dataRow = sheet.createRow(index + 4)
            dataRow.createCell(0).setCellValue(row.getKeys()[0])
            dataRow.createCell(1).setCellValue(floor(row.position * 100) / 100)
            dataRow.createCell(2).setCellValue(row.clicks)
            dataRow.createCell(3).setCellValue(row.impressions)
            dataRow.createCell(4).setCellValue(floor(row.ctr * 100) / 100)
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

        val outputStream = ByteArrayOutputStream()
        workbook.write(outputStream)
        workbook.close()

        return outputStream
    }

}