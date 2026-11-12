package com.example.xssscanner.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.xssscanner.domain.model.ScanConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "settings")

/**
 * Backs ScanConfigRepositoryImpl. Every field in §7's settings table has a
 * dedicated key here so there is exactly one serialization format.
 * targetUrl is NOT persisted (it's entered fresh on MainActivity each time),
 * so it is not part of these keys; ScanConfigRepositoryImpl fills it in from
 * the current MainUiState.targetUrl when assembling a full ScanConfig.
 */
@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val ENABLE_JS_LINK_FINDER = booleanPreferencesKey("enable_js_link_finder")
        val CRAWL_SITEMAP = booleanPreferencesKey("crawl_sitemap")
        val CRAWL_ROBOTS = booleanPreferencesKey("crawl_robots")
        val DISABLE_REDIRECT = booleanPreferencesKey("disable_redirect")
        val LOCAL_LAB_MODE = booleanPreferencesKey("local_lab_mode")
        val THREADS = intPreferencesKey("threads")
        val TIMEOUT_SECONDS = intPreferencesKey("timeout_seconds")
        val CRAWL_DEPTH = intPreferencesKey("crawl_depth")
        val USER_AGENT = stringPreferencesKey("user_agent")
        val BLACKLIST_URL_REGEX = stringPreferencesKey("blacklist_url_regex")
        val WHITELIST_URL_REGEX = stringPreferencesKey("whitelist_url_regex")
        val WHITELIST_DOMAIN = stringPreferencesKey("whitelist_domain")
        val COOKIE = stringPreferencesKey("cookie")
        val CUSTOM_HEADERS = stringPreferencesKey("custom_headers")
    }

    val configFlow: Flow<ScanConfig> = context.dataStore.data.map { prefs ->
        ScanConfig(
            targetUrl = "",
            enableJsLinkFinder = prefs[Keys.ENABLE_JS_LINK_FINDER] ?: true,
            crawlSitemap = prefs[Keys.CRAWL_SITEMAP] ?: true,
            crawlRobots = prefs[Keys.CRAWL_ROBOTS] ?: true,
            disableRedirect = prefs[Keys.DISABLE_REDIRECT] ?: false,
            localLabMode = prefs[Keys.LOCAL_LAB_MODE] ?: false,
            threads = prefs[Keys.THREADS] ?: 4,
            timeoutSeconds = prefs[Keys.TIMEOUT_SECONDS] ?: 15,
            crawlDepth = prefs[Keys.CRAWL_DEPTH] ?: 2,
            userAgent = prefs[Keys.USER_AGENT] ?: "random",
            blacklistUrlRegex = prefs[Keys.BLACKLIST_URL_REGEX] ?: "",
            whitelistUrlRegex = prefs[Keys.WHITELIST_URL_REGEX] ?: "",
            whitelistDomain = prefs[Keys.WHITELIST_DOMAIN] ?: "",
            cookie = prefs[Keys.COOKIE] ?: "",
            customHeaders = prefs[Keys.CUSTOM_HEADERS] ?: ""
        )
    }

    suspend fun save(config: ScanConfig) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ENABLE_JS_LINK_FINDER] = config.enableJsLinkFinder
            prefs[Keys.CRAWL_SITEMAP] = config.crawlSitemap
            prefs[Keys.CRAWL_ROBOTS] = config.crawlRobots
            prefs[Keys.DISABLE_REDIRECT] = config.disableRedirect
            prefs[Keys.LOCAL_LAB_MODE] = config.localLabMode
            prefs[Keys.THREADS] = config.threads
            prefs[Keys.TIMEOUT_SECONDS] = config.timeoutSeconds
            prefs[Keys.CRAWL_DEPTH] = config.crawlDepth
            prefs[Keys.USER_AGENT] = config.userAgent
            prefs[Keys.BLACKLIST_URL_REGEX] = config.blacklistUrlRegex
            prefs[Keys.WHITELIST_URL_REGEX] = config.whitelistUrlRegex
            prefs[Keys.WHITELIST_DOMAIN] = config.whitelistDomain
            prefs[Keys.COOKIE] = config.cookie
            prefs[Keys.CUSTOM_HEADERS] = config.customHeaders
        }
    }
}
