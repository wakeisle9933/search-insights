package com.si.main.searchinsights.data

data class PageFlowData(
    val pagePath: String,
    val pageTitle: String,
    val totalViews: Int,
    val sources: List<PageFlowSource>,      // 어디서 왔나? (이전 페이지)
    val destinations: List<PageFlowDestination>  // 어디로 갔나? (다음 페이지)
)

data class PageFlowSource(
    val sourcePage: String,      // 이전 페이지 경로 또는 출처
    val sourceTitle: String = "", // 페이지 제목 (내부 페이지인 경우)
    val sessions: Int,           // 세션 수
    val percentage: Double,      // 전체 대비 비율
    val isExternal: Boolean = false  // 외부 사이트 여부
)

data class PageFlowDestination(
    val destinationPage: String,  // 다음 페이지 경로
    val destinationTitle: String = "", // 페이지 제목
    val sessions: Int,           // 세션 수
    val percentage: Double,      // 전체 대비 비율
    val isExit: Boolean = false  // 사이트 이탈 여부
)