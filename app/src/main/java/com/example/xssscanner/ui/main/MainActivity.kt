package com.example.xssscanner.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.xssscanner.R
import com.example.xssscanner.scanner.analyzer.RiskScorer
import com.example.xssscanner.ui.settings.SettingsActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * TODO: needs confirmation — §0.4's dependency list does not include
 * androidx.appcompat, but AppCompatActivity/AlertDialog are the conventional
 * Activity base + dialog APIs. themes.xml (Stage 1) deliberately uses a
 * plain android:Theme.Material.NoActionBar instead of a Theme.AppCompat/
 * Theme.MaterialComponents parent for the same reason; AppCompatActivity
 * still works without an AppCompat theme parent on API 24+, but this is
 * flagged since §0.4 is supposed to be the closed dependency list and
 * androidx.appcompat is not explicitly in it.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject lateinit var riskScorer: RiskScorer

    private val viewModel: MainViewModel by viewModels()

    private lateinit var editTargetUrl: EditText
    private lateinit var buttonStart: Button
    private lateinit var buttonStop: Button
    private lateinit var buttonSettings: Button
    private lateinit var buttonExportCrawl: Button
    private lateinit var buttonExportAnalysis: Button
    private lateinit var textStatus: TextView
    private lateinit var textProgress: TextView
    private lateinit var recyclerLogs: RecyclerView
    private lateinit var recyclerFindings: RecyclerView

    private val logAdapter = LogAdapter()
    private lateinit var resultAdapter: ResultAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        resultAdapter = ResultAdapter(riskScorer)

        bindViews()
        setupRecyclerViews()
        setupListeners()
        observeUiState()
    }

    private fun bindViews() {
        editTargetUrl = findViewById(R.id.editTargetUrl)
        buttonStart = findViewById(R.id.buttonStart)
        buttonStop = findViewById(R.id.buttonStop)
        buttonSettings = findViewById(R.id.buttonSettings)
        buttonExportCrawl = findViewById(R.id.buttonExportCrawl)
        buttonExportAnalysis = findViewById(R.id.buttonExportAnalysis)
        textStatus = findViewById(R.id.textStatus)
        textProgress = findViewById(R.id.textProgress)
        recyclerLogs = findViewById(R.id.recyclerLogs)
        recyclerFindings = findViewById(R.id.recyclerFindings)
    }

    private fun setupRecyclerViews() {
        recyclerLogs.layoutManager = LinearLayoutManager(this)
        recyclerLogs.adapter = logAdapter
        recyclerFindings.layoutManager = LinearLayoutManager(this)
        recyclerFindings.adapter = resultAdapter
    }

    private fun setupListeners() {
        editTargetUrl.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                viewModel.onEvent(MainUiEvent.TargetUrlChanged(s?.toString().orEmpty()))
            }
        })
        buttonStart.setOnClickListener { viewModel.onEvent(MainUiEvent.StartScanRequested) }
        buttonStop.setOnClickListener { viewModel.onEvent(MainUiEvent.StopScanRequested) }
        buttonSettings.setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }
        buttonExportCrawl.setOnClickListener { viewModel.onEvent(MainUiEvent.ExportCrawlRequested) }
        buttonExportAnalysis.setOnClickListener { viewModel.onEvent(MainUiEvent.ExportAnalysisRequested) }
    }

    private var safetyGateDialog: AlertDialog? = null

    private fun observeUiState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    textStatus.text = "Status: ${state.status}"
                    textProgress.text =
                        "Crawled: ${state.crawledUrlCount}  Params: ${state.discoveredParamCount}  Findings: ${state.findingCount}"
                    buttonStart.isEnabled = !state.isScanning
                    buttonStop.isEnabled = state.isScanning
                    buttonExportCrawl.isEnabled = !state.isScanning && state.crawledUrlCount > 0
                    buttonExportAnalysis.isEnabled = !state.isScanning && state.findingCount > 0

                    logAdapter.submitList(state.logs)
                    resultAdapter.submitList(state.findings)

                    if (state.showSafetyGate && safetyGateDialog == null) {
                        safetyGateDialog = showSafetyGateDialog()
                    } else if (!state.showSafetyGate) {
                        safetyGateDialog?.dismiss()
                        safetyGateDialog = null
                    }

                    state.errorMessage?.let { message ->
                        Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
                        viewModel.onEvent(MainUiEvent.ErrorMessageShown)
                    }
                }
            }
        }
    }

    /**
     * Implements §9's mandatory safety/legal gate: must be shown before
     * EVERY scan start, with explicit "I have authorization to test this
     * target" wording, and Cancel must be the default/safe action.
     */
    private fun showSafetyGateDialog(): AlertDialog {
        val dialogView = layoutInflater.inflate(R.layout.dialog_safety_gate, null)
        return AlertDialog.Builder(this)
            .setTitle(getString(R.string.safety_gate_title))
            .setView(dialogView)
            .setCancelable(false)
            .setPositiveButton(getString(R.string.safety_gate_confirm)) { _, _ ->
                viewModel.onEvent(MainUiEvent.SafetyGateConfirmed)
            }
            .setNegativeButton(getString(R.string.safety_gate_cancel)) { _, _ ->
                viewModel.onEvent(MainUiEvent.SafetyGateDismissed)
            }
            .show()
    }
}

