package com.example.dresscode.data.remote

import com.example.dresscode.data.remote.dto.OutfitDto
import com.example.dresscode.data.remote.dto.PagedResponseDto
import com.example.dresscode.data.remote.dto.ToggleFavoriteResponse
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface OutfitApiService {

    @GET("outfits")
    suspend fun getOutfits(
        @Query("page") page: Int,
        @Query("size") pageSize: Int,
        @Query("gender") gender: String? = null,
        @Query("style") style: String? = null,
        @Query("season") season: String? = null,
        @Query("scene") scene: String? = null,
        @Query("weather") weather: String? = null,
        @Query("tags") tags: String? = null,
        @Query("q") query: String? = null
    ): PagedResponseDto<OutfitDto>

    @GET("outfits/{id}")
    suspend fun getOutfit(
        @Path("id") id: String
    ): OutfitDto

    @GET("favorites")
    suspend fun getFavorites(
        @Header("Authorization") token: String?
    ): List<OutfitDto>

    @POST("favorites/{id}")
    suspend fun addFavorite(
        @Path("id") id: String,
        @Header("Authorization") token: String?
    ): ToggleFavoriteResponse

    @DELETE("favorites/{id}")
    suspend fun removeFavorite(
        @Path("id") id: String,
        @Header("Authorization") token: String?
    ): ToggleFavoriteResponse
}
