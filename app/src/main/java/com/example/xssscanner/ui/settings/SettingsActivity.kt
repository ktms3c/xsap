package com.example.xssscanner.ui.settings

import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.xssscanner.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SettingsActivity : AppCompatActivity() {

    private val viewModel: SettingsViewModel by viewModels()

    private lateinit var checkJsLinkFinder: CheckBox
    private lateinit var checkCrawlSitemap: CheckBox
    private lateinit var checkCrawlRobots: CheckBox
    private lateinit var checkDisableRedirect: CheckBox
    private lateinit var checkLocalLabMode: CheckBox
    private lateinit var editThreads: EditText
    private lateinit var editTimeoutSeconds: EditText
    private lateinit var editCrawlDepth: EditText
    private lateinit var editUserAgent: EditText
    private lateinit var editBlacklistRegex: EditText
    private lateinit var editWhitelistRegex: EditText
    private lateinit var editWhitelistDomain: EditText
    private lateinit var editCookie: EditText
    private lateinit var editCustomHeaders: EditText
    private lateinit var buttonSave: Button

    private var isApplyingState = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        bindViews()
        setupListeners()
        observeUiState()
    }

    private fun bindViews() {
        checkJsLinkFinder = findViewById(R.id.checkJsLinkFinder)
        checkCrawlSitemap = findViewById(R.id.checkCrawlSitemap)
        checkCrawlRobots = findViewById(R.id.checkCrawlRobots)
        checkDisableRedirect = findViewById(R.id.checkDisableRedirect)
        checkLocalLabMode = findViewById(R.id.checkLocalLabMode)
        editThreads = findViewById(R.id.editThreads)
        editTimeoutSeconds = findViewById(R.id.editTimeoutSeconds)
        editCrawlDepth = findViewById(R.id.editCrawlDepth)
        editUserAgent = findViewById(R.id.editUserAgent)
        editBlacklistRegex = findViewById(R.id.editBlacklistRegex)
        editWhitelistRegex = findViewById(R.id.editWhitelistRegex)
        editWhitelistDomain = findViewById(R.id.editWhitelistDomain)
        editCookie = findViewById(R.id.editCookie)
        editCustomHeaders = findViewById(R.id.editCustomHeaders)
        buttonSave = findViewById(R.id.buttonSave)
    }

    private fun setupListeners() {
        checkJsLinkFinder.setOnCheckedChangeListener { _, checked ->
            if (!isApplyingState) viewModel.update { it.copy(enableJsLinkFinder = checked) }
        }
        checkCrawlSitemap.setOnCheckedChangeListener { _, checked ->
            if (!isApplyingState) viewModel.update { it.copy(crawlSitemap = checked) }
        }
        checkCrawlRobots.setOnCheckedChangeListener { _, checked ->
            if (!isApplyingState) viewModel.update { it.copy(crawlRobots = checked) }
        }
        checkDisableRedirect.setOnCheckedChangeListener { _, checked ->
            if (!isApplyingState) viewModel.update { it.copy(disableRedirect = checked) }
        }
        checkLocalLabMode.setOnCheckedChangeListener { _, checked ->
            if (!isApplyingState) viewModel.update { it.copy(localLabMode = checked) }
        }

        buttonSave.setOnClickListener {
            // Pull current text field values into the ViewModel just before
            // saving, rather than on every keystroke (simpler + avoids
            // cursor-jump issues with per-keystroke StateFlow round-trips).
            viewModel.update {
                it.copy(
                    threads = editThreads.text.toString().toIntOrNull() ?: it.threads,
                    timeoutSeconds = editTimeoutSeconds.text.toString().toIntOrNull() ?: it.timeoutSeconds,
                    crawlDepth = editCrawlDepth.text.toString().toIntOrNull() ?: it.crawlDepth,
                    userAgent = editUserAgent.text.toString(),
                    blacklistUrlRegex = editBlacklistRegex.text.toString(),
                    whitelistUrlRegex = editWhitelistRegex.text.toString(),
                    whitelistDomain = editWhitelistDomain.text.toString(),
                    cookie = editCookie.text.toString(),
                    customHeaders = editCustomHeaders.text.toString()
                )
            }
            viewModel.save()
        }
    }

    private fun observeUiState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    if (state.isLoaded) applyStateToViews(state)

                    state.validationError?.let { error ->
                        Toast.makeText(this@SettingsActivity, error, Toast.LENGTH_LONG).show()
                    }
                    if (state.saved) {
                        Toast.makeText(this@SettingsActivity, R.string.settings_saved, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    /** Only called once per load to avoid fighting the user's typing on
     *  every StateFlow emission; text fields are read directly at Save
     *  time instead of being kept in perfect sync with the StateFlow. */
    private var appliedOnce = false
    private fun applyStateToViews(state: SettingsUiState) {
        if (appliedOnce) return
        appliedOnce = true
        isApplyingState = true
        checkJsLinkFinder.isChecked = state.enableJsLinkFinder
        checkCrawlSitemap.isChecked = state.crawlSitemap
        checkCrawlRobots.isChecked = state.crawlRobots
        checkDisableRedirect.isChecked = state.disableRedirect
        checkLocalLabMode.isChecked = state.localLabMode
        editThreads.setText(state.threads.toString())
        editTimeoutSeconds.setText(state.timeoutSeconds.toString())
        editCrawlDepth.setText(state.crawlDepth.toString())
        editUserAgent.setText(state.userAgent)
        editBlacklistRegex.setText(state.blacklistUrlRegex)
        editWhitelistRegex.setText(state.whitelistUrlRegex)
        editWhitelistDomain.setText(state.whitelistDomain)
        editCookie.setText(state.cookie)
        editCustomHeaders.setText(state.customHeaders)
        isApplyingState = false
    }
}
