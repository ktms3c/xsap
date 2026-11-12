package com.example.xssscanner.domain.repository

import com.example.xssscanner.domain.model.ScanConfig
import kotlinx.coroutines.flow.Flow

interface ScanConfigRepository {
    fun observeConfig(): Flow<ScanConfig>
    suspend fun getConfig(): ScanConfig
    suspend fun saveConfig(config: ScanConfig)
}
