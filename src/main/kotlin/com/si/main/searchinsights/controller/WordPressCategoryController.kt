package com.si.main.searchinsights.controller

import com.si.main.searchinsights.service.WordPressCategoryService
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.io.File

@RestController
class WordPressCategoryController(
    private val wordPressCategoryService: WordPressCategoryService
) {
    
    @GetMapping("/api/wp-categories-data", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getWpCategoriesData(): ResponseEntity<Resource> {
        val file = File("src/main/resources/static/wp_categories_data.json")
        return if (file.exists()) {
            ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(FileSystemResource(file))
        } else {
            ResponseEntity.notFound().build()
        }
    }
}