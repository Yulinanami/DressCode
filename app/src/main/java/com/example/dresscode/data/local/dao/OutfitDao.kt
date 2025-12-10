package com.example.dresscode.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.dresscode.data.local.entity.OutfitEntity

@Dao
interface OutfitDao {

    @Query("SELECT * FROM outfits WHERE filterKey = :filterKey ORDER BY page, indexInPage")
    fun pagingSource(filterKey: String): PagingSource<Int, OutfitEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<OutfitEntity>)

    @Query("DELETE FROM outfits WHERE filterKey = :filterKey")
    suspend fun clearByFilter(filterKey: String)

    @Query("DELETE FROM outfits WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE outfits SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: String, isFavorite: Boolean)

    @Query("UPDATE outfits SET isFavorite = 0")
    suspend fun clearFavoriteFlags()

    @Query("SELECT * FROM outfits WHERE id IN (:ids)")
    suspend fun findByIds(ids: List<String>): List<OutfitEntity>
}
