package com.example.core_data.di

import android.content.Context
import androidx.room.Room
import com.example.core_data.room.SignDao
import com.example.core_data.room.SignRoomDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideSignRoomDatabase(@ApplicationContext context: Context): SignRoomDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            SignRoomDatabase::class.java,
            "road_signs.sqlite"
        )
            .createFromAsset("road_signs.sqlite")
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideSignDao(database: SignRoomDatabase): SignDao = database.signDao()
}
