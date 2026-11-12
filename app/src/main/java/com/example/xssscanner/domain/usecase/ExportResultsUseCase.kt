package com.example.xssscanner.domain.usecase

import com.example.xssscanner.data.repository.ExportRepository
import com.example.xssscanner.domain.model.ScanError
import javax.inject.Inject

enum class ExportFormat { CRAWL, ANALYSIS }

/**
 * TODO: needs confirmation — §8.3 types this use case's output as
 * List<Uri> (android.net.Uri), but §0.2 forbids domain importing Android
 * framework types. The simplest implementation that respects domain purity
 * is to return URI strings (Uri.toString()) here; ExportRepository (data
 * layer, which legitimately touches SAF/android.net.Uri) does the real
 * Uri work and the ui layer re-parses the string back into a Uri only where
 * it needs one (e.g. to launch a share Intent).
 */
class ExportResultsUseCase @Inject constructor(
    private val exportRepository: ExportRepository
) {
    sealed class Result {
        data class Success(val fileUris: List<String>) : Result()
        data class Error(val error: ScanError) : Result()
    }

    suspend operator fun invoke(scanSessionId: String, format: ExportFormat): Result {
        return try {
            val uris = when (format) {
                ExportFormat.CRAWL -> exportRepository.exportCrawl(scanSessionId)
                ExportFormat.ANALYSIS -> exportRepository.exportAnalysis(scanSessionId)
            }
            Result.Success(uris)
        } catch (e: Exception) {
            Result.Error(ScanError.ExportFailed(e.message ?: "export failed"))
        }
    }
}
