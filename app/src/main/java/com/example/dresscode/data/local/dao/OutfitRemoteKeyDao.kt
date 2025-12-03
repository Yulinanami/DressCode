package com.example.dresscode.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.dresscode.data.local.entity.OutfitRemoteKey

@Dao
interface OutfitRemoteKeyDao {

    @Query("SELECT * FROM outfit_remote_keys WHERE filterKey = :filterKey AND id = :id")
    suspend fun remoteKey(filterKey: String, id: String): OutfitRemoteKey?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(keys: List<OutfitRemoteKey>)

    @Query("DELETE FROM outfit_remote_keys WHERE filterKey = :filterKey")
    suspend fun clearByFilter(filterKey: String)
}
