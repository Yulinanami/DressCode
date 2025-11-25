package com.example.dresscode.data.repository

import com.example.dresscode.model.OutfitPreview
import com.example.dresscode.model.OutfitUiState
import com.example.dresscode.model.ProfileUiState
import com.example.dresscode.model.TryOnUiState
import com.example.dresscode.model.WeatherUiState

class WeatherRepository {
    fun snapshot(): WeatherUiState = WeatherUiState(
        city = "上海",
        summary = "晴·微风",
        temperature = "26°"
    )
}

class OutfitRepository {
    private val featured = listOf(
        OutfitPreview(id = "look-1", title = "夏日通勤", tags = listOf("夏季", "通勤", "简约")),
        OutfitPreview(id = "look-2", title = "周末休闲", tags = listOf("休闲", "牛仔")),
        OutfitPreview(id = "look-3", title = "运动风", tags = listOf("运动", "街头"))
    )

    fun featured(): List<OutfitPreview> = featured

    fun snapshot(): OutfitUiState = OutfitUiState(
        highlight = "精选穿搭 ${featured.size} 套",
        filters = "默认筛选：全部"
    )
}

class TryOnRepository {
    fun snapshot(): TryOnUiState = TryOnUiState(
        status = "尚未提交换装任务",
        hint = "上传人像并选择收藏穿搭"
    )
}

class UserRepository {
    fun profile(): ProfileUiState = ProfileUiState(
        subtitle = "游客模式",
        notes = "登录后可同步收藏与设置"
    )
}
