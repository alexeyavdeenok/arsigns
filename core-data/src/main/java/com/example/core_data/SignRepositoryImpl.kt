package com.example.core_data

import com.example.domain.api.ISignRepository
import com.example.domain.model.SignEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SignRepositoryImpl @Inject constructor(
    private val signDao: SignDao
) : ISignRepository {

    private var _cachedSigns: Map<Int, SignEntity> = emptyMap()

    override val cachedSigns: Map<Int, SignEntity>
        get() = _cachedSigns

    override suspend fun preloadCache() = withContext(Dispatchers.IO) {
        _cachedSigns = signDao.getAll()
            .map { it.toDomain() }
            .associateBy { it.id }
    }

    override suspend fun getSignById(id: Int): SignEntity? = withContext(Dispatchers.IO) {
        signDao.getById(id.toString())?.toDomain() ?: _cachedSigns[id]
    }

    override suspend fun getSignByYoloClassIndex(yoloClassIndex: String): SignEntity? = withContext(Dispatchers.IO) {
        signDao.getByYoloClassIndex(yoloClassIndex)?.toDomain()
            ?: _cachedSigns.values.firstOrNull { it.pddCode == yoloClassIndex }
    }

    override suspend fun getAllSigns(): List<SignEntity> = withContext(Dispatchers.IO) {
        signDao.getAll().map { it.toDomain() }
    }

    override fun getCachedSignById(id: Int): SignEntity? = _cachedSigns[id]
}
