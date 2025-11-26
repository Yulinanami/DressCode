package com.example.dresscode.model

data class OutfitFilters(
    val gender: Gender? = null,
    val tags: Set<String> = emptySet()
)
