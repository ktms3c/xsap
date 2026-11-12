package com.example.xssscanner.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.xssscanner.data.local.entity.CrawlResultEntity
import com.example.xssscanner.data.local.entity.ScanLogEntity
import com.example.xssscanner.data.local.entity.XssFindingEntity

@Database(
    entities = [CrawlResultEntity::class, XssFindingEntity::class, ScanLogEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scanResultDao(): ScanResultDao
    abstract fun scanLogDao(): ScanLogDao
}
