package com.example.dresscode.data.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.example.dresscode.data.local.db.DressCodeDatabase
import com.example.dresscode.data.local.entity.FavoriteEntity
import com.example.dresscode.data.local.entity.OutfitEntity
import com.example.dresscode.data.remote.OutfitApiService
import com.example.dresscode.data.remote.dto.OutfitDto
import com.example.dresscode.model.Gender
import com.example.dresscode.model.OutfitFilters
import com.example.dresscode.model.OutfitPreview
import com.example.dresscode.data.local.entity.SearchHistoryEntity
import com.example.dresscode.data.repository.OutfitFilterKey.build
import com.example.dresscode.data.repository.paging.OutfitRemoteMediator
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

class OutfitRepository @Inject constructor(
    private val api: OutfitApiService,
    private val database: DressCodeDatabase,
    private val userRepository: UserRepository
) {

    private val outfitDao = database.outfitDao()
    private val favoriteDao = database.favoriteDao()
    private val searchHistoryDao = database.searchHistoryDao()

    fun observeFavorites(): Flow<List<OutfitPreview>> {
        return favoriteDao.favorites()
            .map { list -> list.map { it.toPreview() } }
            .distinctUntilChanged()
    }

    fun recentSearches(limit: Int = 8): Flow<List<String>> =
        searchHistoryDao.recent(limit).map { items -> items.map { it.query } }

    @OptIn(ExperimentalPagingApi::class)
    fun pagedOutfits(
        query: String,
        filters: OutfitFilters,
        pageSize: Int = 20
    ): Flow<PagingData<OutfitPreview>> {
        val filterKey = build(query, filters)
        val mediator = OutfitRemoteMediator(
            filterKey = filterKey,
            query = query,
            filters = filters,
            service = api,
            database = database,
            favoriteResolver = { favoriteDao.favorites().first().map { it.outfitId }.toSet() }
        )
        return Pager(
            config = PagingConfig(pageSize = pageSize, enablePlaceholders = false),
            remoteMediator = mediator,
            pagingSourceFactory = { outfitDao.pagingSource(filterKey) }
        ).flow.map { pagingData ->
            pagingData.map { entity -> entity.toPreview() }
        }
    }

    suspend fun toggleFavorite(id: String): Result<Boolean> {
        val auth = userRepository.authState().first()
        if (!auth.isLoggedIn || auth.token.isNullOrBlank()) {
            return Result.failure(IllegalStateException("请登录后再收藏"))
        }
        val bearer = "Bearer ${auth.token}"
        val currentlyFavorite = favoriteDao.isFavorite(id)
        val targetFavorite = !currentlyFavorite
        return withContext(Dispatchers.IO) {
            runCatching {
                if (targetFavorite) {
                    api.addFavorite(id, bearer)
                } else {
                    api.removeFavorite(id, bearer)
                }
                val entity = loadOutfitSnapshot(id)
                if (targetFavorite) {
                    entity?.let { favoriteDao.upsert(it.toFavoriteEntity()) }
                } else {
                    favoriteDao.delete(id)
                }
                outfitDao.updateFavorite(id, targetFavorite)
                targetFavorite
            }.recoverCatching { error ->
                if (error is HttpException && error.code() == 401) {
                    userRepository.logout()
                    throw IllegalStateException("登录已过期，请重新登录")
                }
                throw error
            }
        }
    }

    suspend fun refreshFavoritesFromRemote(): Result<Unit> {
        val auth = userRepository.authState().first()
        val token = auth.token ?: return Result.failure(IllegalStateException("未登录"))
        return withContext(Dispatchers.IO) {
            runCatching {
                val bearer = "Bearer $token"
                val favorites = api.getFavorites(bearer)
                val entities = favorites.map { dto ->
                    dto.toFavoriteEntity()
                }
                favoriteDao.clearAll()
                outfitDao.clearFavoriteFlags()
                entities.forEach { favorite ->
                    favoriteDao.upsert(favorite)
                    outfitDao.updateFavorite(favorite.outfitId, true)
                }
            }.recoverCatching { error ->
                if (error is HttpException && error.code() == 401) {
                    userRepository.logout()
                    throw IllegalStateException("登录已过期，请重新登录")
                }
                throw error
            }
        }
    }

    suspend fun recordSearch(query: String) {
        val normalized = query.trim()
        if (normalized.isBlank()) return
        withContext(Dispatchers.IO) {
            searchHistoryDao.deleteByQuery(normalized)
            searchHistoryDao.insert(SearchHistoryEntity(query = normalized))
        }
    }

    private suspend fun loadOutfitSnapshot(id: String): OutfitEntity? {
        val cached = outfitDao.findByIds(listOf(id)).firstOrNull()
        if (cached != null) return cached
        val remote = runCatching { api.getOutfit(id) }.getOrNull() ?: return null
        return remote.toEntity(
            filterKey = build("", OutfitFilters()),
            page = 0,
            indexInPage = 0,
            isFavorite = true
        )
    }
}

private fun OutfitDto.toEntity(
    filterKey: String,
    page: Int,
    indexInPage: Int,
    isFavorite: Boolean
): OutfitEntity {
    return OutfitEntity(
        id = id,
        filterKey = filterKey,
        title = title,
        imageUrl = imageUrl,
        gender = gender?.let { runCatching { Gender.valueOf(it.uppercase()) }.getOrNull() },
        style = tags?.style?.firstOrNull(),
        season = tags?.season?.firstOrNull(),
        scene = tags?.scene?.firstOrNull(),
        weather = tags?.weather?.firstOrNull(),
        tags = collectTags(tags),
        isFavorite = isFavorite,
        page = page,
        indexInPage = indexInPage
    )
}

private fun OutfitDto.toFavoriteEntity(): FavoriteEntity {
    return FavoriteEntity(
        outfitId = id,
        title = title,
        imageUrl = imageUrl,
        gender = gender?.let { runCatching { Gender.valueOf(it.uppercase()) }.getOrNull() },
        tags = collectTags(tags)
    )
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

private fun OutfitEntity.toPreview(): OutfitPreview {
    return OutfitPreview(
        id = id,
        title = title,
        imageUrl = imageUrl,
        tags = tags,
        gender = gender ?: Gender.UNISEX,
        isFavorite = isFavorite
    )
}

private fun FavoriteEntity.toPreview(): OutfitPreview {
    return OutfitPreview(
        id = outfitId,
        title = title,
        imageUrl = imageUrl,
        tags = tags,
        gender = gender ?: Gender.UNISEX,
        isFavorite = true
    )
}

private fun OutfitEntity.toFavoriteEntity(): FavoriteEntity {
    return FavoriteEntity(
        outfitId = id,
        title = title,
        imageUrl = imageUrl,
        gender = gender,
        tags = tags
    )
}
