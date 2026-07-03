package com.example.domain.repository

import com.example.domain.model.SignEntity

interface ISignRepository {
    suspend fun getSignById(id: Int): SignEntity?
    suspend fun preloadCache()
}
