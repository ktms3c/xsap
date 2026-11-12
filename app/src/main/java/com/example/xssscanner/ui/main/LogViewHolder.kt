package com.example.xssscanner.ui.main

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.xssscanner.R
import com.example.xssscanner.domain.model.ScanLog
import com.example.xssscanner.ui.common.LogColorMapper

class LogViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val textView: TextView = itemView.findViewById(R.id.textLogLine)

    fun bind(log: ScanLog) {
        val statusPart = log.statusCode?.let { " [$it]" }.orEmpty()
        textView.text = "[${log.category}]$statusPart ${log.message}"
        val colorRes = LogColorMapper.colorForStatusCode(log.statusCode)
            ?: LogColorMapper.colorForLevel(log.level)
        textView.setTextColor(itemView.context.getColor(colorRes))
    }
}
