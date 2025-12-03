package com.example.dresscode.data.repository

import com.example.dresscode.model.Gender
import com.example.dresscode.model.OutfitFilters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OutfitFilterKeyTest {

    @Test
    fun `build key sorts tags and normalizes`() {
        val filters = OutfitFilters(
            gender = Gender.FEMALE,
            style = "休闲",
            tags = setOf("雨天", "通勤")
        )
        val key = OutfitFilterKey.build("  夏日 ", filters)
        assertTrue(key.startsWith("FEMALE"))
        assertTrue(key.contains("休闲"))
        assertTrue(key.contains("雨天,通勤") || key.contains("通勤,雨天"))
        assertTrue(key.endsWith("夏日"))
    }

    @Test
    fun `empty filters fallback to defaults`() {
        val key = OutfitFilterKey.build("", OutfitFilters())
        assertEquals("any||||||", key)
    }
}
