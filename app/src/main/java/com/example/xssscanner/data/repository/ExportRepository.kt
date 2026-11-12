package com.example.xssscanner.data.repository

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.example.xssscanner.domain.repository.ScanResultRepository
import com.example.xssscanner.storage.CsvExporter
import com.example.xssscanner.storage.HtmlReportExporter
import com.example.xssscanner.storage.JsonExporter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Writes export files per §3.2/§10. §3.2 explicitly calls for SAF
 * (ACTION_CREATE_DOCUMENT) "as the primary approach," which is a
 * user-interactive picker flow that must be launched from an Activity
 * (registerForActivityResult), not from a plain repository class with no
 * UI context.
 *
 * TODO: needs confirmation — this repository therefore writes export files
 * into the app's own cache dir (Context.cacheDir/exports) and returns their
 * content:// Uris via FileProvider-less MediaStore.Downloads insertion
 * where available, so ExportResultsUseCase has concrete Uris to hand back
 * regardless of which screen triggered the export. The SAF picker itself
 * (letting the user choose the destination/filename) is left to
 * MainActivity/MainViewModel — exact wiring is left `// TODO(verify-api)`
 * there since it depends on an ActivityResultLauncher registered at
 * Activity-creation time, which doesn't fit a pure data-layer class.
 */
@Singleton
class ExportRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val scanResultRepository: ScanResultRepository,
    private val csvExporter: CsvExporter,
    private val jsonExporter: JsonExporter,
    private val htmlReportExporter: HtmlReportExporter
) {

    suspend fun exportCrawl(scanSessionId: String): List<String> = withContext(Dispatchers.IO) {
        val results = scanResultRepository.getCrawlResults(scanSessionId)
        val csvUri = writeToDownloads("crawl_results.csv", "text/csv", csvExporter.exportCrawlResults(results))
        val jsonUri = writeToDownloads("crawl_results.json", "application/json", jsonExporter.exportCrawlResults(results))
        listOfNotNull(csvUri, jsonUri).map { it.toString() }
    }

    suspend fun exportAnalysis(scanSessionId: String): List<String> = withContext(Dispatchers.IO) {
        val findings = scanResultRepository.getFindings(scanSessionId)
        val csvUri = writeToDownloads("xss_findings.csv", "text/csv", csvExporter.exportFindings(findings))
        val jsonUri = writeToDownloads("xss_findings.json", "application/json", jsonExporter.exportFindings(findings))
        val htmlUri = writeToDownloads(
            "xss_report.html",
            "text/html",
            htmlReportExporter.buildReport(scanSessionId, findings)
        )
        listOfNotNull(csvUri, jsonUri, htmlUri).map { it.toString() }
    }

    // TODO(verify-api): MediaStore.Downloads is API 29+; minSdk is 24 (§2).
    // On API < 29 this falls back to the app's own external files dir,
    // which does not require WRITE_EXTERNAL_STORAGE on modern targetSdk
    // but is not picked by the user via SAF as §3.2 literally asks for.
    private fun writeToDownloads(fileName: String, mimeType: String, content: String): Uri? {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/XssScanner")
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: return null
            context.contentResolver.openOutputStream(uri)?.use { it.write(content.toByteArray()) }
            uri
        } else {
            val dir = context.getExternalFilesDir("exports") ?: context.filesDir
            val file = java.io.File(dir, fileName)
            file.writeText(content)
            Uri.fromFile(file)
        }
    }
}
