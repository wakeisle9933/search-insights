package com.si.main.searchinsights.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.si.main.searchinsights.extension.logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.io.File

@Service
class WordPressCategoryService(
    @Value("\${domain}")
    private val domain: String
) {
    private val logger = logger()
    private val restTemplate = RestTemplate()
    private val objectMapper = ObjectMapper()
    private val categoryDataFile = "wp_categories_data.json"
    private val postDataFolder = File("wp_posts_data").apply { mkdirs() }

    // 카테고리 및 포스트 데이터 구조
    private var categories = mutableMapOf<Int, String>()
    private var postCategories = mutableMapOf<Int, List<Int>>()
    private var lastFetchedPostId = 0

    init {
        loadCachedData()
    }

    /**
     * 캐시된 데이터 불러오기
     */
    private fun loadCachedData() {
        val file = File(categoryDataFile)
        if (file.exists()) {
            try {
                val jsonNode = objectMapper.readTree(file)

                // 카테고리 정보 로딩
                val categoriesNode = jsonNode.get("categories")
                categoriesNode.fields().forEach { entry ->
                    categories[entry.key.toInt()] = entry.value.asText()
                }

                // 포스트-카테고리 매핑 로딩
                val postsNode = jsonNode.get("posts")
                postsNode.fields().forEach { entry ->
                    val postId = entry.key.toInt()
                    val categoryIds = entry.value.map { it.asInt() }
                    postCategories[postId] = categoryIds

                    // 가장 높은 포스트 ID 찾기
                    if (postId > lastFetchedPostId) {
                        lastFetchedPostId = postId
                    }
                }

                logger.info("🎉 캐시된 카테고리 데이터 로드 완료! 카테고리 ${categories.size}개, 포스트 ${postCategories.size}개")
            } catch (e: Exception) {
                logger.error("⚠️ 캐시된 카테고리 데이터 로드 실패", e)
            }
        }
    }

    /**
     * 모든 카테고리와 포스트를 가져오는 전체 동기화 실행
     */
    fun fullSync(forceFullSync: Boolean = false) {
        logger.info("🔄 워드프레스 카테고리 동기화 시작...")

        // 카테고리는 항상 전체 가져오기
        fetchAllCategories()

        // forceFullSync가 true이거나 wp_categories_data.json 파일이 없을 때만 전체 동기화
        val file = File(categoryDataFile)
        if (forceFullSync || !file.exists()) {
            logger.info("📚 전체 포스트 동기화 시작!")
            fetchAllPosts()
        } else {
            logger.info("⚡ 증분 업데이트 시작! 최신 포스트만 확인!")
            fetchNewPosts()
        }

        saveCategoryData()
        logger.info("✅ 워드프레스 카테고리 동기화 완료!")
    }

    /**
     * 모든 워드프레스 카테고리 가져오기
     */
    private fun fetchAllCategories() {
        try {
            var page = 1
            var fetchedCategories: List<JsonNode>
            val allCategories = mutableListOf<JsonNode>()

            do {
                val url = "${domain.removeSuffix("/")}/wp-json/wp/v2/categories?per_page=100&page=$page"
                fetchedCategories = restTemplate.getForObject(url, Array<JsonNode>::class.java)?.toList() ?: emptyList()
                allCategories.addAll(fetchedCategories)
                page++
            } while (fetchedCategories.isNotEmpty() && fetchedCategories.size == 100)

            categories.clear()
            for (category in allCategories) {
                val id = category.get("id").asInt()
                val name = category.get("name").asText()
                categories[id] = name
            }

            logger.info("📚 총 ${categories.size}개 카테고리 가져오기 완료!")
        } catch (e: Exception) {
            logger.error("⚠️ 워드프레스 카테고리 가져오기 실패", e)
        }
    }

    /**
     * 모든 워드프레스 포스트 가져오기
     */
    private fun fetchAllPosts() {
        lastFetchedPostId = 0
        postCategories.clear()
        fetchNewPosts()
    }

    /**
     * 새로운 워드프레스 포스트만 가져오기 (증분 업데이트)
     */
    private fun fetchNewPosts() {
        try {
            logger.info("🚀 최신 포스트 확인 시작! 페이지당 100개씩 가져올게요~")

            var page = 1
            var fetchedPosts: List<JsonNode>
            var newPostsCount = 0
            var foundExistingPost = false

            do {
                val url = "${domain.removeSuffix("/")}/wp-json/wp/v2/posts?per_page=100&page=$page"
                logger.info("✨ ${page}번째 페이지 요청 중...")
                fetchedPosts = restTemplate.getForObject(url, Array<JsonNode>::class.java)?.toList() ?: emptyList()
                logger.info("📝 ${page}번째 페이지: ${fetchedPosts.size}개 포스트 가져옴!")

                // 이미 처리한 포스트를 만나면 중단하는 로직
                for (post in fetchedPosts) {
                    val postId = post.get("id").asInt()

                    // 이미 데이터베이스에 있는 포스트를 만나면
                    if (postCategories.containsKey(postId)) {
                        foundExistingPost = true
                        logger.info("🛑 포스트 ID $postId 는 이미 있어요! 여기서 중단할게요.")
                        break
                    }

                    val categoryIds = post.get("categories").map { it.asInt() }
                    postCategories[postId] = categoryIds

                    if (postId > lastFetchedPostId) {
                        lastFetchedPostId = postId
                    }

                    newPostsCount++
                }

                page++
                // 이미 처리한 포스트를 만났거나, 페이지의 크기가 100보다 작은 경우(마지막 페이지) 중단
            } while (!foundExistingPost && fetchedPosts.isNotEmpty() && fetchedPosts.size == 100)

            logger.info("📝 새 포스트 ${newPostsCount}개 가져오기 완료! 이제 총 ${postCategories.size}개 포스트 정보 보유 중!")
        } catch (e: Exception) {
            logger.error("⚠️ 워드프레스 포스트 가져오기 실패", e)
        }
    }

    /**
     * 카테고리 및 포스트 정보 저장
     */
    private fun saveCategoryData() {
        try {
            val rootNode = objectMapper.createObjectNode()

            // 카테고리 정보 저장
            val categoriesNode = objectMapper.createObjectNode()
            categories.forEach { (id, name) -> categoriesNode.put(id.toString(), name) }
            rootNode.set<JsonNode>("categories", categoriesNode)

            // 포스트-카테고리 매핑 저장
            val postsNode = objectMapper.createObjectNode()
            postCategories.forEach { (postId, categoryIds) ->
                val categoryArray = objectMapper.createArrayNode()
                categoryIds.forEach { categoryArray.add(it) }
                postsNode.set<JsonNode>(postId.toString(), categoryArray)
            }
            rootNode.set<JsonNode>("posts", postsNode)

            // 파일에 저장
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(File(categoryDataFile), rootNode)
            logger.info("💾 카테고리 데이터 저장 완료!")
        } catch (e: Exception) {
            logger.error("⚠️ 카테고리 데이터 저장 실패", e)
        }
    }

    /**
     * 특정 URL 경로의 카테고리 가져오기
     */
    fun getCategoriesForPath(path: String): List<String> {
        // URL 경로에서 포스트 ID 추출 (예: "/72821" -> 72821)
        val postId = extractPostIdFromPath(path) ?: return emptyList()

        // 포스트 ID에 해당하는 카테고리 ID 목록 가져오기
        val categoryIds = postCategories[postId] ?: return emptyList()

        // 카테고리 ID를 카테고리 이름으로 변환
        return categoryIds.mapNotNull { categories[it] }
    }

    /**
     * URL 경로에서 포스트 ID 추출
     */
    private fun extractPostIdFromPath(path: String): Int? {
        // "/72821" 또는 "/posts/72821" 같은 패턴에서 숫자 추출
        val regex = ".*/([0-9]+)/?.*".toRegex()
        val matchResult = regex.find(path)
        return matchResult?.groupValues?.get(1)?.toIntOrNull()
    }

    /**
     * 카테고리별 페이지뷰 데이터 가져오기
     */
    fun getCategoryPageViews(pageViews: List<com.si.main.searchinsights.data.PageViewInfo>): Map<String, Double> {
        val categoryViews = mutableMapOf<String, Double>()

        // 각 페이지뷰 항목에 대해
        for (pageView in pageViews) {
            // 페이지 경로에 대한 카테고리 가져오기
            val categories = getCategoriesForPath(pageView.pagePath)

            // 각 카테고리에 페이지뷰 추가
            for (category in categories) {
                categoryViews[category] = (categoryViews[category] ?: 0.0) + pageView.pageViews
            }
        }

        // 페이지뷰 기준으로 내림차순 정렬하여 반환
        return categoryViews.toList().sortedByDescending { it.second }.toMap()
    }
}