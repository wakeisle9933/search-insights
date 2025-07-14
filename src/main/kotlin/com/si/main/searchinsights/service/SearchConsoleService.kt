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
            logger.info("GSC rows: ${execute.rows?.size} date=$startDate")
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

        return getAnalyticsSerivce().use { client ->
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

            try {
                client.runReport(request).rowsList.map { row ->
                    PageViewInfo(
                        pagePath = row.getDimensionValues(0).value,
                        pageTitle = row.getDimensionValues(1).value,
                        pageViews = row.getMetricValues(0).value.toDouble()
                    )
                }
            } catch (e: Exception) {
                logger.error("Analytics Data fetch error", e)
                emptyList()  // 에러 발생 시 빈 리스트 반환
            }
        }
    }

    fun createExcelFile(allRows: List<ApiDataRow>, analyticsAllRows: List<PageViewInfo>, reportFrequency: ReportFrequency): ByteArrayOutputStream {
        val workbook = XSSFWorkbook()
        spreadSheetService.createRawDataSheet(workbook, allRows)
        spreadSheetService.createHighImpressionsLowPositionSheet(workbook, allRows)
        spreadSheetService.createPrefixSummarySheet(workbook, allRows)
        spreadSheetService.createRawAnalyticsDataSheet(workbook, analyticsAllRows)
        spreadSheetService.createPrefixAnalyticsSummarySheet(workbook, analyticsAllRows)

        if(reportFrequency == ReportFrequency.WEEKLY ||
            reportFrequency == ReportFrequency.MONTHLY) {
            spreadSheetService.createBacklinkSummarySheet(workbook)
        }

        if(reportFrequency == ReportFrequency.DAILY) {
            spreadSheetService.createBacklinkToolSheet(workbook)
        }

        val outputStream = ByteArrayOutputStream()
        workbook.write(outputStream)
        workbook.close()

        return outputStream
    }

    fun fetchRealTimeAnalyticsData(): List<PageViewInfo> {
        return getAnalyticsSerivce().use { client ->
            val request = RunReportRequest.newBuilder().apply {
                property = "properties/$propId"

                addDateRanges(DateRange.newBuilder().apply {
                    this.startDate = "today"  // 오늘 데이터!
                    this.endDate = "today"    // 오늘까지!
                })

                // 기존 코드 그대로 유지
                addMetrics(Metric.newBuilder().setName("activeUsers"))
                addDimensions(Dimension.newBuilder().setName("pageTitle"))
                addDimensions(Dimension.newBuilder().setName("pagePath"))
                addOrderBys(OrderBy.newBuilder().apply {
                    metric = OrderBy.MetricOrderBy.newBuilder().setMetricName("activeUsers").build()
                    desc = true
                })
            }.build()

            // 결과 처리 (기존 코드와 동일)
            try {
                client.runReport(request).rowsList.map { row ->
                    PageViewInfo(
                        pageTitle = row.getDimensionValues(0).value,  // pageTitle
                        pagePath = row.getDimensionValues(1).value,   // pagePath
                        pageViews = row.getMetricValues(0).value.toDouble()
                    )
                }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    fun fetchRealTimeAnalyticsWithActiveUsers(): Map<String, Any> {
        val activeUsers = getAnalyticsSerivce().use { client ->
            val request = RunReportRequest.newBuilder().apply {
                property = "properties/$propId"
                addDateRanges(DateRange.newBuilder().apply {
                    this.startDate = "today"
                    this.endDate = "today"
                })
                addMetrics(Metric.newBuilder().setName("activeUsers"))
            }.build()

            try {
                client.runReport(request).rowsList.firstOrNull()?.getMetricValues(0)?.value?.toInt() ?: 0
            } catch (e: Exception) {
                logger.error("Analytics active users fetch error", e)
                0
            }
        }

        return mapOf(
            "activeUsers" to activeUsers,
            "pageViews" to fetchRealTimeAnalyticsData()
        )
    }

    fun fetchLast30MinAnalyticsWithActiveUsers(): Map<String, Any> {
        return getAnalyticsSerivce().use { client ->
            // 최근 29분 활성 사용자 가져오기 (29분이 최대)
            val activeUsersRequest = RunRealtimeReportRequest.newBuilder()
                .setProperty("properties/$propId")
                .addMetrics(Metric.newBuilder().setName("activeUsers"))
                .addMinuteRanges(
                    MinuteRange.newBuilder()
                        .setStartMinutesAgo(29)
                        .setEndMinutesAgo(0)
                )
                .build()

            val activeUsers = try {
                client.runRealtimeReport(activeUsersRequest).rowsList
                    .firstOrNull()?.getMetricValues(0)?.value?.toInt() ?: 0
            } catch (e: Exception) {
                logger.error("최근 30분 활성 사용자 데이터 가져오기 실패", e)
                0
            }

            // 최근 29분 페이지뷰 가져오기
            val pageViewsRequest = RunRealtimeReportRequest.newBuilder()
                .setProperty("properties/$propId")
                .addMinuteRanges(
                    MinuteRange.newBuilder()
                        .setStartMinutesAgo(29)
                        .setEndMinutesAgo(0)
                )
                .addDimensions(Dimension.newBuilder().setName("unifiedScreenName"))
                .addMetrics(Metric.newBuilder().setName("screenPageViews"))
                .addOrderBys(
                    OrderBy.newBuilder()
                        .setMetric(OrderBy.MetricOrderBy.newBuilder().setMetricName("screenPageViews"))
                        .setDesc(true)
                )
                .build()

            val pageViews = try {
                client.runRealtimeReport(pageViewsRequest).rowsList.map { row ->
                    PageViewInfo(
                        pageTitle = row.getDimensionValues(0).value, // unifiedScreenName을 pageTitle에 넣기
                        pagePath = "", // 경로는 없지만 객체 구조 유지
                        pageViews = row.getMetricValues(0).value.toDouble()
                    )
                }
            } catch (e: Exception) {
                logger.error("최근 30분 페이지뷰 데이터 가져오기 실패", e)
                emptyList<PageViewInfo>()
            }

            mapOf(
                "activeUsers" to activeUsers,
                "pageViews" to pageViews
            )
        }
    }

    fun fetchCustomDateAnalyticsWithActiveUsers(startDate: String, endDate: String): Map<String, Any> {
        val client = getAnalyticsSerivce()

        try {
            // 활성 사용자 가져오기
            val request = RunReportRequest.newBuilder().apply {
                property = "properties/$propId"
                addDateRanges(DateRange.newBuilder().apply {
                    this.startDate = startDate
                    this.endDate = endDate
                })
                addMetrics(Metric.newBuilder().setName("activeUsers"))
            }.build()

            val activeUsers = client.runReport(request).rowsList
                .firstOrNull()?.getMetricValues(0)?.value?.toInt() ?: 0

            // 페이지뷰 가져오기
            val pageViewsRequest = RunReportRequest.newBuilder().apply {
                property = "properties/$propId"
                addDateRanges(DateRange.newBuilder().apply {
                    this.startDate = startDate
                    this.endDate = endDate
                })
                addDimensions(Dimension.newBuilder().setName("pageTitle"))
                addDimensions(Dimension.newBuilder().setName("pagePath"))
                addMetrics(Metric.newBuilder().setName("screenPageViews"))
                addOrderBys(OrderBy.newBuilder().apply {
                    metric = OrderBy.MetricOrderBy.newBuilder().setMetricName("screenPageViews").build()
                    desc = true
                })
            }.build()

            val pageViews = client.runReport(pageViewsRequest).rowsList.map { row ->
                PageViewInfo(
                    pageTitle = row.getDimensionValues(0).value,
                    pagePath = row.getDimensionValues(1).value,
                    pageViews = row.getMetricValues(0).value.toDouble()
                )
            }

            return mapOf(
                "activeUsers" to activeUsers,
                "pageViews" to pageViews
            )
        } catch (e: Exception) {
            logger.error("Custom date analytics fetch error", e)
            return mapOf(
                "activeUsers" to 0,
                "pageViews" to emptyList<PageViewInfo>()
            )
        } finally {
            client.close()
        }
    }

}