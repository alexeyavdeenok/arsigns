package com.example.domain.api

import com.example.domain.model.SignEntity

interface ISignRepository {
    val cachedSigns: Map<Int, SignEntity>

    suspend fun preloadCache()

    suspend fun getSignById(id: Int): SignEntity?

    suspend fun getSignByYoloClassIndex(yoloClassIndex: String): SignEntity?

    suspend fun getAllSigns(): List<SignEntity>

    fun getCachedSignById(id: Int): SignEntity?
}
