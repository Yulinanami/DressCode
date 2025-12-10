package com.example.dresscode.data.repository.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.example.dresscode.data.local.db.DressCodeDatabase
import com.example.dresscode.data.local.entity.OutfitEntity
import com.example.dresscode.data.local.entity.OutfitRemoteKey
import com.example.dresscode.data.repository.OutfitMockData
import com.example.dresscode.data.remote.OutfitApiService
import com.example.dresscode.model.OutfitFilters
import com.example.dresscode.data.remote.dto.OutfitDto
import java.io.IOException
import retrofit2.HttpException

@OptIn(ExperimentalPagingApi::class)
class OutfitRemoteMediator(
    private val filterKey: String,
    private val query: String,
    private val filters: OutfitFilters,
    private val service: OutfitApiService,
    private val database: DressCodeDatabase,
    private val favoriteResolver: suspend () -> Set<String>
) : RemoteMediator<Int, OutfitEntity>() {

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, OutfitEntity>
    ): MediatorResult {
        val page = when (loadType) {
            LoadType.REFRESH -> 1
            LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
            LoadType.APPEND -> {
                val lastItem = state.lastItemOrNull()
                if (lastItem == null) return MediatorResult.Success(endOfPaginationReached = true)
                val key = database.remoteKeyDao().remoteKey(filterKey, lastItem.id)
                key?.nextKey ?: return MediatorResult.Success(endOfPaginationReached = true)
            }
        }

        return try {
            val favorites = favoriteResolver.invoke()
            val response = service.getOutfits(
                page = page,
                pageSize = state.config.pageSize,
                gender = filters.gender?.name?.lowercase(),
                style = filters.style,
                season = filters.season,
                scene = filters.scene,
                weather = filters.weather,
                tags = filters.tags.takeIf { it.isNotEmpty() }?.joinToString(","),
                query = query.takeIf { it.isNotBlank() }
            )
            val items = response.items
            val endOfPaginationReached = items.isEmpty() ||
                (response.pageSize != null && items.size < response.pageSize)

            database.withTransaction {
                if (loadType == LoadType.REFRESH) {
                    database.remoteKeyDao().clearByFilter(filterKey)
                    database.outfitDao().clearByFilter(filterKey)
                }
                val prevKey = if (page == 1) null else page - 1
                val nextKey = if (endOfPaginationReached) null else page + 1
                val entities = items.mapIndexed { index, dto ->
                    val userUploadFlag = dto.isUserUploadFlag()
                    dto.toEntity(
                        filterKey = filterKey,
                        page = page,
                        indexInPage = index,
                        isFavorite = favorites.contains(dto.id)
                            || (dto.isFavorite == true)
                        ,
                        isUserUpload = userUploadFlag
                    )
                }
                val keys = entities.map { entity ->
                    OutfitRemoteKey(
                        id = entity.id,
                        filterKey = filterKey,
                        prevKey = prevKey,
                        nextKey = nextKey
                    )
                }
                database.remoteKeyDao().insertAll(keys)
                database.outfitDao().insertAll(entities)
            }
            MediatorResult.Success(endOfPaginationReached = endOfPaginationReached)
        } catch (e: Exception) {
            val fallbackAvailable = loadType == LoadType.REFRESH &&
                (e is IOException || (e is HttpException && e.code() == 404))
            if (fallbackAvailable) {
                val favorites = favoriteResolver.invoke()
                val mockEntities = OutfitMockData.entities(filterKey, favorites)
                database.withTransaction {
                    database.remoteKeyDao().clearByFilter(filterKey)
                    database.outfitDao().clearByFilter(filterKey)
                    val keys = mockEntities.map { entity ->
                        OutfitRemoteKey(
                            id = entity.id,
                            filterKey = filterKey,
                            prevKey = null,
                            nextKey = null
                        )
                    }
                    database.remoteKeyDao().insertAll(keys)
                    database.outfitDao().insertAll(mockEntities)
                }
                return MediatorResult.Success(endOfPaginationReached = true)
            }
            MediatorResult.Error(e)
        }
    }
}

private fun OutfitDto.toEntity(
    filterKey: String,
    page: Int,
    indexInPage: Int,
    isFavorite: Boolean,
    isUserUpload: Boolean
): OutfitEntity {
    return OutfitEntity(
        id = id,
        filterKey = filterKey,
        title = title,
        imageUrl = imageUrl ?: images?.firstOrNull(),
        gender = gender?.let { runCatching { com.example.dresscode.model.Gender.valueOf(it.uppercase()) }.getOrNull() },
        style = tags?.style?.firstOrNull(),
        season = tags?.season?.firstOrNull(),
        scene = tags?.scene?.firstOrNull(),
        weather = tags?.weather?.firstOrNull(),
        tags = collectTags(tags),
        isFavorite = isFavorite,
        isUserUpload = isUserUpload,
        page = page,
        indexInPage = indexInPage
    )
}

private fun OutfitDto.isUserUploadFlag(): Boolean {
    if (isUserUpload == true) return true
    val urlCandidates = buildList {
        imageUrl?.let { add(it) }
        imageUrlLegacy?.let { add(it) }
        images?.let { addAll(it) }
    }
    return urlCandidates.any { it.contains("/user_uploads/", ignoreCase = true) }
}

private fun collectTags(tags: com.example.dresscode.data.remote.dto.OutfitTagsDto?): List<String> {
    if (tags == null) return emptyList()
    return buildList {
        addAll(tags.general.orEmpty())
        addAll(tags.style.orEmpty())
        addAll(tags.scene.orEmpty())
        addAll(tags.weather.orEmpty())
        addAll(tags.season.orEmpty())
    }.filter { it.isNotBlank() }
}
