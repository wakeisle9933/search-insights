package com.si.main.searchinsights.data

data class PrefixSummary(
    val prefix: String,
    val avgPosition: Double,
    val totalClicks: Double,
    val totalImpressions: Double,
    val avgCTR: Double
)