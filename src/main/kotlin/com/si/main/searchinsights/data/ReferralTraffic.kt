package com.si.main.searchinsights.data

data class ReferralTraffic(
    val sourceSite: String,      // 출처 사이트 (예: google.com, naver.com)
    val landingPage: String,     // 랜딩 페이지 (예: /blog/post-1)
    val sessions: Int,           // 세션수
    val users: Int = 0,          // 사용자 수 (선택적)
    val pageviews: Int = 0       // 페이지뷰 수 (선택적)
)