package com.example.feature_cv.fake

import com.example.domain.api.CvLayerApi
import com.example.domain.model.DetectedSign
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Заглушка CvLayerApi для параллельной разработки :app, пока реальный CV-пайплайн не готов.
 * Генерирует +- реалистичный поток DetectedSign с частотой ~30 раз/сек:
 * - координаты нормализованы в [0..1] (тот же контракт, что и у реальной реализации)
 * - у каждого "знака" стабильный id, который живёт МИНИМУМ 5 кадров подряд (имитация трекера)
 * - знаки плавно двигаются по кадру (имитация приближения/движения камеры), а не телепортируются
 * - confidence слегка дрожит от кадра к кадру, а не фиксированное число
 * - знаки со временем пропадают и на их место появляются новые — как в реальной сцене
 *
 * Использование: подменить биндинг CvLayerApi на FakeCvLayerApiImpl в Hilt-модуле
 * (например через отдельный debug-вариант CvModule) — :app и весь код выше по потоку
 * (DynamicListsManagerImpl и т.д.) не заметят разницы, т.к. работают только с контрактом.
 */

/**
class FakeCvLayerApiImpl(
    private val maxConcurrentSigns: Int = 4,
    private val tickIntervalMs: Long = 33L, // ~30 FPS
    private val possibleClassIds: List<Int> = listOf(0, 3, 12, 41, 87) // тестовые classId — под реальные argmax-индексы твоей модели
) : CvLayerApi {

    private val _liveDetectedSigns = MutableStateFlow<List<DetectedSign>>(emptyList())
    override val liveDetectedSigns: StateFlow<List<DetectedSign>> = _liveDetectedSigns.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var loopJob: Job? = null

    private val activeTracks = mutableListOf<SimTrack>()
    private var nextId = 0

    override fun startDetection() {
        if (loopJob?.isActive == true) return // уже запущено, повторный вызов ничего не делает

        loopJob = scope.launch {
            while (true) {
                tick()
                delay(tickIntervalMs)
            }
        }
    }

    override fun stopDetection() {
        // Отменяем цикл, но НЕ чистим _liveDetectedSigns — так же ведёт себя и реальная
        // реализация (Preview продолжал бы жить, а анализ просто на паузе).
        loopJob?.cancel()
        loopJob = null
    }

    private fun tick() {
        moveExistingTracks()
        removeExpiredTracks()
        if (activeTracks.isEmpty()) {
            // Гарантированный спавн, если сцена пустая (в т.ч. самый первый тик после старта) —
            // без этого в коротких тестах/на старте экран мог бы случайно остаться пустым
            // из-за вероятностного maybeSpawnNewTrack().
            repeat(minOf(2, maxConcurrentSigns)) { spawnNewTrack() }
        } else {
            maybeSpawnNewTrack()
        }

        _liveDetectedSigns.value = activeTracks.map { track ->
            DetectedSign(
                id = track.id,
                confidence = jitterConfidence(track.baseConfidence),
                xMin = track.xMin,
                yMin = track.yMin,
                xMax = track.xMax,
                yMax = track.yMax,
                classId = track.classId
            )
        }
    }

    private fun moveExistingTracks() {
        activeTracks.forEach { track ->
            track.xMin = (track.xMin + track.velocityX).coerceIn(0f, 1f - track.width)
            track.xMax = track.xMin + track.width
            track.yMin = (track.yMin + track.velocityY).coerceIn(0f, 1f - track.height)
            track.yMax = track.yMin + track.height
            track.framesLeft -= 1
        }
    }

    private fun removeExpiredTracks() {
        activeTracks.removeAll { it.framesLeft <= 0 }
    }

    private fun maybeSpawnNewTrack() {
        if (activeTracks.size >= maxConcurrentSigns) return
        // ~15% шанс спавна нового знака за тик — не все слоты сразу забиваются
        if (Random.nextFloat() > 0.15f) return
        spawnNewTrack()
    }

    private fun spawnNewTrack() {
        if (activeTracks.size >= maxConcurrentSigns) return

        val width = Random.nextFloat() * 0.15f + 0.08f  // 0.08..0.23 от ширины кадра
        val height = width * (0.9f + Random.nextFloat() * 0.3f) // знаки почти квадратные/чуть вытянутые

        val xMin = Random.nextFloat() * (1f - width)
        val yMin = Random.nextFloat() * (1f - height)

        activeTracks += SimTrack(
            id = nextId++,
            classId = possibleClassIds.random(),
            xMin = xMin,
            yMin = yMin,
            width = width,
            height = height,
            velocityX = (Random.nextFloat() - 0.5f) * 0.01f, // лёгкий дрейф, не телепортация
            velocityY = (Random.nextFloat() - 0.5f) * 0.006f,
            baseConfidence = 0.55f + Random.nextFloat() * 0.4f, // 0.55..0.95
            framesLeft = Random.nextInt(5, 60) // минимум 5 кадров жизни, как и просили
        )
    }

    private fun jitterConfidence(base: Float): Float =
        (base + (Random.nextFloat() - 0.5f) * 0.05f).coerceIn(0.05f, 0.99f)

    private class SimTrack(
        val id: Int,
        val classId: Int,
        var xMin: Float,
        var yMin: Float,
        val width: Float,
        val height: Float,
        val velocityX: Float,
        val velocityY: Float,
        val baseConfidence: Float,
        var framesLeft: Int
    ) {
        var xMax: Float = xMin + width
        var yMax: Float = yMin + height
    }
}
*/