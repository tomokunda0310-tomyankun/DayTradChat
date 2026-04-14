//app/src/main/java/com/daytradchat/papa/ui/SignalGridAdapter.kt
//ver 2.13-00
package com.daytradchat.papa.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.daytradchat.papa.R
import com.daytradchat.papa.databinding.ItemSignalGridBinding
import com.daytradchat.papa.model.SignalCardUiModel

class SignalGridAdapter : ListAdapter<SignalCardUiModel, SignalGridAdapter.SignalViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SignalViewHolder {
        val binding = ItemSignalGridBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SignalViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SignalViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class SignalViewHolder(private val binding: ItemSignalGridBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: SignalCardUiModel) {
            binding.textCodeName.text = item.codeName
            binding.textPrice.text = item.priceText
            binding.textDelta.text = item.deltaText
            binding.textSub1.text = item.sub1
            binding.textSub2.text = item.sub2
            binding.textUpdatedAt.text = item.updatedAt

            val context = binding.root.context
            val type = item.signalType.uppercase()
            val (bg, fg) = when (type) {
                "BUY" -> Pair(
                    ContextCompat.getColor(context, R.color.buy_bg),
                    ContextCompat.getColor(context, R.color.buy_text)
                )
                "SELL" -> Pair(
                    ContextCompat.getColor(context, R.color.sell_bg),
                    ContextCompat.getColor(context, R.color.sell_text)
                )
                "INDEX" -> if (item.changeRate >= 0.0) {
                    Pair(
                        ContextCompat.getColor(context, R.color.buy_bg),
                        ContextCompat.getColor(context, R.color.buy_text)
                    )
                } else {
                    Pair(
                        ContextCompat.getColor(context, R.color.sell_bg),
                        ContextCompat.getColor(context, R.color.sell_text)
                    )
                }
                else -> Pair(
                    ContextCompat.getColor(context, R.color.skip_bg),
                    ContextCompat.getColor(context, R.color.skip_text)
                )
            }

            binding.rootSignal.setBackgroundColor(bg)
            binding.textCodeName.setTextColor(fg)
            binding.textPrice.setTextColor(fg)
            binding.textDelta.setTextColor(fg)
            binding.textSub1.setTextColor(fg)
            binding.textSub2.setTextColor(fg)
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<SignalCardUiModel>() {
            override fun areItemsTheSame(oldItem: SignalCardUiModel, newItem: SignalCardUiModel): Boolean {
                return oldItem.slotId == newItem.slotId
            }

            override fun areContentsTheSame(oldItem: SignalCardUiModel, newItem: SignalCardUiModel): Boolean {
                return oldItem == newItem
            }
        }
    }
}
