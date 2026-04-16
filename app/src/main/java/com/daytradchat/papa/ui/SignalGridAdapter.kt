//app/src/main/java/com/daytradchat/papa/ui/SignalGridAdapter.kt
//ver 2.16-15
package com.daytradchat.papa.ui

import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.daytradchat.papa.R
import com.daytradchat.papa.databinding.ItemSignalGridBinding
import com.daytradchat.papa.model.SignalCardUiModel
import java.text.DecimalFormat
import kotlin.math.abs

data class ProfitDisplay(
    val text: String,
    val isPositive: Boolean? = null
)

data class PriceVisual(
    val bgColorRes: Int,
    val codeNameColorRes: Int
)

class SignalGridAdapter(
    private val onItemClick: (SignalCardUiModel) -> Unit,
    private val profitProvider: (String, Double) -> ProfitDisplay,
    private val priceVisualProvider: (String, Double) -> PriceVisual
) : ListAdapter<SignalCardUiModel, SignalGridAdapter.SignalViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SignalViewHolder {
        val binding = ItemSignalGridBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SignalViewHolder(binding, onItemClick, profitProvider, priceVisualProvider)
    }

    override fun onBindViewHolder(holder: SignalViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class SignalViewHolder(
        private val binding: ItemSignalGridBinding,
        private val onItemClick: (SignalCardUiModel) -> Unit,
        private val profitProvider: (String, Double) -> ProfitDisplay,
        private val priceVisualProvider: (String, Double) -> PriceVisual
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SignalCardUiModel) {
            binding.rootSignal.setOnClickListener { onItemClick(item) }

            binding.textCode.text = item.code
            binding.textName.text = item.name
            binding.textPrice.text = formatPrice(item.price)
            binding.textDelta.text = "前日比 ${formatSigned(item.changeRate)}%"
            binding.textSub1.text = "score ${item.score}"
            binding.textSub2.text = item.reasonShort
            binding.textUpdatedAt.text = item.updatedAt.takeLast(8)

            val visual = priceVisualProvider(item.code, item.price)
            binding.rootSignal.setBackgroundColor(ContextCompat.getColor(binding.root.context, visual.bgColorRes))
            binding.textCode.setTextColor(ContextCompat.getColor(binding.root.context, visual.codeNameColorRes))
            binding.textName.setTextColor(ContextCompat.getColor(binding.root.context, visual.codeNameColorRes))

            val priceColor = ContextCompat.getColor(binding.root.context, visual.codeNameColorRes)
            val subColor = ContextCompat.getColor(binding.root.context, R.color.text_secondary)
            binding.textPrice.setTextColor(priceColor)
            binding.textDelta.setTextColor(subColor)
            binding.textSub1.setTextColor(subColor)
            binding.textSub2.setTextColor(subColor)
            binding.textUpdatedAt.setTextColor(subColor)

            val profit = profitProvider(item.code, item.price)
            binding.textProfit.text = profit.text
            when (profit.isPositive) {
                true -> binding.textProfit.setTextColor(ContextCompat.getColor(binding.root.context, R.color.profit_plus))
                false -> binding.textProfit.setTextColor(ContextCompat.getColor(binding.root.context, R.color.profit_minus))
                null -> binding.textProfit.setTextColor(subColor)
            }

            binding.textPrice.gravity = Gravity.END

            val isIndex = item.code.equals("NIKKEI225", ignoreCase = true)
            if (isIndex) {
                binding.textCode.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9f)
                binding.textName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 6f)
                binding.textPrice.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            } else {
                binding.textCode.setTextSize(TypedValue.COMPLEX_UNIT_SP, 8f)
                binding.textName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 5.8f)
                binding.textPrice.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            }
            binding.textDelta.setTextSize(TypedValue.COMPLEX_UNIT_SP, 5.8f)
            binding.textSub1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 5.8f)
            binding.textSub2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 5.8f)
            binding.textUpdatedAt.setTextSize(TypedValue.COMPLEX_UNIT_SP, 5.8f)
            binding.textProfit.setTextSize(TypedValue.COMPLEX_UNIT_SP, 8f)
        }

        private fun formatPrice(v: Double): String {
            val noDecimal = abs(v - v.toLong().toDouble()) < 0.000001
            return if (noDecimal) DecimalFormat("#,###").format(v.toLong()) else DecimalFormat("#,##0.0").format(v)
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
            override fun areItemsTheSame(oldItem: SignalCardUiModel, newItem: SignalCardUiModel): Boolean {
                return oldItem.slotId == newItem.slotId
            }

            override fun areContentsTheSame(oldItem: SignalCardUiModel, newItem: SignalCardUiModel): Boolean {
                return oldItem == newItem
            }
        }
    }
}
