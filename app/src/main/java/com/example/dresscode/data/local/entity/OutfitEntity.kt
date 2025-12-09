package com.example.dresscode.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.dresscode.model.Gender

@Entity(tableName = "outfits", primaryKeys = ["id", "filterKey"])
data class OutfitEntity(
    val id: String,
    val filterKey: String,
    val title: String,
    val imageUrl: String? = null,
    val gender: Gender? = null,
    val style: String? = null,
    val season: String? = null,
    val scene: String? = null,
    val weather: String? = null,
    val tags: List<String> = emptyList(),
    val isFavorite: Boolean = false,
    val isUserUpload: Boolean = false,
    val page: Int = 0,
    val indexInPage: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
)
