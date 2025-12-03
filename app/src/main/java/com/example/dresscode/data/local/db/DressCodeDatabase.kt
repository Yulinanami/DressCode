package com.example.dresscode.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.dresscode.data.local.dao.FavoriteDao
import com.example.dresscode.data.local.dao.OutfitDao
import com.example.dresscode.data.local.dao.OutfitRemoteKeyDao
import com.example.dresscode.data.local.dao.SearchHistoryDao
import com.example.dresscode.data.local.entity.FavoriteEntity
import com.example.dresscode.data.local.entity.OutfitEntity
import com.example.dresscode.data.local.entity.OutfitRemoteKey
import com.example.dresscode.data.local.entity.SearchHistoryEntity

@Database(
    entities = [
        OutfitEntity::class,
        OutfitRemoteKey::class,
        FavoriteEntity::class,
        SearchHistoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(DbConverters::class)
abstract class DressCodeDatabase : RoomDatabase() {
    abstract fun outfitDao(): OutfitDao
    abstract fun remoteKeyDao(): OutfitRemoteKeyDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun searchHistoryDao(): SearchHistoryDao
}
