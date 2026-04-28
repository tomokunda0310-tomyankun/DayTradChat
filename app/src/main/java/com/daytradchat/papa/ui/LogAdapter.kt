// app/src/main/java/com/daytradchat/papa/ui/LogAdapter.kt
// ver 2.17-15
package com.daytradchat.papa.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.daytradchat.papa.databinding.ItemLogBinding // ★ItemLogLineBindingをこれに変更
import com.daytradchat.papa.model.LogLineUiModel

class LogAdapter : ListAdapter<LogLineUiModel, LogAdapter.LogViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        // ここも ItemLogBinding に修正
        val binding = ItemLogBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LogViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class LogViewHolder(private val binding: ItemLogBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: LogLineUiModel) {
            binding.textLog.text = item.text
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<LogLineUiModel>() {
            override fun areItemsTheSame(oldItem: LogLineUiModel, newItem: LogLineUiModel) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: LogLineUiModel, newItem: LogLineUiModel) = oldItem == newItem
        }
    }
}
