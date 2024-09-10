package com.si.main.searchinsights.service

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import mu.KotlinLogging
import org.springframework.stereotype.Service

@Service
class GoogleTrendsService {
    private val logger = KotlinLogging.logger {}

    private val mapper = jacksonObjectMapper()

    fun fetchTrendingSearches(): List<String> {
        val result = executePythonScript("trending_searches")
        return mapper.convertValue(result, object : TypeReference<List<String>>() {})
    }

    fun fetchRelatedQueries(keyword: String): Map<String, Any> {
        val result = executePythonScript("related_queries", keyword)
        return mapper.convertValue(result, object : TypeReference<Map<String, Any>>() {})
    }

    fun fetchRelatedTopics(keyword: String): Map<String, Any> {
        val result = executePythonScript("related_topics", keyword)
        return mapper.convertValue(result, object : TypeReference<Map<String, Any>>() {})
    }

    fun fetchSuggestions(keyword: String): List<Map<String, String>> {
        val result = executePythonScript("suggestions", keyword)
        return mapper.convertValue(result, object : TypeReference<List<Map<String, String>>>() {})
    }

    private fun executePythonScript(method: String, vararg args: String): Any {
        // 절대 경로로 수정
        val scriptPath = "/app/src/main/resources/python/fetch_trends.py"

        val command = listOf("python3", scriptPath, method) + args
        val process = ProcessBuilder(command).start()

        val result = process.inputStream.bufferedReader().readText()
        val error = process.errorStream.bufferedReader().readText()
        process.waitFor()

        if (error.isNotEmpty()) {
            logger.error("Error from Python script: $error")
            throw RuntimeException("Error executing Python script")
        }

        return jacksonObjectMapper().readValue(result, Any::class.java)
    }
}