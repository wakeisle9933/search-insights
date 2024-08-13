package com.si.main.searchinsights.service

import com.google.analytics.data.v1beta.*
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.gax.core.FixedCredentialsProvider
import com.google.api.services.searchconsole.v1.SearchConsole
import com.google.api.services.searchconsole.v1.model.ApiDataRow
import com.google.api.services.searchconsole.v1.model.SearchAnalyticsQueryRequest
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials
import com.si.main.searchinsights.data.PageViewInfo
import com.si.main.searchinsights.enum.ReportFrequency
import com.si.main.searchinsights.extension.logger
import com.si.main.searchinsights.util.DateUtils
import org.apache.commons.io.output.ByteArrayOutputStream
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.FileNotFoundException
import java.io.InputStream
import java.time.LocalDate

@Service
class SearchConsoleService (
    private val spreadSheetService: SpreadSheetService,
    @Value("\${domain}")
    private val domain: String,
    @Value("\${analytics.prop.id}")
    private val propId: String
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

    fun getAnalyticsSerivce(): BetaAnalyticsDataClient{
        val credentialStream: InputStream =
            this::class.java.classLoader.getResourceAsStream("credential/search-insights.json")
                ?: throw FileNotFoundException("Resource not found: credential/search-insights.json")

        val credential = GoogleCredentials.fromStream(credentialStream)
            .createScoped(listOf("https://www.googleapis.com/auth/analytics.readonly"))

        return BetaAnalyticsDataClient.create(
            BetaAnalyticsDataSettings.newBuilder()
            .setCredentialsProvider(FixedCredentialsProvider.create(credential))
            .build())
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

    // Entire Dimension list
    // https://developers.google.com/analytics/devguides/reporting/data/v1/api-schema?hl=ko
    fun fetchAnalyticsData(
        startDateParam: String? = LocalDate.now().minusDays(3).toString(),
        endDateParam: String? = startDateParam,
        limit: Int? = null
    ): List<PageViewInfo> {
        val startDate = startDateParam ?: LocalDate.now().minusDays(3).toString()
        val endDate = endDateParam ?: startDate
        val client = getAnalyticsSerivce()
        val request = RunReportRequest.newBuilder().apply {
            property = "properties/$propId"
            addDateRanges(DateRange.newBuilder().apply {
                this.startDate = DateUtils.convertToLocalDateString(startDate)
                this.endDate = DateUtils.convertToLocalDateString(endDate)
            })
            addDimensions(Dimension.newBuilder().setName("pagePath"))
            addDimensions(Dimension.newBuilder().setName("pageTitle"))
            addMetrics(Metric.newBuilder().setName("screenPageViews"))
            limit?.let { this.limit = it.toLong() }
            addOrderBys(OrderBy.newBuilder().apply {
                metric = OrderBy.MetricOrderBy.newBuilder().setMetricName("screenPageViews").build()
                desc = true
            })
        }.build()

        return try {
            val response = client.runReport(request)
            response.rowsList.map { row ->
                PageViewInfo(
                    pagePath = row.getDimensionValues(0).value,
                    pageTitle = row.getDimensionValues(1).value,
                    pageViews = row.getMetricValues(0).value.toInt()
                )
            }
        } catch (e: Exception) {
            throw RuntimeException("Analytics Data fetch error : ${e.message}")
        }
    }

    fun createExcelFile(allRows: List<ApiDataRow>, analyticsAllRows: List<PageViewInfo>, reportFrequency: ReportFrequency): ByteArrayOutputStream {
        val workbook = XSSFWorkbook()
        spreadSheetService.createRawDataSheet(workbook, allRows)
        spreadSheetService.createPrefixSummarySheet(workbook, allRows)
        if(reportFrequency == ReportFrequency.DAILY) {
            spreadSheetService.createBacklinkToolSheet(workbook)
            spreadSheetService.createBacklinkSummarySheet(workbook)
        }

        val outputStream = ByteArrayOutputStream()
        workbook.write(outputStream)
        workbook.close()

        return outputStream
    }

}