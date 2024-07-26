package com.si.main.searchinsights.service

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.api.services.searchconsole.v1.model.ApiDataRow
import com.si.main.searchinsights.data.Backlink
import com.si.main.searchinsights.data.PrefixSummary
import okhttp3.OkHttpClient
import okhttp3.Request
import org.apache.poi.common.usermodel.HyperlinkType
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFCellStyle
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import kotlin.math.floor

@Service
class SpreadSheetService(
    @Value("\${base.domain}")
    private val baseDomain: String,
    @Value("\${rapidapi.key}")
    private val rapidApiKey: String
) {

    fun createRawDataSheet(workbook: XSSFWorkbook, allRows: List<ApiDataRow>): Sheet {
        val sheet = workbook.createSheet("Search Analytics Raw Data")
        val creationHelper = workbook.creationHelper

        // Summary Data
        val avgPosition = allRows.map { it.position }.average()
        val totalClicks = allRows.sumOf { it.clicks }
        val totalImpressions = allRows.sumOf { it.impressions }
        val avgCTR = allRows.map { it.ctr }.average()

        // Summary Header
        val headerStyle = createHeaderStyle(workbook)
        val summaryHeaderRow = sheet.createRow(0)
        val summaryHeaders = listOf("Raw Data", "Position", "Clicks", "Impressions", "CTR")
        summaryHeaders.forEachIndexed { index, header ->
            val cell = summaryHeaderRow.createCell(index)
            cell.setCellValue(header)
            cell.cellStyle = headerStyle
        }

        val summaryDataRow = sheet.createRow(1)
        summaryDataRow.createCell(1).setCellValue(avgPosition)
        summaryDataRow.createCell(2).setCellValue(totalClicks)
        summaryDataRow.createCell(3).setCellValue(totalImpressions)
        summaryDataRow.createCell(4).setCellValue(avgCTR)

        // Header
        val headerRow = sheet.createRow(3)
        val headers = listOf("Query", "Position", "Clicks", "Impressions", "CTR", "Page Link")
        headers.forEachIndexed { index, header ->
            val cell = headerRow.createCell(index)
            cell.setCellValue(header)
            cell.cellStyle = headerStyle
        }

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

        return sheet
    }

    fun createPrefixSummarySheet(workbook: XSSFWorkbook, allRows: List<ApiDataRow>): List<Sheet> {
        return listOf(1, 2, 3).map { wordCount ->
            val sheetName = "Prefix Summary (${wordCount} word${if (wordCount > 1) "s" else ""})"
            val sheet = workbook.createSheet(sheetName)
            val groupedData = groupByPrefix(allRows, wordCount)
            val summaryData = calculatePrefixSummary(groupedData)

            // header setting, style
            val headerStyle = createHeaderStyle(workbook)
            val headerRow = sheet.createRow(0)
            val headers =
                listOf("Prefix", "Avg Position", "Total Clicks", "Total Impressions", "Avg CTR")
            headers.forEachIndexed { index, header ->
                val cell = headerRow.createCell(index)
                cell.setCellValue(header)
                cell.cellStyle = headerStyle
            }

            // input data
            summaryData.forEachIndexed { index, summary ->
                val row = sheet.createRow(index + 1)
                row.createCell(0).setCellValue(summary.prefix)
                row.createCell(1).setCellValue(floor(summary.avgPosition * 100) / 100)
                row.createCell(2).setCellValue(summary.totalClicks)
                row.createCell(3).setCellValue(summary.totalImpressions)
                row.createCell(4).setCellValue(floor(summary.avgCTR * 100) / 100)
            }

            // automatically adjust column widths
            for (i in 0..4) {
                sheet.autoSizeColumn(i)
            }

            sheet
        }
    }

    fun createBacklinkSummarySheet(workbook: XSSFWorkbook) {
        val sheet = workbook.createSheet("Backlink Summary")

        // Summary Header
        val headerStyle = createHeaderStyle(workbook)
        val summaryHeaderRow = sheet.createRow(0)
        val summaryHeaders = listOf("URL To", "Title", "URL From")
        summaryHeaders.forEachIndexed { index, header ->
            val cell = summaryHeaderRow.createCell(index)
            cell.setCellValue(header)
            cell.cellStyle = headerStyle
        }

        val backlinks = try {
            getBacklinksFromAhrefs()
        } catch (e: Exception) {
            println("Error fetching backlinks: ${e.message}")
            emptyList()
        }

        // Input data
        backlinks.forEachIndexed { index, backlink ->
            val row = sheet.createRow(index + 1)
            row.createCell(0).setCellValue(backlink.urlTo)
            row.createCell(1).setCellValue(backlink.title)
            row.createCell(2).setCellValue(backlink.urlFrom)
        }

        // Automatically adjust column widths
        for (i in 0..2) {
            sheet.autoSizeColumn(i)
        }
    }

    private fun getBacklinksFromAhrefs(): List<Backlink> {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("https://ahrefs1.p.rapidapi.com/v1/backlink-checker?url=$baseDomain&mode=subdomains")
            .get()
            .addHeader("x-rapidapi-key", rapidApiKey)
            .addHeader("x-rapidapi-host", "ahrefs1.p.rapidapi.com")
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: return emptyList()

        val mapper = jacksonObjectMapper()
        val jsonData: Map<String, Any> = mapper.readValue(responseBody, object : TypeReference<Map<String, Any>>() {})

        val topBacklinks = jsonData["topBacklinks"] as? Map<String, Any>
        val backlinks = topBacklinks?.get("backlinks") as? List<Map<String, Any>>

        return backlinks?.map { backlink ->
            Backlink(
                title = backlink["title"] as? String ?: "",
                urlFrom = backlink["urlFrom"] as? String ?: "",
                urlTo = backlink["urlTo"] as? String ?: ""
            )
        } ?: emptyList()
    }

    fun calculatePrefixSummary(groupedData: Map<String, List<ApiDataRow>>): List<PrefixSummary> {
        return groupedData.map { (prefix, rows) ->
            PrefixSummary(
                prefix = prefix,
                avgPosition = rows.map { it.position }.average(),
                totalClicks = rows.sumOf { it.clicks },
                totalImpressions = rows.sumOf { it.impressions },
                avgCTR = rows.map { it.ctr }.average()
            )
        }.sortedByDescending { it.totalClicks }
    }

    fun getPrefix(query: String, wordCount: Int): String {
        return query.split(" ").take(wordCount).joinToString(" ")
    }

    fun groupByPrefix(allRows: List<ApiDataRow>, wordCount: Int): Map<String, List<ApiDataRow>> {
        return allRows.groupBy { row ->
            val words = row.getKeys()[0].split(" ")
            when {
                wordCount == 1 -> words.firstOrNull() ?: ""
                words.size >= wordCount -> words.take(wordCount).joinToString(" ")
                else -> ""
            }
        }.filterKeys { it.isNotEmpty() }
    }

    fun createHeaderStyle(workbook: XSSFWorkbook): XSSFCellStyle {
        val style = workbook.createCellStyle()
        val font = workbook.createFont()

        // Set Bold
        font.bold = true
        style.setFont(font)

        // Set Background (연한 회색)
        style.fillForegroundColor = IndexedColors.GREY_25_PERCENT.index
        style.fillPattern = FillPatternType.SOLID_FOREGROUND

        // Set Border
        style.borderBottom = BorderStyle.THIN
        style.bottomBorderColor = IndexedColors.BLACK.index

        return style
    }

}