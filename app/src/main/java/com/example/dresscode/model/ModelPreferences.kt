package com.example.dresscode.model

data class ModelPreferences(
    val tryOnModel: TryOnModel = TryOnModel.AITRYON_PLUS,
    val taggingModel: TaggingModel = TaggingModel.GEMINI_FLASH
)

enum class TryOnModel(val value: String) {
    AITRYON("aitryon"),
    AITRYON_PLUS("aitryon-plus");

    companion object {
        fun fromValue(raw: String?): TryOnModel =
            entries.firstOrNull { it.value == raw } ?: AITRYON_PLUS
    }
}

enum class TaggingModel(val value: String) {
    GEMINI_FLASH("gemini-2.5-flash"),
    GEMINI_FLASH_LITE("gemini-2.5-flash-lite");

    companion object {
        fun fromValue(raw: String?): TaggingModel =
            entries.firstOrNull { it.value == raw } ?: GEMINI_FLASH
    }
}
