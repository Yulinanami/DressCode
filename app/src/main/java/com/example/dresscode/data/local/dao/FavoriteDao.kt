package com.example.dresscode.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.dresscode.data.local.entity.FavoriteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {

    @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    fun favorites(): Flow<List<FavoriteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE outfitId = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM favorites")
    suspend fun clearAll()

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE outfitId = :id)")
    suspend fun isFavorite(id: String): Boolean
}
