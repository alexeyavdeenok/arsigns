package com.example.core_data.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SignDao {
    @Query("SELECT * FROM signs")
    suspend fun getAll(): List<SignEntityDb>

    @Query("SELECT * FROM signs WHERE Original_Category_ID = :id LIMIT 1")
    suspend fun getById(id: String): SignEntityDb?

    @Query("SELECT * FROM signs WHERE GOST_Sign_Number = :index LIMIT 1")
    suspend fun getByYoloClassIndex(index: String): SignEntityDb?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(signs: List<SignEntityDb>)
}
