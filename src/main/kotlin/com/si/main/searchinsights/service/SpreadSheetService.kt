package com.si.main.searchinsights.service

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.api.services.searchconsole.v1.model.ApiDataRow
import com.si.main.searchinsights.data.Backlink
import com.si.main.searchinsights.data.PageViewInfo
import com.si.main.searchinsights.data.PrefixAnalyticsSummary
import com.si.main.searchinsights.data.PrefixSummary
import mu.KotlinLogging
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
    @Value("\${domain}")
    private val fullDomain: String,
    @Value("\${base.domain}")
    private val baseDomain: String,
    @Value("\${rapidapi.key}")
    private val rapidApiKey: String

) {

    companion object {
        private val logger = KotlinLogging.logger {}
    }

    fun createRawDataSheet(workbook: XSSFWorkbook, allRows: List<ApiDataRow>): Sheet {
        val sheet = workbook.createSheet("Search Console Raw Data")
        val creationHelper = workbook.creationHelper
        val linkStyle = createLinkStyle(workbook)

        // Remove duplicate queries - keep only the one with highest impressions
        val uniqueRows = allRows.groupBy { it.getKeys()[0] }  // group by query
            .map { (_, rows) -> 
                rows.maxByOrNull { it.impressions } ?: rows.first()  // get row with max impressions
            }

        // Summary Data - use deduplicated data
        val avgPosition = if (uniqueRows.isEmpty()) 0.0 else uniqueRows.map { it.position }.average()
        val totalClicks = uniqueRows.sumOf { it.clicks }
        val totalImpressions = uniqueRows.sumOf { it.impressions }
        val avgCTR = if (uniqueRows.isEmpty()) 0.0 else uniqueRows.map { it.ctr }.average()

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
        summaryDataRow.createCell(1).setCellValue(if (avgPosition.isNaN()) 0.0 else avgPosition)
        summaryDataRow.createCell(2).setCellValue(totalClicks)
        summaryDataRow.createCell(3).setCellValue(totalImpressions)
        summaryDataRow.createCell(4).setCellValue(if (avgCTR.isNaN()) 0.0 else avgCTR)

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

        // Data - use deduplicated data
        uniqueRows.forEachIndexed { index, row ->
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
            linkCell.cellStyle = linkStyle
        }

        return sheet
    }

    fun createPrefixSummarySheet(workbook: XSSFWorkbook, allRows: List<ApiDataRow>): List<Sheet> {
        return listOf(1, 2, 3).map { wordCount ->
            val sheetName = "Prefix GSC Summary (${wordCount}w)"
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

    fun createPrefixAnalyticsSummarySheet(workbook: XSSFWorkbook, allRows: List<PageViewInfo>): List<Sheet> {
        return listOf(1, 2, 3).map { wordCount ->
            val sheetName = "Prefix GA Summary (${wordCount}w)"
            val sheet = workbook.createSheet(sheetName)
            val groupedData = groupByAnalyticsPrefix(allRows, wordCount)
            val summaryData = calculateAnalyticsPrefixSummary(groupedData)

            // header setting, style
            val headerStyle = createHeaderStyle(workbook)
            val headerRow = sheet.createRow(0)
            val headers =
                listOf("Prefix", "Total PageViews")
            headers.forEachIndexed { index, header ->
                val cell = headerRow.createCell(index)
                cell.setCellValue(header)
                cell.cellStyle = headerStyle
            }

            // input data
            summaryData.forEachIndexed { index, summary ->
                val row = sheet.createRow(index + 1)
                row.createCell(0).setCellValue(summary.prefix)
                row.createCell(1).setCellValue(summary.totalPageView)
            }

            // automatically adjust column widths
            for (i in 0..1) {
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

    fun createBacklinkToolSheet(workbook: XSSFWorkbook) {
        val sheet = workbook.createSheet("Backlink Tools")
        val creationHelper = workbook.creationHelper
        val linkStyle = createLinkStyle(workbook)

        // Summary Header
        val headerStyle = createHeaderStyle(workbook)
        val summaryHeaderRow = sheet.createRow(0)
        val summaryHeaders = listOf("Tool", "Link")
        summaryHeaders.forEachIndexed { index, header ->
            val cell = summaryHeaderRow.createCell(index)
            cell.setCellValue(header)
            cell.cellStyle = headerStyle
        }

        val backlinkTools = listOf(
            Pair("Google Search Console", "https://search.google.com/search-console/links?resource_id=https%3A%2F%2F$baseDomain%2F&hl=ko"),
            Pair("Semrush", "https://www.semrush.com/analytics/backlinks/overview/?q=$baseDomain&searchType=domain")
        )

        backlinkTools.forEachIndexed { index, (tool, link) ->
            val row = sheet.createRow(index + 1)
            row.createCell(0).setCellValue(tool)

            val linkCell = row.createCell(1)
            linkCell.setCellValue(link)

            val hyperlink = creationHelper.createHyperlink(HyperlinkType.URL)
            hyperlink.address = link
            linkCell.hyperlink = hyperlink

            // Link Style
            linkCell.cellStyle = linkStyle
        }

        // Automatically adjust column widths
        for (i in 0..2) {
            sheet.autoSizeColumn(i)
        }
    }

    fun createRawAnalyticsDataSheet(workbook: XSSFWorkbook, allRows: List<PageViewInfo>): Sheet  {
        val sheet = workbook.createSheet("Google Analytics Raw Data")
        val creationHelper = workbook.creationHelper
        val linkStyle = createLinkStyle(workbook)

        // Summary Header
        val headerStyle = createHeaderStyle(workbook)
        val summaryHeaderRow = sheet.createRow(0)
        val summaryHeaders = listOf("Total PageView")
        summaryHeaders.forEachIndexed { index, header ->
            val cell = summaryHeaderRow.createCell(index)
            cell.setCellValue(header)
            cell.cellStyle = headerStyle
        }

        val totalPageView = allRows.sumOf { it.pageViews }
        val summaryDataRow = sheet.createRow(1)
        summaryDataRow.createCell(0).setCellValue(totalPageView.toString())

        // Header
        val headerRow = sheet.createRow(3)
        val headers = listOf("Views", "Page Title", "Page Path")
        headers.forEachIndexed { index, header ->
            val cell = headerRow.createCell(index)
            cell.setCellValue(header)
            cell.cellStyle = headerStyle
        }

        // Data
        allRows.forEachIndexed { index, row ->
            val dataRow = sheet.createRow(index + 4)
            dataRow.createCell(0).setCellValue(row.pageTitle)
            dataRow.createCell(1).setCellValue(row.pageViews.toString())
            dataRow.createCell(2).setCellValue(row.pagePath)
            // Making Hyperlink
            val linkCell = dataRow.createCell(2)
            val cleanPath = row.pagePath.removePrefix("/")
            val fullUrl = fullDomain + cleanPath
            linkCell.setCellValue(row.pagePath)

            val hyperlink = creationHelper.createHyperlink(HyperlinkType.URL)
            hyperlink.address = fullUrl
            linkCell.hyperlink = hyperlink

            // Link Style
            linkCell.cellStyle = linkStyle
        }

        // Automatically adjust column widths
        for (i in 0..2) {
            sheet.autoSizeColumn(i)
            if (i == 0) { // 첫 번째 열 최대 길이 제한
                val maxWidth = 13 * 1440
                val currentWidth = sheet.getColumnWidth(i)

                if (currentWidth > maxWidth) {
                    sheet.setColumnWidth(i, maxWidth)
                }
            }
        }

        return sheet

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

    fun createHighImpressionsLowPositionSheet(workbook: XSSFWorkbook, allRows: List<ApiDataRow>): Sheet {
        val sheet = workbook.createSheet("Potential Hits")
        val creationHelper = workbook.creationHelper
        val linkStyle = createLinkStyle(workbook)

        // Header Style
        val headerStyle = createHeaderStyle(workbook)

        // Header
        val headerRow = sheet.createRow(0)
        val headers = listOf("Query", "Position", "Clicks", "Impressions", "CTR", "Page Link")
        headers.forEachIndexed { index, header ->
            val cell = headerRow.createCell(index)
            cell.setCellValue(header)
            cell.cellStyle = headerStyle
        }

        // Data Filtering, Sorting and Remove duplicates
        val filteredRows = allRows.filter { it.position > 3.0 }
            .groupBy { it.getKeys()[0] }  // group by query
            .map { (_, rows) -> 
                rows.maxByOrNull { it.impressions } ?: rows.first()  // get row with max impressions
            }
            .sortedByDescending { it.impressions }

        filteredRows.forEachIndexed { index, row ->
            val dataRow = sheet.createRow(index + 1)
            dataRow.createCell(0).setCellValue(row.getKeys()[0])
            dataRow.createCell(1).setCellValue(floor(row.position * 100) / 100)
            dataRow.createCell(2).setCellValue(row.clicks)
            dataRow.createCell(3).setCellValue(row.impressions)
            dataRow.createCell(4).setCellValue(floor(row.ctr * 100) / 100)

            // Hyperlink
            val linkCell = dataRow.createCell(5)
            linkCell.setCellValue(row.getKeys()[1])

            val hyperlink = creationHelper.createHyperlink(HyperlinkType.URL)
            hyperlink.address = row.getKeys()[1]
            linkCell.hyperlink = hyperlink

            // Hyperlink Style
            linkCell.cellStyle = linkStyle
        }

        // Automatically adjust column widths
        for (i in 0..5) {
            sheet.autoSizeColumn(i)
            if (i == 0) {  // Query 열인 경우
                val maxWidth = 5 * 1440  // 최대 너비 설정
                val currentWidth = sheet.getColumnWidth(i)
                // 현재 너비가 최대 너비보다 크면 최대 너비로 제한
                if (currentWidth > maxWidth) {
                    sheet.setColumnWidth(i, maxWidth)
                }
                // 현재 너비가 최대 너비보다 작으면 현재 너비 유지 (자동 조정된 값)
            }
        }

        return sheet
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

    fun calculateAnalyticsPrefixSummary(groupedData: Map<String, List<PageViewInfo>>): List<PrefixAnalyticsSummary> {
        return groupedData.map { (prefix, rows) ->
            PrefixAnalyticsSummary(
                prefix = prefix,
                totalPageView = rows.sumOf { it.pageViews }
            )
        }.sortedByDescending { it.totalPageView }
    }

    fun getPrefix(query: String, wordCount: Int): String {
        return query.split(" ").take(wordCount).joinToString(" ")
    }

    fun <T> groupByPrefix(allRows: List<T>, wordCount: Int, keyExtractor: (T) -> String): Map<String, List<T>> {
        return allRows.groupBy { row ->
            val words = keyExtractor(row).split(" ")
            when {
                wordCount == 1 -> words.firstOrNull() ?: ""
                words.size >= wordCount -> words.take(wordCount).joinToString(" ")
                else -> ""
            }
        }.filterKeys { it.isNotEmpty() }
    }

    fun groupByPrefix(allRows: List<ApiDataRow>, wordCount: Int): Map<String, List<ApiDataRow>> {
        return groupByPrefixGeneric(allRows, wordCount) { it.getKeys()[0] }
    }

    fun groupByAnalyticsPrefix(allRows: List<PageViewInfo>, wordCount: Int): Map<String, List<PageViewInfo>> {
        return groupByPrefixGeneric(allRows, wordCount) { it.pageTitle }
    }

    private fun <T> groupByPrefixGeneric(allRows: List<T>, wordCount: Int, keyExtractor: (T) -> String): Map<String, List<T>> {
        return allRows.groupBy { row ->
            val words = keyExtractor(row).lowercase().split(" ")
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

    private fun createLinkStyle(workbook: XSSFWorkbook): XSSFCellStyle {
        val linkStyle = workbook.createCellStyle()
        val linkFont = workbook.createFont()
        linkFont.underline = Font.U_SINGLE
        linkFont.color = IndexedColors.BLUE.index
        linkStyle.setFont(linkFont)
        return linkStyle
    }

}