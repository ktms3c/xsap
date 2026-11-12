package com.example.xssscanner.ui.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.example.xssscanner.R
import com.example.xssscanner.domain.model.XssFinding
import com.example.xssscanner.scanner.analyzer.RiskScorer

class ResultAdapter(private val riskScorer: RiskScorer) : ListAdapter<XssFinding, ResultViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ResultViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_finding, parent, false)
        return ResultViewHolder(view, riskScorer)
    }

    override fun onBindViewHolder(holder: ResultViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<XssFinding>() {
            override fun areItemsTheSame(oldItem: XssFinding, newItem: XssFinding): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: XssFinding, newItem: XssFinding): Boolean =
                oldItem == newItem
        }
    }
}
