package com.example.xssscanner.ui.main

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.xssscanner.R
import com.example.xssscanner.domain.model.XssFinding
import com.example.xssscanner.scanner.analyzer.RiskScorer
import com.example.xssscanner.ui.common.LogColorMapper

class ResultViewHolder(itemView: View, private val riskScorer: RiskScorer) : RecyclerView.ViewHolder(itemView) {
    private val title: TextView = itemView.findViewById(R.id.textFindingTitle)
    private val detail: TextView = itemView.findViewById(R.id.textFindingDetail)

    fun bind(finding: XssFinding) {
        val severity = riskScorer.severityLabel(finding.riskScore)
        title.text = "${finding.status.name} ($severity, ${finding.riskScore}) - ${finding.parameter}"
        title.setTextColor(itemView.context.getColor(LogColorMapper.colorForFindingStatus(finding.status)))
        detail.text = "${finding.url}\n${finding.context} | ${finding.payload}"
    }
}
