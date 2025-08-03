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
import com.si.main.searchinsights.data.ReferralTraffic
import com.si.main.searchinsights.data.TrafficSource
import com.si.main.searchinsights.data.PageFlowData
import com.si.main.searchinsights.data.PageFlowSource
import com.si.main.searchinsights.data.PageFlowDestination
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
            // 로그 제거 - 테스트 완료
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
            // 로그 제거 - 테스트 완료
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
    
    fun fetchReferralTrafficData(
        startDate: String,
        endDate: String
    ): List<ReferralTraffic> {
        try {
            val request = RunReportRequest.newBuilder().apply {
                property = "properties/$propId"
                addDateRanges(DateRange.newBuilder().apply {
                    this.startDate = startDate
                    this.endDate = endDate
                })
                // Dimension: sessionSource (출처 사이트), landingPagePlusQueryString (랜딩 페이지), pageTitle (페이지 제목)
                addDimensions(Dimension.newBuilder().setName("sessionSource"))
                addDimensions(Dimension.newBuilder().setName("landingPagePlusQueryString"))
                addDimensions(Dimension.newBuilder().setName("pageTitle"))
                // Metrics: sessions (세션수), totalUsers (사용자수), screenPageViews (페이지뷰)
                addMetrics(Metric.newBuilder().setName("sessions"))
                addMetrics(Metric.newBuilder().setName("totalUsers"))
                addMetrics(Metric.newBuilder().setName("screenPageViews"))
                // Filter: sessionDefaultChannelGroup이 'Referral'인 것만
                dimensionFilter = FilterExpression.newBuilder()
                    .setFilter(Filter.newBuilder()
                        .setFieldName("sessionDefaultChannelGroup")
                        .setStringFilter(Filter.StringFilter.newBuilder()
                            .setMatchType(Filter.StringFilter.MatchType.EXACT)
                            .setValue("Referral")
                        )
                    )
                    .build()
                // Order by sessions DESC
                addOrderBys(OrderBy.newBuilder().apply {
                    desc = true
                    metric = OrderBy.MetricOrderBy.newBuilder()
                        .setMetricName("sessions")
                        .build()
                })
                limit = 1000 // 넉넉하게 1,000개! 🥳
            }.build()
            
            val response = analyticsDataClient.runReport(request)
            
            return response.rowsList.map { row ->
                val source = row.getDimensionValues(0).value
                val landingPage = row.getDimensionValues(1).value
                val pageTitle = row.getDimensionValues(2).value
                
                // 페이지 제목이 있으면 경로와 함께 표시
                val landingPageDisplay = if (pageTitle.isNotBlank() && pageTitle != "(not set)") {
                    "$landingPage ($pageTitle)"
                } else {
                    landingPage
                }
                
                ReferralTraffic(
                    sourceSite = source,
                    landingPage = landingPageDisplay,
                    sessions = row.getMetricValues(0).value.toIntOrNull() ?: 0,
                    users = row.getMetricValues(1).value.toIntOrNull() ?: 0,
                    pageviews = row.getMetricValues(2).value.toIntOrNull() ?: 0
                )
            }.filter { it.sessions > 0 } // 세션이 0인 것은 제외
            
        } catch (e: Exception) {
            logger.error("Referral traffic data fetch error", e)
            throw ExternalApiException(
                errorCode = ErrorCode.ANALYTICS_API_ERROR,
                message = "Referral 트래픽 데이터를 가져오는 중 오류가 발생했습니다",
                cause = e
            )
        }
    }
    
    /**
     * 도메인별 트래픽 소스 데이터를 가져옵니다.
     * Google, Naver, Daum, Yahoo 등 주요 검색엔진 및 소셜미디어, 직접유입 등 모든 트래픽 소스를 분석합니다.
     */
    fun fetchTrafficBySource(startDate: String, endDate: String): List<TrafficSource> {
        try {
            val request = RunReportRequest.newBuilder().apply {
                property = "properties/$propId"
                addDateRanges(DateRange.newBuilder().apply {
                    this.startDate = startDate
                    this.endDate = endDate
                })
                // Dimensions: sessionSource (출처), sessionDefaultChannelGroup (채널 그룹)
                addDimensions(Dimension.newBuilder().setName("sessionSource"))
                addDimensions(Dimension.newBuilder().setName("sessionDefaultChannelGroup"))
                // Metrics: sessions, totalUsers, screenPageViews
                addMetrics(Metric.newBuilder().setName("sessions"))
                addMetrics(Metric.newBuilder().setName("totalUsers"))
                addMetrics(Metric.newBuilder().setName("screenPageViews"))
                // 세션 수로 정렬 (많은 순)
                addOrderBys(OrderBy.newBuilder()
                    .setDesc(true)
                    .setMetric(OrderBy.MetricOrderBy.newBuilder()
                        .setMetricName("sessions")
                    )
                )
                limit = 100 // 상위 100개 소스
            }.build()
            
            val response = analyticsDataClient.runReport(request)
            
            return response.rowsList.map { row ->
                val source = row.getDimensionValues(0).value
                val channelGroup = row.getDimensionValues(1).value
                val sessions = row.getMetricValues(0).value.toIntOrNull() ?: 0
                val users = row.getMetricValues(1).value.toIntOrNull() ?: 0
                val pageviews = row.getMetricValues(2).value.toIntOrNull() ?: 0
                
                TrafficSource(
                    source = source,
                    channelGroup = channelGroup,
                    sessions = sessions,
                    users = users,
                    pageviews = pageviews
                )
            }.filter { it.sessions > 0 } // 세션이 0인 것은 제외
            
        } catch (e: Exception) {
            logger.error("Traffic source data fetch error", e)
            throw ExternalApiException(
                errorCode = ErrorCode.ANALYTICS_API_ERROR,
                message = "트래픽 소스 데이터를 가져오는 중 오류가 발생했습니다",
                cause = e
            )
        }
    }
    
    /**
     * 특정 도메인/소스에서 유입된 페이지뷰 데이터를 가져옵니다.
     * 어떤 페이지를 많이 봤는지 분석합니다.
     */
    fun fetchPageViewsBySource(
        startDate: String,
        endDate: String,
        sourceDomain: String
    ): Map<String, Any> {
        try {
            // 활성 사용자 수 (해당 소스에서)
            val activeUsersRequest = RunReportRequest.newBuilder().apply {
                property = "properties/$propId"
                addDateRanges(DateRange.newBuilder().apply {
                    this.startDate = startDate
                    this.endDate = endDate
                })
                
                addMetrics(Metric.newBuilder().setName("activeUsers"))
                
                // 특정 소스 필터링
                dimensionFilter = FilterExpression.newBuilder()
                    .setFilter(Filter.newBuilder()
                        .setFieldName("sessionSource")
                        .setStringFilter(Filter.StringFilter.newBuilder()
                            .setMatchType(Filter.StringFilter.MatchType.CONTAINS)
                            .setValue(sourceDomain)
                            .setCaseSensitive(false)
                        )
                    )
                    .build()
            }.build()
            
            val activeUsers = analyticsDataClient.runReport(activeUsersRequest).rowsList
                .firstOrNull()?.getMetricValues(0)?.value?.toInt() ?: 0
            
            // 페이지별 조회수 (해당 소스에서)
            val pageViewsRequest = RunReportRequest.newBuilder().apply {
                property = "properties/$propId"
                addDateRanges(DateRange.newBuilder().apply {
                    this.startDate = startDate
                    this.endDate = endDate
                })
                
                // Dimensions
                addDimensions(Dimension.newBuilder().setName("pageTitle"))
                addDimensions(Dimension.newBuilder().setName("pagePath"))
                
                // Metrics
                addMetrics(Metric.newBuilder().setName("screenPageViews"))
                addMetrics(Metric.newBuilder().setName("activeUsers"))
                
                // 특정 소스 필터링
                dimensionFilter = FilterExpression.newBuilder()
                    .setFilter(Filter.newBuilder()
                        .setFieldName("sessionSource")
                        .setStringFilter(Filter.StringFilter.newBuilder()
                            .setMatchType(Filter.StringFilter.MatchType.CONTAINS)
                            .setValue(sourceDomain)
                            .setCaseSensitive(false)
                        )
                    )
                    .build()
                
                // 조회수 기준 내림차순 정렬
                addOrderBys(OrderBy.newBuilder().apply {
                    metric = OrderBy.MetricOrderBy.newBuilder()
                        .setMetricName("screenPageViews")
                        .build()
                    desc = true
                })
                
                limit = 500 // 상위 500개
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
                "sourceDomain" to sourceDomain,
                "startDate" to startDate,
                "endDate" to endDate
            )
            
        } catch (e: Exception) {
            logger.error("Page views by source fetch error", e)
            return mapOf(
                "activeUsers" to 0,
                "pageViews" to emptyList<PageViewInfo>(),
                "sourceDomain" to sourceDomain,
                "error" to (e.message ?: "Unknown error")
            )
        }
    }
    
    fun fetchDemographicsDetailData(
        startDate: String, 
        endDate: String, 
        gender: String, 
        ageGroup: String
    ): Map<String, Any> {
        try {
            // 페이지별 데이터 가져오기
            val pageViewsRequest = RunReportRequest.newBuilder().apply {
                property = "properties/$propId"
                addDateRanges(DateRange.newBuilder().apply {
                    this.startDate = startDate
                    this.endDate = endDate
                })
                
                // 페이지 정보
                addDimensions(Dimension.newBuilder().setName("pageTitle"))
                addDimensions(Dimension.newBuilder().setName("pagePath"))
                
                // 성별과 연령 dimension으로 필터링
                dimensionFilter = FilterExpression.newBuilder()
                    .setAndGroup(FilterExpressionList.newBuilder()
                        .addExpressions(FilterExpression.newBuilder()
                            .setFilter(Filter.newBuilder()
                                .setFieldName("userGender")
                                .setStringFilter(Filter.StringFilter.newBuilder()
                                    .setMatchType(Filter.StringFilter.MatchType.EXACT)
                                    .setValue(gender)
                                )
                            )
                        )
                        .addExpressions(FilterExpression.newBuilder()
                            .setFilter(Filter.newBuilder()
                                .setFieldName("userAgeBracket")
                                .setStringFilter(Filter.StringFilter.newBuilder()
                                    .setMatchType(Filter.StringFilter.MatchType.EXACT)
                                    .setValue(ageGroup)
                                )
                            )
                        )
                    )
                    .build()
                
                // 페이지뷰 메트릭
                addMetrics(Metric.newBuilder().setName("screenPageViews"))
                
                // 페이지뷰 기준 내림차순 정렬
                addOrderBys(OrderBy.newBuilder().apply {
                    metric = OrderBy.MetricOrderBy.newBuilder()
                        .setMetricName("screenPageViews")
                        .build()
                    desc = true
                })
                
                limit = 200 // 상위 200개 페이지
            }.build()
            
            val pageViews = analyticsDataClient.runReport(pageViewsRequest).rowsList.map { row ->
                PageViewInfo(
                    pageTitle = row.getDimensionValues(0).value,
                    pagePath = row.getDimensionValues(1).value,
                    pageViews = row.getMetricValues(0).value.toDouble()
                )
            }
            
            return mapOf(
                "pageViews" to pageViews,
                "gender" to gender,
                "ageGroup" to ageGroup,
                "startDate" to startDate,
                "endDate" to endDate
            )
            
        } catch (e: Exception) {
            logger.error("Demographics detail data fetch error", e)
            return mapOf(
                "pageViews" to emptyList<PageViewInfo>(),
                "error" to (e.message ?: "Unknown error")
            )
        }
    }
    
    // 페이지 플로우 데이터 가져오기 (GA4 page_referrer 활용)
    fun fetchPageFlowData(pagePath: String, startDate: String, endDate: String): PageFlowData {
        logger.info("페이지 플로우 분석 시작: $pagePath ($startDate ~ $endDate)")
        
        // 1. 해당 페이지의 총 조회수 가져오기
        val pageViews = fetchPageTotalViews(pagePath, startDate, endDate)
        
        // 2. 이전 페이지 (어디서 왔나?) 분석
        val sources = fetchPageSources(pagePath, startDate, endDate)
        
        // 3. 다음 페이지 (어디로 갔나?) 분석 - 현재 페이지를 referrer로 가진 페이지들
        val destinations = fetchPageDestinations(pagePath, startDate, endDate)
        
        // 페이지 제목 가져오기
        val pageTitle = getPageTitle(pagePath, startDate, endDate)
        
        return PageFlowData(
            pagePath = pagePath,
            pageTitle = pageTitle,
            totalViews = pageViews,
            sources = sources,
            destinations = destinations
        )
    }
    
    // 특정 페이지의 총 조회수
    private fun fetchPageTotalViews(pagePath: String, startDate: String, endDate: String): Int {
        val request = RunReportRequest.newBuilder().apply {
            property = "properties/$propId"
            addDateRanges(DateRange.newBuilder().apply {
                this.startDate = startDate
                this.endDate = endDate
            })
            
            addDimensions(Dimension.newBuilder().setName("pagePath"))
            addMetrics(Metric.newBuilder().setName("screenPageViews"))
            
            // 특정 페이지만 필터링
            dimensionFilter = FilterExpression.newBuilder()
                .setFilter(Filter.newBuilder()
                    .setFieldName("pagePath")
                    .setStringFilter(Filter.StringFilter.newBuilder()
                        .setMatchType(Filter.StringFilter.MatchType.EXACT)
                        .setValue(pagePath)
                    )
                ).build()
        }.build()
        
        return try {
            val response = analyticsDataClient.runReport(request)
            response.rowsList.firstOrNull()?.getMetricValues(0)?.value?.toInt() ?: 0
        } catch (e: Exception) {
            logger.error("페이지 조회수 가져오기 실패: $pagePath", e)
            0
        }
    }
    
    // 페이지로의 유입 경로 (이전 페이지) 분석
    private fun fetchPageSources(pagePath: String, startDate: String, endDate: String): List<PageFlowSource> {
        val request = RunReportRequest.newBuilder().apply {
            property = "properties/$propId"
            addDateRanges(DateRange.newBuilder().apply {
                this.startDate = startDate
                this.endDate = endDate
            })
            
            // pageReferrer를 사용하여 이전 페이지 추적
            addDimensions(Dimension.newBuilder().setName("pageReferrer"))
            addDimensions(Dimension.newBuilder().setName("pagePath"))
            addMetrics(Metric.newBuilder().setName("sessions"))
            
            // 현재 페이지로의 유입만 필터링
            dimensionFilter = FilterExpression.newBuilder()
                .setFilter(Filter.newBuilder()
                    .setFieldName("pagePath")
                    .setStringFilter(Filter.StringFilter.newBuilder()
                        .setMatchType(Filter.StringFilter.MatchType.EXACT)
                        .setValue(pagePath)
                    )
                ).build()
                
            // 세션수 기준 정렬
            addOrderBys(OrderBy.newBuilder().apply {
                metric = OrderBy.MetricOrderBy.newBuilder()
                    .setMetricName("sessions")
                    .build()
                desc = true
            })
            
            limit = 20 // 상위 20개 소스
        }.build()
        
        return try {
            val response = analyticsDataClient.runReport(request)
            val totalSessions = response.rowsList.sumOf { 
                it.getMetricValues(0).value.toIntOrNull() ?: 0 
            }
            
            response.rowsList.map { row ->
                val referrer = row.getDimensionValues(0).value
                val sessions = row.getMetricValues(0).value.toIntOrNull() ?: 0
                val percentage = if (totalSessions > 0) {
                    (sessions.toDouble() / totalSessions * 100)
                } else 0.0
                
                // 외부/내부 구분
                val isExternal = !referrer.startsWith("/") && referrer != "(not set)"
                
                PageFlowSource(
                    sourcePage = if (referrer == "(not set)") "직접 유입" else referrer,
                    sourceTitle = if (!isExternal && referrer != "(not set)") {
                        getPageTitle(referrer, startDate, endDate)
                    } else "",
                    sessions = sessions,
                    percentage = percentage,
                    isExternal = isExternal
                )
            }
        } catch (e: Exception) {
            logger.error("페이지 유입 경로 분석 실패: $pagePath", e)
            emptyList()
        }
    }
    
    // 페이지에서의 이탈 경로 (다음 페이지) 분석
    private fun fetchPageDestinations(pagePath: String, startDate: String, endDate: String): List<PageFlowDestination> {
        // GA4에서는 다음 페이지를 직접 추적하기 어려우므로
        // 현재 페이지를 pageReferrer로 가진 페이지들을 찾아서 분석
        val request = RunReportRequest.newBuilder().apply {
            property = "properties/$propId"
            addDateRanges(DateRange.newBuilder().apply {
                this.startDate = startDate
                this.endDate = endDate
            })
            
            addDimensions(Dimension.newBuilder().setName("pageReferrer"))
            addDimensions(Dimension.newBuilder().setName("pagePath"))
            addDimensions(Dimension.newBuilder().setName("pageTitle"))
            addMetrics(Metric.newBuilder().setName("sessions"))
            
            // 현재 페이지가 referrer인 경우만 필터링
            dimensionFilter = FilterExpression.newBuilder()
                .setFilter(Filter.newBuilder()
                    .setFieldName("pageReferrer")
                    .setStringFilter(Filter.StringFilter.newBuilder()
                        .setMatchType(Filter.StringFilter.MatchType.CONTAINS)
                        .setValue(pagePath)
                    )
                ).build()
                
            addOrderBys(OrderBy.newBuilder().apply {
                metric = OrderBy.MetricOrderBy.newBuilder()
                    .setMetricName("sessions")
                    .build()
                desc = true
            })
            
            limit = 20
        }.build()
        
        return try {
            val response = analyticsDataClient.runReport(request)
            val destinations = mutableListOf<PageFlowDestination>()
            
            val totalSessions = response.rowsList.sumOf { 
                it.getMetricValues(0).value.toIntOrNull() ?: 0 
            }
            
            // 실제 다음 페이지들
            response.rowsList.forEach { row ->
                val nextPage = row.getDimensionValues(1).value
                val nextTitle = row.getDimensionValues(2).value
                val sessions = row.getMetricValues(0).value.toIntOrNull() ?: 0
                val percentage = if (totalSessions > 0) {
                    (sessions.toDouble() / totalSessions * 100)
                } else 0.0
                
                destinations.add(PageFlowDestination(
                    destinationPage = nextPage,
                    destinationTitle = nextTitle,
                    sessions = sessions,
                    percentage = percentage,
                    isExit = false
                ))
            }
            
            // 이탈률 계산 - 전체 페이지뷰 기준이 아닌, 소스에서 온 세션 기준으로 계산
            val sourceTotalSessions = fetchPageSources(pagePath, startDate, endDate).sumOf { it.sessions }
            val exitSessions = if (sourceTotalSessions > totalSessions) {
                sourceTotalSessions - totalSessions
            } else {
                0
            }
            
            // 퍼센트 재계산 (이탈 포함한 전체 기준)
            val totalWithExit = totalSessions + exitSessions
            val recalculatedDestinations = destinations.map { dest ->
                dest.copy(
                    percentage = if (totalWithExit > 0) {
                        (dest.sessions.toDouble() / totalWithExit * 100)
                    } else 0.0
                )
            }.toMutableList()
            
            if (exitSessions > 0) {
                val exitPercentage = (exitSessions.toDouble() / totalWithExit * 100)
                recalculatedDestinations.add(PageFlowDestination(
                    destinationPage = "사이트 이탈",
                    destinationTitle = "",
                    sessions = exitSessions,
                    percentage = exitPercentage,
                    isExit = true
                ))
            }
            
            recalculatedDestinations.sortedByDescending { it.sessions }
        } catch (e: Exception) {
            logger.error("페이지 이탈 경로 분석 실패: $pagePath", e)
            emptyList()
        }
    }
    
    // 페이지 제목 가져오기
    private fun getPageTitle(pagePath: String, startDate: String, endDate: String): String {
        val request = RunReportRequest.newBuilder().apply {
            property = "properties/$propId"
            addDateRanges(DateRange.newBuilder().apply {
                this.startDate = startDate
                this.endDate = endDate
            })
            
            addDimensions(Dimension.newBuilder().setName("pagePath"))
            addDimensions(Dimension.newBuilder().setName("pageTitle"))
            addMetrics(Metric.newBuilder().setName("screenPageViews"))
            
            dimensionFilter = FilterExpression.newBuilder()
                .setFilter(Filter.newBuilder()
                    .setFieldName("pagePath")
                    .setStringFilter(Filter.StringFilter.newBuilder()
                        .setMatchType(Filter.StringFilter.MatchType.EXACT)
                        .setValue(pagePath)
                    )
                ).build()
                
            limit = 1
        }.build()
        
        return try {
            val response = analyticsDataClient.runReport(request)
            response.rowsList.firstOrNull()?.getDimensionValues(1)?.value ?: "(제목 없음)"
        } catch (e: Exception) {
            logger.warn("페이지 제목 가져오기 실패: $pagePath", e)
            "(제목 없음)"
        }
    }

}