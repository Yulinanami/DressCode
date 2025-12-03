package com.example.dresscode.data.local.db

import androidx.room.TypeConverter
import com.example.dresscode.model.Gender

class DbConverters {

    @TypeConverter
    fun fromStringList(value: String?): List<String> {
        if (value.isNullOrBlank()) return emptyList()
        return value.split("||").mapNotNull { it.takeIf { item -> item.isNotBlank() } }
    }

    @TypeConverter
    fun toStringList(list: List<String>?): String? {
        if (list.isNullOrEmpty()) return null
        return list.joinToString("||")
    }

    @TypeConverter
    fun genderFromString(value: String?): Gender? {
        return value?.let { runCatching { Gender.valueOf(it) }.getOrNull() }
    }

    @TypeConverter
    fun genderToString(gender: Gender?): String? = gender?.name
}
