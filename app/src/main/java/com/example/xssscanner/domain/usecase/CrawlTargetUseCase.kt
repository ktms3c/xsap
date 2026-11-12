package com.example.xssscanner.domain.usecase

import com.example.xssscanner.domain.model.CrawlResult
import com.example.xssscanner.domain.model.ScanConfig
import com.example.xssscanner.scanner.crawler.CrawlEngine
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class CrawlTargetUseCase @Inject constructor(
    private val crawlEngine: CrawlEngine
) {
    operator fun invoke(config: ScanConfig): Flow<CrawlResult> {
        return crawlEngine.crawl(config)
    }
}
