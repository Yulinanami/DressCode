package com.example.dresscode.data.local.db

import com.example.dresscode.model.Gender
import org.junit.Assert.assertEquals
import org.junit.Test

class DbConvertersTest {

    private val converters = DbConverters()

    @Test
    fun `string list round trip`() {
        val list = listOf("夏季", "通勤", "防水")
        val encoded = converters.toStringList(list)
        val decoded = converters.fromStringList(encoded)
        assertEquals(list, decoded)
    }

    @Test
    fun `gender conversion round trip`() {
        val gender = Gender.MALE
        val encoded = converters.genderToString(gender)
        val decoded = converters.genderFromString(encoded)
        assertEquals(gender, decoded)
    }
}
