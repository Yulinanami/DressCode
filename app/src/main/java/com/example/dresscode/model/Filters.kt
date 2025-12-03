package com.example.dresscode.model

data class OutfitFilters(
    val gender: Gender? = null,
    val style: String? = null,
    val season: String? = null,
    val scene: String? = null,
    val weather: String? = null,
    val tags: Set<String> = emptySet()
) {
    fun withDefaults(defaults: OutfitFilters): OutfitFilters {
        return this.copy(
            gender = this.gender ?: defaults.gender,
            tags = if (this.tags.isEmpty()) defaults.tags else this.tags,
            style = this.style ?: defaults.style,
            season = this.season ?: defaults.season,
            scene = this.scene ?: defaults.scene,
            weather = this.weather ?: defaults.weather
        )
    }
}
