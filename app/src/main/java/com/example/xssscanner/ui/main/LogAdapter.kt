package com.example.xssscanner.ui.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.xssscanner.R
import com.example.xssscanner.domain.model.ScanLog

class LogAdapter : ListAdapter<ScanLog, LogViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_log, parent, false)
        return LogViewHolder(view)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<ScanLog>() {
            override fun areItemsTheSame(oldItem: ScanLog, newItem: ScanLog): Boolean =
                oldItem.timestamp == newItem.timestamp && oldItem.message == newItem.message

            override fun areContentsTheSame(oldItem: ScanLog, newItem: ScanLog): Boolean =
                oldItem == newItem
        }
    }
}
