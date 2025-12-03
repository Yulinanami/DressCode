package com.example.dresscode.data.repository

import com.example.dresscode.data.local.entity.OutfitEntity
import com.example.dresscode.model.Gender

object OutfitMockData {
    private val mockList = listOf(
        OutfitEntity(
            id = "look-1",
            filterKey = "",
            title = "夏日通勤",
            imageUrl = null,
            gender = Gender.FEMALE,
            style = "通勤",
            season = "夏季",
            scene = "通勤",
            weather = "晴",
            tags = listOf("夏季", "通勤", "简约"),
            isFavorite = false,
            page = 1,
            indexInPage = 0
        ),
        OutfitEntity(
            id = "look-2",
            filterKey = "",
            title = "周末休闲",
            imageUrl = null,
            gender = Gender.UNISEX,
            style = "休闲",
            season = "四季",
            scene = "出街",
            weather = "多云",
            tags = listOf("休闲", "牛仔"),
            isFavorite = false,
            page = 1,
            indexInPage = 1
        ),
        OutfitEntity(
            id = "look-3",
            filterKey = "",
            title = "运动风",
            imageUrl = null,
            gender = Gender.MALE,
            style = "运动",
            season = "夏季",
            scene = "运动",
            weather = "晴",
            tags = listOf("运动", "街头"),
            isFavorite = false,
            page = 1,
            indexInPage = 2
        ),
        OutfitEntity(
            id = "look-4",
            filterKey = "",
            title = "雨天通勤",
            imageUrl = null,
            gender = Gender.FEMALE,
            style = "通勤",
            season = "春季",
            scene = "办公室",
            weather = "雨天",
            tags = listOf("雨天", "通勤", "防水"),
            isFavorite = false,
            page = 1,
            indexInPage = 3
        ),
        OutfitEntity(
            id = "look-5",
            filterKey = "",
            title = "晚间约会",
            imageUrl = null,
            gender = Gender.FEMALE,
            style = "优雅",
            season = "夏季",
            scene = "约会",
            weather = "晴",
            tags = listOf("约会", "优雅", "夏季"),
            isFavorite = false,
            page = 1,
            indexInPage = 4
        )
    )

    fun entities(filterKey: String, favoriteIds: Set<String>): List<OutfitEntity> {
        return mockList.mapIndexed { index, entity ->
            entity.copy(
                filterKey = filterKey,
                indexInPage = index,
                isFavorite = favoriteIds.contains(entity.id)
            )
        }
    }
}
