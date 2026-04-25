//app/src/main/java/com/daytradchat/papa/ui/SignalFragment.kt
//ver 2.16-10
package com.daytradchat.papa.ui

import android.app.AlertDialog
import android.graphics.Typeface
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.daytradchat.papa.databinding.FragmentSignalBinding
import com.daytradchat.papa.model.SignalCardUiModel
import kotlinx.coroutines.launch

class SignalFragment : Fragment() {
    private var _binding: FragmentSignalBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TradeViewModel by activityViewModels()

    private val adapter = SignalGridAdapter(
        onItemClick = { item -> showHistoryDialog(item) },
        profitProvider = { code, price -> viewModel.getProfitDisplay(code, price) },
        priceVisualProvider = { code, price -> viewModel.getPriceVisual(code, price) }
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSignalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.recyclerSignals.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.recyclerSignals.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.signalItems.collect { list ->
                    adapter.submitList(list)
                }
            }
        }
    }

    private fun showHistoryDialog(item: SignalCardUiModel) {
        val textView = TextView(requireContext()).apply {
            text = viewModel.buildHistoryDialogText(item.code)
            setPadding(24, 24, 24, 24)
            textSize = 12f
            typeface = Typeface.MONOSPACE
            movementMethod = ScrollingMovementMethod()
        }

        AlertDialog.Builder(requireContext())
            .setTitle("${item.code} ${item.name}")
            .setView(textView)
            .setPositiveButton("閉じる", null)
            .show()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
