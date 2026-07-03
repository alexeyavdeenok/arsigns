package com.example.core_data

import androidx.room.Dao
import androidx.room.Query

@Dao
interface SignDao {
    @Query("SELECT * FROM signs ORDER BY Original_Category_ID ASC")
    suspend fun getAll(): List<SignRow>

    @Query("SELECT * FROM signs WHERE Original_Category_ID = :id LIMIT 1")
    suspend fun getById(id: String): SignRow?

    @Query("SELECT * FROM signs WHERE Original_Category_ID = :id LIMIT 1")
    suspend fun getByYoloClassIndex(id: String): SignRow?
}
