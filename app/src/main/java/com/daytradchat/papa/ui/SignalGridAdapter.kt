//app/src/main/java/com/daytradchat/papa/ui/SignalGridAdapter.kt
//ver 2.13-16

package com.daytradchat.papa.ui

import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.daytradchat.papa.R
import com.daytradchat.papa.databinding.ItemSignalGridBinding
import com.daytradchat.papa.model.SignalCardUiModel
import kotlin.math.abs

class SignalGridAdapter(
    private val onItemClick: (SignalCardUiModel) -> Unit = {}
) : ListAdapter<SignalCardUiModel, SignalGridAdapter.SignalViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SignalViewHolder {
        val binding =
            ItemSignalGridBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SignalViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: SignalViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class SignalViewHolder(
        private val binding: ItemSignalGridBinding,
        private val onItemClick: (SignalCardUiModel) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SignalCardUiModel) {
            binding.textCodeName.text = buildCodeName(item)
            binding.textPrice.text = formatPrice(item.price)
            binding.textDelta.text = "前日比 ${formatSigned(item.changeRate)}%"
            binding.textSub1.text = "score ${item.score}"
            binding.textSub2.text = buildSub2(item)

            val context = binding.root.context
            val type = item.signalType.uppercase()

            val (bgColor, fgColor) = when (type) {
                "BUY" -> Pair(
                    ContextCompat.getColor(context, R.color.buy_bg),
                    ContextCompat.getColor(context, R.color.buy_text)
                )
                "SELL" -> Pair(
                    ContextCompat.getColor(context, R.color.sell_bg),
                    ContextCompat.getColor(context, R.color.sell_text)
                )
                "INDEX" -> {
                    if (item.changeRate >= 0.0) {
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
                }
                "SKIP" -> Pair(
                    ContextCompat.getColor(context, R.color.skip_bg),
                    ContextCompat.getColor(context, R.color.skip_text)
                )
                else -> Pair(
                    ContextCompat.getColor(context, R.color.bg_surface),
                    ContextCompat.getColor(context, R.color.text_primary)
                )
            }

            binding.rootSignal.setBackgroundColor(bgColor)
            binding.textCodeName.setTextColor(fgColor)
            binding.textPrice.setTextColor(fgColor)
            binding.textDelta.setTextColor(fgColor)
            binding.textSub1.setTextColor(fgColor)
            binding.textSub2.setTextColor(fgColor)

            val isIndex =
                item.code.equals("NIKKEI225", ignoreCase = true) ||
                    item.name.contains("日経")

            if (isIndex) {
                binding.textCodeName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                binding.textPrice.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                binding.textDelta.setTextSize(TypedValue.COMPLEX_UNIT_SP, 8f)
                binding.textSub1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 6f)
                binding.textSub2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 6f)
            } else {
                binding.textCodeName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                binding.textPrice.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
                binding.textDelta.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9f)
                binding.textSub1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 7f)
                binding.textSub2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 6f)
            }

            binding.root.setOnClickListener {
                onItemClick(item)
            }
        }

        private fun buildCodeName(item: SignalCardUiModel): String {
            val code = item.code.trim()
            val name = item.name.trim()
            return when {
                code.isEmpty() && name.isEmpty() -> "--"
                name.isEmpty() -> code
                code.isEmpty() -> name
                else -> "$code $name"
            }
        }

        private fun buildSub2(item: SignalCardUiModel): String {
            val reason = item.reasonShort.trim()
            val time = item.updatedAt.trim()
            return if (time.isBlank()) reason else "$reason  $time"
        }

        private fun formatPrice(v: Double): String {
            return if (abs(v - v.toLong().toDouble()) < 0.000001) {
                v.toLong().toString()
            } else {
                "%.1f".format(v)
            }
        }

        private fun formatSigned(v: Double): String {
            return when {
                v > 0.0 -> "+%.1f".format(v)
                v < 0.0 -> "%.1f".format(v)
                else -> "0.0"
            }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<SignalCardUiModel>() {
            override fun areItemsTheSame(
                oldItem: SignalCardUiModel,
                newItem: SignalCardUiModel
            ): Boolean = oldItem.slotId == newItem.slotId

            override fun areContentsTheSame(
                oldItem: SignalCardUiModel,
                newItem: SignalCardUiModel
            ): Boolean = oldItem == newItem
        }
    }
}