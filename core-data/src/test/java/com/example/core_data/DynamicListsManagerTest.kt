package com.example.core_data

import com.example.domain.model.ActiveSign
import com.example.domain.model.SignEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class DynamicListsManagerTest {

    @Test
    fun recordRecognizedSign_deduplicatesConsecutiveDuplicatesAndKeepsLast50() = runBlocking {
        val manager = DynamicListsManagerImpl(maxHistorySize = 3)
        val first = SignEntity(id = 1, pddCode = "1", title = "A", ttsTitle = "A", description = "", svgPath = "")
        val second = SignEntity(id = 2, pddCode = "2", title = "B", ttsTitle = "B", description = "", svgPath = "")
        val duplicate = SignEntity(id = 1, pddCode = "1", title = "A", ttsTitle = "A", description = "", svgPath = "")
        val third = SignEntity(id = 3, pddCode = "3", title = "C", ttsTitle = "C", description = "", svgPath = "")

        manager.recordRecognizedSign(first)
        manager.recordRecognizedSign(first)
        manager.recordRecognizedSign(second)
        manager.recordRecognizedSign(duplicate)
        manager.recordRecognizedSign(third)

        assertEquals(listOf(3, 1, 2), manager.historySigns.value.map { it.id })
    }

    @Test
    fun updateActiveSigns_emitsProvidedList() = runBlocking {
        val manager = DynamicListsManagerImpl()
        val signs = listOf(ActiveSign(1, 0.82f, 0.1f, 0.2f, 0.3f, 0.4f))

        manager.updateActiveSigns(signs)

        assertEquals(signs, manager.activeSigns.value)
    }
}
