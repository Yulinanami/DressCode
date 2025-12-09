package com.example.dresscode.model

enum class Gender { FEMALE, MALE, UNISEX }

data class WeatherUiState(
    val title: String = "天气",
    val city: String = "未定位",
    val summary: String = "等待天气数据",
    val temperature: String = "--°",
    val isLoading: Boolean = false,
    val isRecommending: Boolean = false,
    val recommendation: WeatherRecommendation? = null,
    val error: String? = null
)

data class WeatherRecommendation(
    val outfit: OutfitPreview,
    val reason: String
)

data class AuthState(
    val isLoggedIn: Boolean = false,
    val email: String? = null,
    val displayName: String? = null,
    val token: String? = null
)

data class OutfitUiState(
    val title: String = "穿搭",
    val highlight: String = "探索穿搭灵感",
    val filters: String = "默认筛选：全部",
    val query: String = "",
    val recentQueries: List<String> = emptyList(),
    val selectedGender: Gender? = null,
    val selectedStyle: String? = null,
    val selectedSeason: String? = null,
    val selectedScene: String? = null,
    val selectedWeather: String? = null,
    val selectedTags: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val error: String? = null
)

data class TryOnUiState(
    val title: String = "智能换装",
    val status: String = "暂未提交任务",
    val hint: String = "上传人像与穿搭",
    val selectedPhotoLabel: String? = null,
    val selectedOutfitTitle: String? = null,
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val resultPreview: String? = null,
    val selectedPhotoBytes: ByteArray? = null,
    val selectedOutfitBytes: ByteArray? = null,
    val resultImageBase64: String? = null
)

data class TaggingUiState(
    val status: String = "上传穿搭图获取标签",
    val selectedFileName: String? = null,
    val suggestedName: String? = null,
    val tagsPreview: String? = null,
    val isUploading: Boolean = false,
    val error: String? = null
)

data class ProfileUiState(
    val title: String = "我的",
    val subtitle: String = "未登录",
    val notes: String = "设置性别与默认筛选"
)

data class OutfitPreview(
    val id: String,
    val title: String,
    val imageUrl: String? = null,
    val tags: List<String>,
    val gender: Gender = Gender.UNISEX,
    val isFavorite: Boolean = false,
    val isUserUpload: Boolean = false
)

data class OutfitDetail(
    val id: String,
    val title: String,
    val images: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val isUserUpload: Boolean = false
)
