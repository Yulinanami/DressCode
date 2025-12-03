package com.example.dresscode.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.dresscode.model.Gender

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val outfitId: String,
    val title: String,
    val imageUrl: String? = null,
    val gender: Gender? = null,
    val tags: List<String> = emptyList(),
    val addedAt: Long = System.currentTimeMillis()
)
