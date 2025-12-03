package com.example.dresscode.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "outfit_remote_keys", primaryKeys = ["id", "filterKey"])
data class OutfitRemoteKey(
    val id: String,
    val filterKey: String,
    val prevKey: Int?,
    val nextKey: Int?
)
