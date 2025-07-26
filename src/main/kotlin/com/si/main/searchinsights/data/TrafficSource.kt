package com.si.main.searchinsights.data

data class TrafficSource(
    val source: String,           // 트래픽 소스 (예: google, naver, daum 등)
    val channelGroup: String,     // 채널 그룹 (예: Organic Search, Direct, Referral 등)
    val sessions: Int,            // 세션 수
    val users: Int,               // 사용자 수
    val pageviews: Int           // 페이지뷰 수
)