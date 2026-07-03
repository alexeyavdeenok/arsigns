package com.example.core_data.repository

import com.example.core_data.mapper.SignMapper.toDomain
import com.example.core_data.room.SignDao
import com.example.domain.model.SignEntity
import com.example.domain.repository.ISignRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SignRepositoryImpl @Inject constructor(
    private val signDao: SignDao
) : ISignRepository {

    private var _cachedSigns: Map<Int, SignEntity> = emptyMap()

    override suspend fun preloadCache() = withContext(Dispatchers.IO) {
        _cachedSigns = signDao.getAll()
            .map { it.toDomain() }
            .associateBy { it.id }
    }

    override suspend fun getSignById(id: Int): SignEntity? = withContext(Dispatchers.IO) {
        signDao.getById(id.toString())?.toDomain() ?: _cachedSigns[id]
    }
}
