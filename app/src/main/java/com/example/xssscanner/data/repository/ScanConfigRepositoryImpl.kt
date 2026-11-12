package com.example.xssscanner.data.repository

import com.example.xssscanner.data.local.SettingsDataStore
import com.example.xssscanner.domain.model.ScanConfig
import com.example.xssscanner.domain.repository.ScanConfigRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class ScanConfigRepositoryImpl @Inject constructor(
    private val dataStore: SettingsDataStore
) : ScanConfigRepository {

    override fun observeConfig(): Flow<ScanConfig> = dataStore.configFlow

    override suspend fun getConfig(): ScanConfig = dataStore.configFlow.first()

    override suspend fun saveConfig(config: ScanConfig) = dataStore.save(config)
}
