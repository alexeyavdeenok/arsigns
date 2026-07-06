package com.example.feature_cv.tracker

import com.example.feature_cv.inference.model.RawDetection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IouSignTrackerTest {

    private fun box(classId: Int, confidence: Float, xMin: Float, yMin: Float, xMax: Float, yMax: Float) =
        RawDetection(classId = classId, confidence = confidence, xMin = xMin, yMin = yMin, xMax = xMax, yMax = yMax)

    @Test
    fun `новая детекция без пары создаёт новый трек с новым id`() {
        val tracker = IouSignTracker()

        val result = tracker.update(listOf(box(classId = 1, confidence = 0.9f, xMin = 0.1f, yMin = 0.1f, xMax = 0.3f, yMax = 0.3f)))

        assertEquals(1, result.size)
        assertEquals(0, result.first().id) // первый трек всегда получает id=0
    }

    @Test
    fun `детекция совпала с треком по IoU - id сохраняется между кадрами`() {
        val tracker = IouSignTracker()

        val frame1 = tracker.update(listOf(box(classId = 1, confidence = 0.9f, xMin = 0.1f, yMin = 0.1f, xMax = 0.3f, yMax = 0.3f)))
        val idAfterFrame1 = frame1.first().id

        // Почти та же рамка на следующем кадре — небольшое смещение, как от реального движения
        val frame2 = tracker.update(listOf(box(classId = 1, confidence = 0.85f, xMin = 0.11f, yMin = 0.1f, xMax = 0.31f, yMax = 0.3f)))

        assertEquals(1, frame2.size)
        assertEquals("id должен сохраниться между кадрами при высоком IoU", idAfterFrame1, frame2.first().id)
    }

    @Test
    fun `трек не нашёл пару N кадров подряд - удаляется после исчерпания жизней`() {
        val tracker = IouSignTracker(maxLives = 3)

        tracker.update(listOf(box(classId = 1, confidence = 0.9f, xMin = 0.1f, yMin = 0.1f, xMax = 0.3f, yMax = 0.3f)))

        // 3 кадра подряд без детекций — трек должен потерять все жизни и исчезнуть
        tracker.update(emptyList())
        tracker.update(emptyList())
        val lastFrame = tracker.update(emptyList())

        assertTrue("трек должен быть удалён после исчерпания жизней", lastFrame.isEmpty())
    }

    @Test
    fun `трек не нашёл пару 1 кадр, но снова совпал - не потерян`() {
        val tracker = IouSignTracker(maxLives = 3)

        val frame1 = tracker.update(listOf(box(classId = 1, confidence = 0.9f, xMin = 0.1f, yMin = 0.1f, xMax = 0.3f, yMax = 0.3f)))
        val originalId = frame1.first().id

        tracker.update(emptyList()) // 1 кадр без детекции — жизнь потрачена, но трек жив

        val frame3 = tracker.update(listOf(box(classId = 1, confidence = 0.9f, xMin = 0.1f, yMin = 0.1f, xMax = 0.3f, yMax = 0.3f)))

        assertEquals(1, frame3.size)
        assertEquals("трек должен пережить один пропущенный кадр и сохранить id", originalId, frame3.first().id)
    }

    @Test
    fun `две детекции почти не пересекаются - не матчатся, оба трека существуют`() {
        val tracker = IouSignTracker()

        tracker.update(
            listOf(
                box(classId = 1, confidence = 0.9f, xMin = 0.0f, yMin = 0.0f, xMax = 0.1f, yMax = 0.1f),
                box(classId = 1, confidence = 0.8f, xMin = 0.8f, yMin = 0.8f, xMax = 0.9f, yMax = 0.9f)
            )
        )

        val result = tracker.update(
            listOf(
                box(classId = 1, confidence = 0.9f, xMin = 0.0f, yMin = 0.0f, xMax = 0.1f, yMax = 0.1f),
                box(classId = 1, confidence = 0.8f, xMin = 0.8f, yMin = 0.8f, xMax = 0.9f, yMax = 0.9f)
            )
        )

        assertEquals("два далёких друг от друга знака должны остаться отдельными треками", 2, result.size)
        assertEquals(2, result.map { it.id }.distinct().size)
    }

    @Test
    fun `одинаковые координаты, но разные classId - не матчатся между собой`() {
        // maxLives=1: старый трек класса 1 гарантированно угаснет за один пропущенный кадр,
        // иначе (при дефолтном maxLives=5) он бы остался жить ещё несколько кадров по TTL
        // ОДНОВРЕМЕННО с новым треком класса 2 — это не баг трекера, а его штатное поведение
        // ("не мигать при кратковременной потере"), просто оно не то, что здесь хотим проверить.
        val tracker = IouSignTracker(maxLives = 1)

        val frame1 = tracker.update(listOf(box(classId = 1, confidence = 0.9f, xMin = 0.1f, yMin = 0.1f, xMax = 0.3f, yMax = 0.3f)))
        val idClass1 = frame1.first().id

        // Тот же бокс, но другой класс — это ДРУГОЙ физический знак (или ошибка классификации),
        // трекер не должен считать это тем же треком просто из-за совпадения координат
        val frame2 = tracker.update(listOf(box(classId = 2, confidence = 0.9f, xMin = 0.1f, yMin = 0.1f, xMax = 0.3f, yMax = 0.3f)))

        assertEquals(1, frame2.size)
        assertTrue("смена класса при том же боксе должна создать новый трек, не переиспользовать старый id", frame2.first().id != idClass1)
    }

    @Test
    fun `трек с истёкшими жизнями и новая детекция того же класса на том же месте - получает новый id`() {
        val tracker = IouSignTracker(maxLives = 1)

        val frame1 = tracker.update(listOf(box(classId = 1, confidence = 0.9f, xMin = 0.1f, yMin = 0.1f, xMax = 0.3f, yMax = 0.3f)))
        val originalId = frame1.first().id

        tracker.update(emptyList()) // maxLives=1 -> трек умирает сразу после первого пропуска

        val frame3 = tracker.update(listOf(box(classId = 1, confidence = 0.9f, xMin = 0.1f, yMin = 0.1f, xMax = 0.3f, yMax = 0.3f)))

        assertEquals(1, frame3.size)
        assertTrue("после смерти трека новая детекция должна получить новый id, а не переиспользовать старый", frame3.first().id != originalId)
    }
}