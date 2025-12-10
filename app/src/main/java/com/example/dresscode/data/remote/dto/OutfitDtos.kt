package com.example.dresscode.data.remote.dto

data class OutfitDto(
    val id: String,
    val title: String,
    val imageUrl: String? = null,
    @com.squareup.moshi.Json(name = "image_url")
    val imageUrlLegacy: String? = null,
    val images: List<String>? = null,
    val gender: String? = null,
    val tags: OutfitTagsDto? = null,
    val isFavorite: Boolean? = null,
    @com.squareup.moshi.Json(name = "is_user_upload")
    val isUserUpload: Boolean? = null
)

data class OutfitTagsDto(
    val style: List<String>? = null,
    val season: List<String>? = null,
    val scene: List<String>? = null,
    val weather: List<String>? = null,
    val general: List<String>? = null
)

data class PagedResponseDto<T>(
    val items: List<T> = emptyList(),
    val page: Int? = null,
    val pageSize: Int? = null,
    val total: Int? = null
)

data class ToggleFavoriteResponse(
    val isFavorite: Boolean? = null
)

data class OutfitRecommendationRequest(
    val city: String? = null,
    val temperature: Double? = null,
    val weatherText: String? = null,
    val model: String? = null
)

data class OutfitRecommendationResponse(
    val outfit: OutfitDto,
    val reason: String
)
