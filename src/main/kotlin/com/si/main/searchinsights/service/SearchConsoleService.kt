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
import com.si.main.searchinsights.enum.ErrorCode
import com.si.main.searchinsights.exception.BusinessException
import com.si.main.searchinsights.exception.DataProcessingException
import com.si.main.searchinsights.exception.ExternalApiException
import kotlinx.coroutines.*
import kotlin.math.ceil
import org.apache.commons.io.output.ByteArrayOutputStream
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.apache.poi.xssf.streaming.SXSSFWorkbook
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.io.FileNotFoundException
import java.io.InputStream
import java.time.LocalDate

@Service
class SearchConsoleService (
    private val spreadSheetService: SpreadSheetService,
    private val searchConsoleClient: SearchConsole,
    private val analyticsDataClient: BetaAnalyticsDataClient,
    @Value("\${domain}")
    private val domain: String,
    @Value("\${analytics.prop.id}")
    private val propId: String
) {

    private val logger = logger()


    @Cacheable(value = ["searchAnalyticsData"], key = "#startDate + '_' + #endDate")
    fun fetchSearchAnalyticsData(
        startDate: String = DateUtils.getFormattedDateBeforeDays(3),
        endDate: String = startDate
    ): List<ApiDataRow> = runBlocking {
        fetchSearchAnalyticsDataParallel(startDate, endDate)
    }

    // 병렬 처리를 위한 새로운 함수! 🚀
    private suspend fun fetchSearchAnalyticsDataParallel(
        startDate: String,
        endDate: String
    ): List<ApiDataRow> = coroutineScope {
        val rowLimit = 25000 // 25배 증가!! 🔥
        
        // 먼저 전체 데이터 크기 확인
        val firstBatch = fetchBatch(startDate, endDate, 0, rowLimit)
        
        // 전체 데이터가 첫 배치에 다 들어왔으면 바로 반환
        if (firstBatch.size < rowLimit) {
            logger.info("전체 데이터 ${firstBatch.size}개 - 한 번에 로드 완료! 🎉")
            return@coroutineScope firstBatch
        }
        
        // 여러 페이지가 필요한 경우 병렬 처리! 💨
        val allRows = mutableListOf<ApiDataRow>()
        allRows.addAll(firstBatch)
        
        var currentBatch = 1
        val deferreds = mutableListOf<Deferred<List<ApiDataRow>>>()
        
        // 최대 4개의 병렬 요청으로 제한 (API 부하 방지)
        while (true) {
            val batchJobs = (0 until 4).map { i ->
                val startRow = (currentBatch + i) * rowLimit
                async(Dispatchers.IO) {
                    fetchBatch(startDate, endDate, startRow, rowLimit)
                }
            }
            
            val results = batchJobs.awaitAll()
            var hasMore = false
            
            results.forEach { batch ->
                if (batch.isNotEmpty()) {
                    allRows.addAll(batch)
                    if (batch.size == rowLimit) hasMore = true
                }
            }
            
            if (!hasMore) break
            currentBatch += 4
        }
        
        logger.info("🎊 전체 데이터셋 로드 완료: ${allRows.size}개 (병렬 처리로 초고속 완료!)")
        allRows
    }
    
    // 단일 배치를 가져오는 헬퍼 함수
    private suspend fun fetchBatch(
        startDate: String,
        endDate: String,
        startRow: Int,
        rowLimit: Int
    ): List<ApiDataRow> = withContext(Dispatchers.IO) {
        try {
            val request = SearchAnalyticsQueryRequest()
                .setStartDate(startDate)
                .setEndDate(endDate)
                .setDimensions(listOf("query", "page"))
                .setRowLimit(rowLimit)
                .setStartRow(startRow)
            
            val response = searchConsoleClient.searchanalytics().query(domain, request).execute()
            logger.info("GSC 배치 로드: ${response.rows?.size ?: 0}개 (startRow: $startRow, date: $startDate)")
            response.rows ?: emptyList()
        } catch (e: Exception) {
            logger.error("배치 로드 실패 (startRow: $startRow): ${e.message}", e)
            emptyList()
        }
    }

    // Entire Dimension list
    // https://developers.google.com/analytics/devguides/reporting/data/v1/api-schema?hl=ko
    @Cacheable(value = ["searchAnalyticsData"], key = "#startDateParam + '_' + #endDateParam + '_' + #limit")
    fun fetchAnalyticsData(
        startDateParam: String? = LocalDate.now().minusDays(3).toString(),
        endDateParam: String? = startDateParam,
        limit: Int? = null
    ): List<PageViewInfo> {
        val startDate = startDateParam ?: LocalDate.now().minusDays(3).toString()
        val endDate = endDateParam ?: startDate

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
            return analyticsDataClient.runReport(request).rowsList.map { row ->
                PageViewInfo(
                    pagePath = row.getDimensionValues(0).value,
                    pageTitle = row.getDimensionValues(1).value,
                    pageViews = row.getMetricValues(0).value.toDouble()
                )
            }
        } catch (e: Exception) {
            logger.error("Analytics Data fetch error", e)
            throw ExternalApiException(
                errorCode = ErrorCode.ANALYTICS_API_ERROR,
                message = "Google Analytics 데이터를 가져오는 중 오류가 발생했습니다",
                cause = e
            )
        }
    }

    fun createExcelFile(allRows: List<ApiDataRow>, analyticsAllRows: List<PageViewInfo>, reportFrequency: ReportFrequency): ByteArrayOutputStream {
        // 메모리 효율을 위해 일부 최적화는 유지하되, 호환성을 위해 XSSFWorkbook 사용
        val workbook = XSSFWorkbook()
        
        try {
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
            
            return outputStream
        } finally {
            workbook.close()
        }
    }

    @Cacheable(value = ["realtimeAnalytics"])
    fun fetchRealTimeAnalyticsData(): List<PageViewInfo> {
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
            return analyticsDataClient.runReport(request).rowsList.map { row ->
                PageViewInfo(
                    pageTitle = row.getDimensionValues(0).value,  // pageTitle
                    pagePath = row.getDimensionValues(1).value,   // pagePath
                    pageViews = row.getMetricValues(0).value.toDouble()
                )
            }
        } catch (e: Exception) {
            logger.error("Real-time analytics fetch error", e)
            throw ExternalApiException(
                errorCode = ErrorCode.ANALYTICS_API_ERROR,
                message = "실시간 분석 데이터를 가져오는 중 오류가 발생했습니다",
                cause = e
            )
        }
    }

    @Cacheable(value = ["realtimeAnalytics"], key = "'withActiveUsers'")
    fun fetchRealTimeAnalyticsWithActiveUsers(): Map<String, Any> {
        val request = RunReportRequest.newBuilder().apply {
            property = "properties/$propId"
            addDateRanges(DateRange.newBuilder().apply {
                this.startDate = "today"
                this.endDate = "today"
            })
            addMetrics(Metric.newBuilder().setName("activeUsers"))
        }.build()

        val activeUsers = try {
            analyticsDataClient.runReport(request).rowsList.firstOrNull()?.getMetricValues(0)?.value?.toInt() ?: 0
        } catch (e: Exception) {
            logger.error("Analytics active users fetch error", e)
            throw ExternalApiException(
                errorCode = ErrorCode.ANALYTICS_API_ERROR,
                message = "활성 사용자 데이터를 가져오는 중 오류가 발생했습니다",
                cause = e
            )
        }

        return mapOf(
            "activeUsers" to activeUsers,
            "pageViews" to fetchRealTimeAnalyticsData()
        )
    }

    @Cacheable(value = ["last30minAnalytics"])
    fun fetchLast30MinAnalyticsWithActiveUsers(): Map<String, Any> {
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
            analyticsDataClient.runRealtimeReport(activeUsersRequest).rowsList
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
            analyticsDataClient.runRealtimeReport(pageViewsRequest).rowsList.map { row ->
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

        return mapOf(
            "activeUsers" to activeUsers,
            "pageViews" to pageViews
        )
    }

    @Cacheable(value = ["customDateAnalytics"], key = "#startDate + '_' + #endDate")
    fun fetchCustomDateAnalyticsWithActiveUsers(startDate: String, endDate: String): Map<String, Any> {
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

            val activeUsers = analyticsDataClient.runReport(request).rowsList
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

            val pageViews = analyticsDataClient.runReport(pageViewsRequest).rowsList.map { row ->
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
        }
    }

    @Cacheable(value = ["hourlyHeatmapData"], key = "#startDate + '_' + #endDate")
    fun fetchHourlyHeatmapData(startDate: String, endDate: String): Map<String, Any> {
        try {
            val request = RunReportRequest.newBuilder().apply {
                property = "properties/$propId"
                addDateRanges(DateRange.newBuilder().apply {
                    this.startDate = startDate
                    this.endDate = endDate
                })
                
                // 시간대와 요일 dimension 추가!!! 🔥
                addDimensions(Dimension.newBuilder().setName("hour"))
                addDimensions(Dimension.newBuilder().setName("dayOfWeek"))
                
                // 활성 사용자와 페이지뷰 메트릭
                addMetrics(Metric.newBuilder().setName("activeUsers"))
                addMetrics(Metric.newBuilder().setName("screenPageViews"))
                
                // 정렬: 요일 -> 시간 순
                addOrderBys(OrderBy.newBuilder().apply {
                    dimension = OrderBy.DimensionOrderBy.newBuilder()
                        .setDimensionName("dayOfWeek")
                        .setOrderType(OrderBy.DimensionOrderBy.OrderType.NUMERIC)
                        .build()
                })
                addOrderBys(OrderBy.newBuilder().apply {
                    dimension = OrderBy.DimensionOrderBy.newBuilder()
                        .setDimensionName("hour")
                        .setOrderType(OrderBy.DimensionOrderBy.OrderType.NUMERIC)
                        .build()
                })
            }.build()
            
            val response = analyticsDataClient.runReport(request)
            
            // 히트맵 데이터 구성 (7일 x 24시간 매트릭스)
            val heatmapData = Array(7) { Array(24) { 0 } }
            val pageViewData = Array(7) { Array(24) { 0 } }
            
            response.rowsList.forEach { row ->
                try {
                    // hour는 "00"~"23" 문자열로 옴
                    val hour = row.getDimensionValues(0).value.toIntOrNull() ?: 0
                    // dayOfWeek는 "0"(일요일)~"6"(토요일) 문자열로 옴
                    val dayOfWeek = row.getDimensionValues(1).value.toIntOrNull() ?: 0
                    
                    val activeUsers = row.getMetricValues(0).value.toIntOrNull() ?: 0
                    val pageViews = row.getMetricValues(1).value.toIntOrNull() ?: 0
                    
                    if (dayOfWeek in 0..6 && hour in 0..23) {
                        // 각 시간대별로 값을 누적 (여러 날짜의 데이터가 합쳐짐)
                        heatmapData[dayOfWeek][hour] += activeUsers
                        pageViewData[dayOfWeek][hour] += pageViews
                    }
                } catch (e: Exception) {
                    logger.warn("히트맵 데이터 파싱 중 오류: ${e.message}")
                }
            }
            
            return mapOf(
                "heatmapData" to heatmapData,
                "pageViewData" to pageViewData,
                "startDate" to startDate,
                "endDate" to endDate
            )
            
        } catch (e: Exception) {
            logger.error("Hourly heatmap data fetch error", e)
            throw ExternalApiException(
                errorCode = ErrorCode.ANALYTICS_API_ERROR,
                message = "시간대별 히트맵 데이터를 가져오는 중 오류가 발생했습니다",
                cause = e
            )
        }
    }
    
    @Cacheable(value = ["hourlyDetailPageViews"], key = "#date + '_' + #hour")
    fun fetchHourlyDetailPageViews(date: String, hour: Int): Map<String, Any> {
        try {
            // 활성 사용자 수 가져오기
            val activeUsersRequest = RunReportRequest.newBuilder().apply {
                property = "properties/$propId"
                addDateRanges(DateRange.newBuilder().apply {
                    this.startDate = date
                    this.endDate = date
                })
                
                // 시간대 필터 추가
                addDimensions(Dimension.newBuilder().setName("hour"))
                addMetrics(Metric.newBuilder().setName("activeUsers"))
                
                // 특정 시간대만 필터링
                dimensionFilter = FilterExpression.newBuilder().apply {
                    filter = Filter.newBuilder().apply {
                        fieldName = "hour"
                        stringFilter = Filter.StringFilter.newBuilder().apply {
                            matchType = Filter.StringFilter.MatchType.EXACT
                            value = hour.toString().padStart(2, '0') // 00~23 형식
                        }.build()
                    }.build()
                }.build()
            }.build()
            
            val activeUsersResponse = analyticsDataClient.runReport(activeUsersRequest)
            val activeUsers = if (activeUsersResponse.rowsCount > 0) {
                activeUsersResponse.getRows(0).getMetricValues(0).value.toIntOrNull() ?: 0
            } else {
                0
            }
            
            // 페이지별 조회수 가져오기
            val pageViewsRequest = RunReportRequest.newBuilder().apply {
                property = "properties/$propId"
                addDateRanges(DateRange.newBuilder().apply {
                    this.startDate = date
                    this.endDate = date
                })
                
                // Dimensions
                addDimensions(Dimension.newBuilder().setName("pageTitle"))
                addDimensions(Dimension.newBuilder().setName("pagePath"))
                addDimensions(Dimension.newBuilder().setName("hour"))
                
                // Metrics
                addMetrics(Metric.newBuilder().setName("screenPageViews"))
                
                // 특정 시간대만 필터링
                dimensionFilter = FilterExpression.newBuilder().apply {
                    filter = Filter.newBuilder().apply {
                        fieldName = "hour"
                        stringFilter = Filter.StringFilter.newBuilder().apply {
                            matchType = Filter.StringFilter.MatchType.EXACT
                            value = hour.toString().padStart(2, '0')
                        }.build()
                    }.build()
                }.build()
                
                // 조회수 기준 내림차순 정렬
                addOrderBys(OrderBy.newBuilder().apply {
                    metric = OrderBy.MetricOrderBy.newBuilder()
                        .setMetricName("screenPageViews")
                        .build()
                    desc = true
                })
                
                limit = 500 // 상위 500개만
            }.build()
            
            val pageViews = analyticsDataClient.runReport(pageViewsRequest).rowsList.map { row ->
                PageViewInfo(
                    pageTitle = row.getDimensionValues(0).value,
                    pagePath = row.getDimensionValues(1).value,
                    pageViews = row.getMetricValues(0).value.toDouble()
                )
            }
            
            return mapOf(
                "activeUsers" to activeUsers,
                "pageViews" to pageViews,
                "hour" to hour,
                "date" to date
            )
            
        } catch (e: Exception) {
            logger.error("Hourly detail pageviews fetch error", e)
            return mapOf(
                "activeUsers" to 0,
                "pageViews" to emptyList<PageViewInfo>(),
                "hour" to hour,
                "date" to date
            )
        }
    }
    
    @Cacheable(value = ["demographicsData"], key = "#startDate + '_' + #endDate")
    fun fetchDemographicsHeatmapData(startDate: String, endDate: String): Map<String, Any> {
        try {
            val request = RunReportRequest.newBuilder().apply {
                property = "properties/$propId"
                addDateRanges(DateRange.newBuilder().apply {
                    this.startDate = startDate
                    this.endDate = endDate
                })
                
                // 성별과 연령 dimension 추가!!! 🔥💕
                addDimensions(Dimension.newBuilder().setName("userGender"))
                addDimensions(Dimension.newBuilder().setName("userAgeBracket"))
                
                // 활성 사용자와 페이지뷰 메트릭
                addMetrics(Metric.newBuilder().setName("activeUsers"))
                addMetrics(Metric.newBuilder().setName("screenPageViews"))
                
                // 정렬: 성별 -> 연령 순
                addOrderBys(OrderBy.newBuilder().apply {
                    dimension = OrderBy.DimensionOrderBy.newBuilder()
                        .setDimensionName("userGender")
                        .build()
                })
                addOrderBys(OrderBy.newBuilder().apply {
                    dimension = OrderBy.DimensionOrderBy.newBuilder()
                        .setDimensionName("userAgeBracket")
                        .build()
                })
            }.build()
            
            val response = analyticsDataClient.runReport(request)
            
            // 연령대 순서 정의
            val ageOrder = listOf("18-24", "25-34", "35-44", "45-54", "55-64", "65+")
            val genderOrder = listOf("female", "male")
            
            // 히트맵 데이터 초기화 (성별 x 연령대)
            val heatmapData = genderOrder.map { gender ->
                ageOrder.map { 0 }.toMutableList()
            }.toMutableList()
            
            val pageViewData = genderOrder.map { gender ->
                ageOrder.map { 0 }.toMutableList()
            }.toMutableList()
            
            // 응답 데이터 처리
            response.rowsList.forEach { row ->
                try {
                    val gender = row.getDimensionValues(0).value
                    val ageBracket = row.getDimensionValues(1).value
                    
                    val activeUsers = row.getMetricValues(0).value.toIntOrNull() ?: 0
                    val pageViews = row.getMetricValues(1).value.toIntOrNull() ?: 0
                    
                    val genderIndex = genderOrder.indexOf(gender)
                    val ageIndex = ageOrder.indexOf(ageBracket)
                    
                    if (genderIndex >= 0 && ageIndex >= 0) {
                        heatmapData[genderIndex][ageIndex] = activeUsers
                        pageViewData[genderIndex][ageIndex] = pageViews
                    }
                } catch (e: Exception) {
                    logger.error("Demographics data 처리 중 오류: ${e.message}")
                }
            }
            
            return mapOf(
                "heatmapData" to heatmapData,
                "pageViewData" to pageViewData,
                "genderLabels" to listOf("여성", "남성"),
                "ageLabels" to ageOrder,
                "startDate" to startDate,
                "endDate" to endDate
            )
            
        } catch (e: Exception) {
            logger.error("Demographics heatmap data fetch error", e)
            
            // 에러 시 빈 데이터 반환
            val ageOrder = listOf("18-24", "25-34", "35-44", "45-54", "55-64", "65+")
            val emptyData = listOf(
                ageOrder.map { 0 },
                ageOrder.map { 0 }
            )
            
            return mapOf(
                "heatmapData" to emptyData,
                "pageViewData" to emptyData,
                "genderLabels" to listOf("여성", "남성"),
                "ageLabels" to ageOrder,
                "startDate" to startDate,
                "endDate" to endDate,
                "error" to "데이터를 가져올 수 없습니다. Google Signals가 활성화되어 있는지 확인해주세요."
            )
        }
    }

}