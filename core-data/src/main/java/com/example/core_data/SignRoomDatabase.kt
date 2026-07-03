package com.example.core_data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [SignRow::class], version = 1, exportSchema = false)
abstract class SignRoomDatabase : RoomDatabase() {
    abstract fun signDao(): SignDao

    companion object {
        private const val DATABASE_NAME = "road_signs.sqlite"

        fun create(context: Context): SignRoomDatabase {
            return Room.databaseBuilder(context.applicationContext, SignRoomDatabase::class.java, DATABASE_NAME)
                .createFromAsset("road_signs.sqlite")
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
