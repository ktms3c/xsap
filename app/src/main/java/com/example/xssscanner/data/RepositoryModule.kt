package com.example.xssscanner.data

import com.example.xssscanner.data.repository.ScanConfigRepositoryImpl
import com.example.xssscanner.data.repository.ScanResultRepositoryImpl
import com.example.xssscanner.data.remote.HttpClientService
import com.example.xssscanner.domain.repository.HttpClientPort
import com.example.xssscanner.domain.repository.ScanConfigRepository
import com.example.xssscanner.domain.repository.ScanResultRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Per §2: "Every interface in domain/repository is bound to its data
 * implementation via @Binds in a dedicated @Module." Lives under data/ per
 * that same instruction.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindScanConfigRepository(impl: ScanConfigRepositoryImpl): ScanConfigRepository

    @Binds
    @Singleton
    abstract fun bindScanResultRepository(impl: ScanResultRepositoryImpl): ScanResultRepository

    @Binds
    @Singleton
    abstract fun bindHttpClientPort(impl: HttpClientService): HttpClientPort
}
