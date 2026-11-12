package com.example.xssscanner.data.local

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// TODO: needs confirmation — §1's file list for data/local does not include
// a DatabaseModule.kt, only AppDatabase/DAOs/entities/SettingsDataStore.
// Room needs a Hilt @Provides somewhere to build AppDatabase/DAOs; this is
// added as the minimal, conventional place for that (co-located with
// AppDatabase), rather than overloading RepositoryModule (which is
// specifically for binding domain/repository interfaces per §2).
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, "xss_scanner.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideScanResultDao(db: AppDatabase): ScanResultDao = db.scanResultDao()

    @Provides
    fun provideScanLogDao(db: AppDatabase): ScanLogDao = db.scanLogDao()
}
