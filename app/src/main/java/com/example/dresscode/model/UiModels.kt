package com.example.dresscode.model

data class WeatherUiState(
    val title: String = "天气",
    val city: String = "未定位",
    val summary: String = "等待天气数据",
    val temperature: String = "--°"
)

data class OutfitUiState(
    val title: String = "穿搭",
    val highlight: String = "探索穿搭灵感",
    val filters: String = "默认筛选：全部"
)

data class TryOnUiState(
    val title: String = "智能换装",
    val status: String = "暂未提交任务",
    val hint: String = "上传人像并选择收藏穿搭"
)

data class ProfileUiState(
    val title: String = "我的",
    val subtitle: String = "未登录",
    val notes: String = "设置性别与默认筛选"
)

data class OutfitPreview(
    val id: String,
    val title: String,
    val tags: List<String>
)
