//app/src/main/java/com/daytradchat/papa/ui/LogAdapter.kt
//ver 2.13-00
package com.daytradchat.papa.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.daytradchat.papa.databinding.ItemLogBinding
import com.daytradchat.papa.model.LogLineUiModel

class LogAdapter : ListAdapter<LogLineUiModel, LogAdapter.LogViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val binding = ItemLogBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LogViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class LogViewHolder(private val binding: ItemLogBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: LogLineUiModel) {
            binding.textLog.text = item.text
            binding.root.setOnClickListener {
                val context = binding.root.context
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("log_line", item.text))
                Toast.makeText(context, "コピーしました", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<LogLineUiModel>() {
            override fun areItemsTheSame(oldItem: LogLineUiModel, newItem: LogLineUiModel): Boolean = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: LogLineUiModel, newItem: LogLineUiModel): Boolean = oldItem == newItem
        }
    }
}
