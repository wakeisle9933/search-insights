package com.si.main.searchinsights.service

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import mu.KotlinLogging
import org.springframework.stereotype.Service

@Service
class GoogleTrendsService {
    companion object {
        private val logger = KotlinLogging.logger {}
    }
    fun fetchGoogleTrends(): List<String> {
        val scriptPath = javaClass.classLoader.getResource("python/fetch_trends.py")?.toURI()?.path
            ?: throw IllegalStateException("Python script not found")

        // Windows에서 경로 문제 해결
        val fixedPath = if (System.getProperty("os.name").toLowerCase().contains("win")) {
            scriptPath.removePrefix("/")
        } else {
            scriptPath
        }

        val process = ProcessBuilder("python", fixedPath).start()

        val result = process.inputStream.bufferedReader().readText()
        val error = process.errorStream.bufferedReader().readText()
        process.waitFor()

        if (error.isNotEmpty()) {
            logger.error("Error from Python script: $error")
            return emptyList()
        }

        if (result.isEmpty()) {
            logger.error("No data received from Python script.")
            return emptyList()
        }

        return try {
            jacksonObjectMapper().readValue(result, object : TypeReference<List<String>>() {})
        } catch (e: Exception) {
            logger.error("Error parsing Google Trends data", e)
            emptyList()
        }
    }
}