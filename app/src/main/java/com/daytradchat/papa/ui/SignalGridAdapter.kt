// app/src/main/java/com/daytradchat/papa/ui/SignalGridAdapter.kt
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

// ここにあった ProfitDisplay / PriceVisual の定義は削除しました (TradeViewModel側にあるため)

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
            binding.textPrice.text = formatPrice(item.price ?: 0.0)
            binding.textDelta.text = "前日比 ${formatSigned(item.changeRate ?: 0.0)}%"
            binding.textSub1.text = "score ${item.score}"
            binding.textSub2.text = item.reasonShort
            binding.textUpdatedAt.text = if ((item.updatedAt?.length ?: 0) >= 8) item.updatedAt.takeLast(8) else item.updatedAt

            val visual = priceVisualProvider(item.code ?: "", item.price ?: 0.0)
            val context = binding.root.context
            binding.rootSignal.setBackgroundColor(ContextCompat.getColor(context, visual.bgColorRes))
            
            val mainTextColor = ContextCompat.getColor(context, visual.codeNameColorRes)
            val subTextColor = ContextCompat.getColor(context, R.color.text_secondary)

            binding.textCode.setTextColor(mainTextColor)
            binding.textName.setTextColor(mainTextColor)
            binding.textPrice.setTextColor(mainTextColor)
            
            val profit = profitProvider(item.code ?: "", item.price ?: 0.0)
            binding.textProfit.text = profit.text
            when (profit.isPositive) {
                true -> binding.textProfit.setTextColor(ContextCompat.getColor(context, R.color.profit_plus))
                false -> binding.textProfit.setTextColor(ContextCompat.getColor(context, R.color.profit_minus))
                null -> binding.textProfit.setTextColor(subTextColor)
            }

            binding.textPrice.gravity = Gravity.END

            val isIndex = item.code?.contains("N225", ignoreCase = true) == true
            if (isIndex) {
                binding.textCode.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9f)
                binding.textName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 6f)
                binding.textPrice.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            } else {
                binding.textCode.setTextSize(TypedValue.COMPLEX_UNIT_SP, 8f)
                binding.textName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 5.8f)
                binding.textPrice.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            }
        }

        private fun formatPrice(v: Double): String {
            if (v == 0.0) return "--"
            return DecimalFormat("#,##0.0").format(v)
        }

        private fun formatSigned(v: Double): String = if (v > 0) "+%.1f".format(v) else "%.1f".format(v)
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<SignalCardUiModel>() {
            override fun areItemsTheSame(oldItem: SignalCardUiModel, newItem: SignalCardUiModel) = oldItem.slotId == newItem.slotId
            override fun areContentsTheSame(oldItem: SignalCardUiModel, newItem: SignalCardUiModel) = oldItem == newItem
        }
    }
}
