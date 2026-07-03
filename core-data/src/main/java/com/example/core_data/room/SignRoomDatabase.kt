package com.example.core_data.room

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [SignEntityDb::class], version = 1, exportSchema = false)
abstract class SignRoomDatabase : RoomDatabase() {
    abstract fun signDao(): SignDao
}
