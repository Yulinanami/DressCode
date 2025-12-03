package com.example.dresscode.data.repository

import com.example.dresscode.model.OutfitFilters

object OutfitFilterKey {
    fun build(query: String, filters: OutfitFilters): String {
        val normalizedQuery = query.trim().lowercase()
        val tags = filters.tags.map { it.lowercase() }.sorted().joinToString(",")
        val parts = listOf(
            filters.gender?.name ?: "any",
            filters.style.orEmpty().lowercase(),
            filters.season.orEmpty().lowercase(),
            filters.scene.orEmpty().lowercase(),
            filters.weather.orEmpty().lowercase(),
            tags,
            normalizedQuery
        )
        return parts.joinToString("|")
    }
}
